/**
 * Copyright (c) 2014 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.knx.handler;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.thing.link.ItemChannelLinkRegistry;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.Type;
import org.openhab.binding.knx.GroupAddressListener;
import org.openhab.binding.knx.IndividualAddressListener;
import org.openhab.binding.knx.KNXBridgeListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tuwien.auto.calimero.GroupAddress;
import tuwien.auto.calimero.IndividualAddress;

/**
 * The {@link KNXBaseThingHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Karel Goderis - Initial contribution
 */
public abstract class KNXBaseThingHandler extends BaseThingHandler
        implements IndividualAddressListener, GroupAddressListener, KNXBridgeListener {

    protected Logger logger = LoggerFactory.getLogger(KNXBaseThingHandler.class);

    protected ItemChannelLinkRegistry itemChannelLinkRegistry;

    // group addresses the Thing is monitoring
    protected List<GroupAddress> groupAddresses = new ArrayList<GroupAddress>();
    // the physical address of the KNX actor represented by this Thing
    protected IndividualAddress address;

    public KNXBaseThingHandler(Thing thing, ItemChannelLinkRegistry itemChannelLinkRegistry) {
        super(thing);
        this.itemChannelLinkRegistry = itemChannelLinkRegistry;
    }

    @Override
    public void initialize() {
        // setting the ThingStatus is the responsibility of concrete classes
    }

    @Override
    public void bridgeHandlerInitialized(ThingHandler bridgeHandler, Bridge bridge) {

        if (bridgeHandler != null && bridge != null) {
            ((KNXBridgeBaseThingHandler) bridgeHandler).registerGroupAddressListener(this);
            ((KNXBridgeBaseThingHandler) bridgeHandler).registerKNXBridgeListener(this);
            if (bridge.getStatus() == ThingStatus.OFFLINE) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
            }
        } else {
            logger.warn("Can not initialize ThingHandler '{}' because it's bridge is not existing",
                    this.getThing().getUID());
            updateStatus(ThingStatus.OFFLINE);
        }
    }

    @Override
    public void bridgeHandlerDisposed(ThingHandler bridgeHandler, Bridge bridge) {

        if (bridgeHandler != null && bridge != null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
            ((KNXBridgeBaseThingHandler) bridgeHandler).unregisterGroupAddressListener(this);
            ((KNXBridgeBaseThingHandler) bridgeHandler).unregisterKNXBridgeListener(this);
        } else {
            logger.warn("Can not dispose ThingHandler '{}' because it's bridge is not existing",
                    this.getThing().getUID());
        }
    }

    @Override
    public void onBridgeConnected(KNXBridgeBaseThingHandler bridge) {
        initialize();
        updateStatus(ThingStatus.ONLINE);
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

        String dpt = getDPT(channelUID, command);
        String address = getAddress(channelUID, command);
        Type type = getType(channelUID, command);

        bridge.writeToKNX(address, dpt, type);
    }

    @Override
    public boolean listensTo(IndividualAddress source) {
        if (address != null) {
            return address.equals(source);
        } else {
            return false;
        }
    }

    @Override
    public boolean listensTo(GroupAddress destination) {
        return groupAddresses.contains(destination);
    }

    abstract public String getDPT(GroupAddress destination);

    abstract public String getDPT(ChannelUID channelUID, Type command);

    abstract public String getAddress(ChannelUID channelUID, Type command);

    abstract public Type getType(ChannelUID channelUID, Type command);
}
