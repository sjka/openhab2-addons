/**
 * Copyright (c) 2014 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.knx.handler;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.smarthome.core.items.Item;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.thing.link.ItemChannelLinkRegistry;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.core.types.Type;
import org.openhab.binding.knx.KNXBindingConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tuwien.auto.calimero.GroupAddress;
import tuwien.auto.calimero.datapoint.CommandDP;
import tuwien.auto.calimero.datapoint.Datapoint;
import tuwien.auto.calimero.exception.KNXFormatException;

/**
 * The {@link KNXBaseThingHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Karel Goderis - Initial contribution
 */
public abstract class KNXBaseThingHandler extends BaseThingHandler implements GAStatusListener {

    // List of all Configuration parameters
    public static final String READ = "read";
    public static final String INTERVAL = "interval";

    protected Logger logger = LoggerFactory.getLogger(KNXBaseThingHandler.class);

    // used to store events that we have sent ourselves; we need to remember them for not reacting to them
    protected static List<String> ignoreEventList = new ArrayList<String>();
    protected static List<GroupAddress> ignoreAddressList = new ArrayList<GroupAddress>();

    protected ItemChannelLinkRegistry itemChannelLinkRegistry;
    private ScheduledFuture<?> readJob;
    // list of addresses that have to read from the KNX bus pro-actively every INTERVAL seconds
    protected List<String> addresses = new ArrayList<String>();

    public KNXBaseThingHandler(Thing thing, ItemChannelLinkRegistry itemChannelLinkRegistry) {
        super(thing);
        this.itemChannelLinkRegistry = itemChannelLinkRegistry;
    }

    private Runnable readRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                logger.trace("Reading the 'Read'able Group Addresses of {}", getThing().getUID());
                readAddress();
            } catch (Exception e) {
                logger.debug("Exception during poll : {}", e);
            }
        }
    };

    @Override
    public void initialize() {
        initializeReadAddresses();
    }

    @Override
    public void bridgeHandlerInitialized(ThingHandler bridgeHandler, Bridge bridge) {

        if (bridgeHandler != null && bridge != null) {
            ((KNXBridgeBaseThingHandler) bridgeHandler).registerGAStatusListener(this);
            if (bridge.getStatus() == ThingStatus.ONLINE) {
                updateStatus(ThingStatus.ONLINE);

                if ((Boolean) getConfig().get(READ) && addresses.size() > 0) {

                    if (readJob == null || readJob.isCancelled()) {
                        BigDecimal readInterval = (BigDecimal) getConfig().get(INTERVAL);

                        if (readInterval != null && readInterval.intValue() > 0) {
                            readJob = scheduler.scheduleWithFixedDelay(readRunnable, 0, readInterval.intValue(),
                                    TimeUnit.SECONDS);
                        } else {
                            readJob = scheduler.schedule(readRunnable, 0, TimeUnit.SECONDS);
                        }
                    }
                }

            } else {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
            }
        } else {
            logger.warn("Can not initialize ThingHandler '{}' because it's bridge is not existing",
                    this.getThing().getUID());
            updateStatus(ThingStatus.OFFLINE);
        }
    }

    @Override
    public void dispose() {

        if (readJob != null && !readJob.isCancelled()) {
            readJob.cancel(true);
            readJob = null;
        }
    }

    @Override
    public void bridgeHandlerDisposed(ThingHandler bridgeHandler, Bridge bridge) {

        if (bridgeHandler != null && bridge != null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
            ((KNXBridgeBaseThingHandler) bridgeHandler).unregisterGAStatusListener(this);
        } else {
            logger.warn("Can not dispose ThingHandler '{}' because it's bridge is not existing",
                    this.getThing().getUID());
        }
    }

    @Override
    public void onBridgeConnected(KNXBridgeBaseThingHandler bridge) {
        initialize();
        updateStatus(ThingStatus.ONLINE);
        readAddress();
    }

    @Override
    public void onBridgeDisconnected(KNXBridgeBaseThingHandler bridge) {
        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {

        KNXBridgeBaseThingHandler bridge = (KNXBridgeBaseThingHandler) getBridge().getHandler();

        if (bridge == null) {
            logger.warn("KNX bridge handler not found. Cannot handle command without bridge.");
            return;
        }

        String ignoreEventListKey = channelUID.toString() + command.toString();
        if (ignoreEventList.contains(ignoreEventListKey)) {
            ignoreEventList.remove(ignoreEventListKey);
            logger.trace(
                    "We received this event (channel='{}', state='{}') from KNX, so we don't send it back again -> ignore!",
                    channelUID, command.toString());
            return;
        }

        String dpt = getDPT(channelUID, command);
        String address = getAddress(channelUID, command);
        Type type = getType(channelUID, command);

        bridge.writeToKNX(address, dpt, type);
    }

    @Override
    public void handleUpdate(ChannelUID channelUID, State state) {

        KNXBridgeBaseThingHandler bridge = (KNXBridgeBaseThingHandler) getBridge().getHandler();

        if (bridge == null) {
            logger.warn("KNX bridge handler not found. Cannot handle an update without bridge.");
            return;
        }

        String ignoreEventListKey = channelUID.toString() + state.toString();
        if (ignoreEventList.contains(ignoreEventListKey)) {
            ignoreEventList.remove(ignoreEventListKey);
            logger.debug(
                    "We received this event (channel='{}', state='{}') from KNX, so we don't send it back again -> ignore!",
                    channelUID, state.toString());
            return;
        }

        String dpt = getDPT(channelUID, state);
        String address = getAddress(channelUID, state);
        Type type = getType(channelUID, state);

        bridge.writeToKNX(address, dpt, type);
    }

    protected void updateStateAndIgnore(ChannelUID channelUID, State state) {

        Set<Item> itemSet = this.getLinkedItems(channelUID.getId());

        for (Item anItem : itemSet) {
            logger.trace("The channel '{}' is bound to item '{}' ", channelUID, anItem);
            for (ChannelUID cUID : itemChannelLinkRegistry.getBoundChannels(anItem.getName())) {
                logger.trace("Item '{}' has a channel with id '{}'", anItem, cUID);
                if (cUID.getBindingId().equals(KNXBindingConstants.BINDING_ID)
                        && !cUID.toString().equals(channelUID.toString())) {
                    logger.trace("Added event (channel='{}', type='{}') to the ignore event list", cUID,
                            state.toString());
                    ignoreEventList.add(cUID.toString() + state.toString());
                }
            }
        }

        updateState(channelUID, state);
        logger.trace("Processed  event (channel='{}', value='{}')", channelUID, state.toString());

    }

    protected void sendCommandAndIgnore(ChannelUID channelUID, Command command) {

        Set<Item> itemSet = this.getLinkedItems(channelUID.getId());

        for (Item anItem : itemSet) {
            logger.trace("The channel '{}' is bound to item '{}' ", channelUID, anItem);
            for (ChannelUID cUID : itemChannelLinkRegistry.getBoundChannels(anItem.getName())) {
                logger.trace("Item '{}' has a channel with id '{}'", anItem, cUID);
                if (cUID.getBindingId().equals(KNXBindingConstants.BINDING_ID)
                        && !cUID.toString().equals(channelUID.toString())) {
                    logger.trace("Added event (channel='{}', type='{}') to the ignore event list", cUID,
                            command.toString());
                    ignoreEventList.add(cUID.toString() + command.toString());
                }
            }
        }

        updateState(channelUID, (State) command);
        logger.trace("Processed  event (channel='{}', value='{}')", channelUID, command.toString());

    }

    protected void readAddress() {
        if (getThing().getStatus() == ThingStatus.ONLINE && addresses != null) {
            KNXBridgeBaseThingHandler bridge = (KNXBridgeBaseThingHandler) getBridge().getHandler();
            for (String address : addresses) {
                GroupAddress groupAddress = null;
                try {
                    groupAddress = new GroupAddress(address);
                } catch (KNXFormatException e) {
                    logger.error("An exception occurred while creating a Group Address : '{}'", e.getMessage());
                }
                Datapoint datapoint = new CommandDP(groupAddress, getThing().getUID().toString(), 0,
                        getDPT(groupAddress));
                bridge.readDatapoint(datapoint, bridge.getReadRetriesLimit());
            }
        }
    }

    abstract void processDataReceived(GroupAddress destination, Type state);

    @Override
    public void onDataReceived(KNXBridgeBaseThingHandler bridge, GroupAddress destination, byte[] asdu) {

        if (listensTo(destination)) {

            if (ignoreAddressList.contains(destination)) {
                ignoreAddressList.remove(destination);
                logger.debug("We were ordered to ignore an event comming from '{}'", destination.toString());
                return;
            }

            String dpt = getDPT(destination);
            if (dpt != null) {
                Type type = bridge.getType(destination, dpt, asdu);
                if (type != null) {
                    processDataReceived(destination, type);
                } else {
                    final char[] hexCode = "0123456789ABCDEF".toCharArray();
                    StringBuilder sb = new StringBuilder(2 + asdu.length * 2);
                    sb.append("0x");
                    for (byte b : asdu) {
                        sb.append(hexCode[(b >> 4) & 0xF]);
                        sb.append(hexCode[(b & 0xF)]);
                    }

                    logger.warn(
                            "Ignoring KNX bus data: couldn't transform to an openHAB type (not supported). Destination='{}', dpt='{}', data='{}'",
                            new Object[] { destination.toString(), dpt, sb.toString() });
                    return;
                }
            } else {
                logger.warn("Ignoring KNX bus data: no DPT is defined for group address '{}'", destination);
            }
        }
    }

    abstract String getDPT(GroupAddress destination);

    abstract String getDPT(ChannelUID channelUID, Type command);

    abstract String getAddress(ChannelUID channelUID, Type command);

    abstract Type getType(ChannelUID channelUID, Type command);

    abstract void initializeReadAddresses();

    protected Set<Item> getLinkedItems(String channelId) {
        Channel channel = getThing().getChannel(channelId);
        if (channel != null) {
            return channel.getLinkedItems();
        } else {
            throw new IllegalArgumentException("Channel with ID '" + channelId + "' does not exists.");
        }
    }
}
