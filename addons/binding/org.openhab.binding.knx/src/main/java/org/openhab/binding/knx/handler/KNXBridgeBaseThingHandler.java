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
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.thing.link.ItemChannelLinkRegistry;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.core.types.Type;
import org.openhab.binding.knx.KNXBindingConstants;
import org.openhab.binding.knx.discovery.KNXBusListener;
import org.openhab.binding.knx.internal.dpt.KNXCoreTypeMapper;
import org.openhab.binding.knx.internal.dpt.KNXTypeMapper;
import org.openhab.binding.knx.internal.logging.LogAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tuwien.auto.calimero.CloseEvent;
import tuwien.auto.calimero.DetachEvent;
import tuwien.auto.calimero.FrameEvent;
import tuwien.auto.calimero.GroupAddress;
import tuwien.auto.calimero.cemi.CEMILData;
import tuwien.auto.calimero.datapoint.CommandDP;
import tuwien.auto.calimero.datapoint.Datapoint;
import tuwien.auto.calimero.exception.KNXException;
import tuwien.auto.calimero.exception.KNXFormatException;
import tuwien.auto.calimero.exception.KNXIllegalArgumentException;
import tuwien.auto.calimero.link.KNXNetworkLink;
import tuwien.auto.calimero.link.NetworkLinkListener;
import tuwien.auto.calimero.log.LogManager;
import tuwien.auto.calimero.process.ProcessCommunicator;
import tuwien.auto.calimero.process.ProcessCommunicatorImpl;
import tuwien.auto.calimero.process.ProcessEvent;
import tuwien.auto.calimero.process.ProcessListener;

/**
 * The {@link KNXBridgeBaseThingHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Kai Kreuzer / Karel Goderis - Initial contribution
 */
public abstract class KNXBridgeBaseThingHandler extends BaseThingHandler implements ProcessListener, GAStatusListener {

    // List of all Configuration parameters
    public static final String AUTO_RECONNECT_PERIOD = "autoReconnectPeriod";
    public static final String RESPONSE_TIME_OUT = "responseTimeOut";
    public static final String READ_RETRIES_LIMIT = "readRetriesLimit";
    public static final String READING_PAUSE = "readingPause";
    public final static String DPT = "dpt";
    public final static String INCREASE_DECREASE_DPT = "increasedecreaseDPT";
    public final static String PERCENT_DPT = "percentDPT";
    public final static String ADDRESS = "address";
    public final static String STATE_ADDRESS = "stateGA";
    public final static String INCREASE_DECREASE_ADDRESS = "increasedecreaseGA";
    public final static String PERCENT_ADDRESS = "percentGA";
    public final static String READ = "read";
    public final static String INTERVAL = "interval";

    public final static int ERROR_INTERVAL_MINUTES = 5;

    protected Logger logger = LoggerFactory.getLogger(KNXBridgeBaseThingHandler.class);

    // used to store events that we have sent ourselves; we need to remember them for not reacting to them
    private static List<String> ignoreEventList = new ArrayList<String>();

    private List<GAStatusListener> gaStatusListeners = new CopyOnWriteArrayList<>();
    private List<KNXBusListener> knxBusListeners = new CopyOnWriteArrayList<>();
    static protected Collection<KNXTypeMapper> typeMappers = new HashSet<KNXTypeMapper>();
    private LinkedBlockingQueue<RetryDatapoint> readDatapoints = new LinkedBlockingQueue<RetryDatapoint>();

    protected ItemChannelLinkRegistry itemChannelLinkRegistry;
    private ProcessCommunicator pc = null;
    private final LogAdapter logAdapter = new LogAdapter();
    protected KNXNetworkLink link;
    private ScheduledFuture<?> reconnectJob;
    private ScheduledFuture<?> busJob;
    private List<ScheduledFuture<?>> readJobs;
    // signals that the connection is shut down on purpose
    public boolean shutdown = false;
    private long intervalTimestamp;
    private long errorsSinceStart;
    private long errorsSinceInterval;

    public KNXBridgeBaseThingHandler(Thing thing, ItemChannelLinkRegistry itemChannelLinkRegistry) {
        super(thing);
        this.itemChannelLinkRegistry = itemChannelLinkRegistry;
    }

    @Override
    public void initialize() {

        // register ourselves as a Group Address Status Listener
        registerGAStatusListener(this);

        // reset the counters
        errorsSinceStart = 0;
        errorsSinceInterval = 0;

        LogManager.getManager().addWriter(null, logAdapter);
        connect();
    }

    @Override
    public void dispose() {

        // unregister ourselves as a Group Address Status Listener
        unregisterGAStatusListener(this);

        disconnect();
        LogManager.getManager().removeWriter(null, logAdapter);
    }

    @Override
    public void thingUpdated(Thing thing) {
        dispose();
        initialize();
    }

    /**
     * Returns the KNXNetworkLink for talking to the KNX bus.
     * The link can be null, if it has not (yet) been established successfully.
     *
     * @return the KNX network link
     */
    public synchronized ProcessCommunicator getCommunicator() {
        if (link != null && !link.isOpen()) {
            connect();
        }
        return pc;
    }

    public int getReadRetriesLimit() {
        return ((BigDecimal) getConfig().get(READ_RETRIES_LIMIT)).intValue();
    }

    public abstract KNXNetworkLink establishConnection() throws KNXException;

    public synchronized void connect() {
        try {
            shutdown = false;

            link = establishConnection();

            NetworkLinkListener linkListener = new NetworkLinkListener() {
                @Override
                public void linkClosed(CloseEvent e) {
                    // if the link is lost, we want to reconnect immediately

                    onConnectionLost();

                    if (!(CloseEvent.USER_REQUEST == e.getInitiator()) && !shutdown) {
                        logger.warn("KNX link has been lost (reason: {} on object {}) - reconnecting...", e.getReason(),
                                e.getSource().toString());
                        connect();
                    }
                    if (!link.isOpen() && !shutdown) {
                        logger.error("KNX link has been lost!");
                        if (((BigDecimal) getConfig().get(AUTO_RECONNECT_PERIOD)).intValue() > 0) {
                            logger.info("KNX link will be retried in "
                                    + ((BigDecimal) getConfig().get(AUTO_RECONNECT_PERIOD)).intValue() + " seconds");

                            Runnable reconnectRunnable = new Runnable() {
                                @Override
                                public void run() {
                                    if (shutdown) {
                                        reconnectJob.cancel(true);
                                    } else {
                                        logger.info("Trying to reconnect to KNX...");
                                        connect();
                                        if (link.isOpen()) {
                                            reconnectJob.cancel(true);
                                        }
                                    }
                                }
                            };

                            reconnectJob = scheduler.scheduleWithFixedDelay(reconnectRunnable,
                                    ((BigDecimal) getConfig().get(AUTO_RECONNECT_PERIOD)).intValue(),
                                    ((BigDecimal) getConfig().get(AUTO_RECONNECT_PERIOD)).intValue(), TimeUnit.SECONDS);

                        }
                    }
                }

                @Override
                public void indication(FrameEvent e) {

                    if (intervalTimestamp == 0) {
                        intervalTimestamp = System.currentTimeMillis();
                        updateState(new ChannelUID(getThing().getUID(), KNXBindingConstants.ERRORS_STARTUP),
                                new DecimalType(errorsSinceStart));
                        updateState(new ChannelUID(getThing().getUID(), KNXBindingConstants.ERRORS_INTERVAL),
                                new DecimalType(errorsSinceInterval));
                    } else if ((System.currentTimeMillis() - intervalTimestamp) > 60 * 1000 * ERROR_INTERVAL_MINUTES) {
                        intervalTimestamp = System.currentTimeMillis();
                        errorsSinceInterval = 0;
                        updateState(new ChannelUID(getThing().getUID(), KNXBindingConstants.ERRORS_INTERVAL),
                                new DecimalType(errorsSinceInterval));
                    }

                    int messageCode = e.getFrame().getMessageCode();

                    switch (messageCode) {
                        case CEMILData.MC_LDATA_IND: {
                            CEMILData cemi = (CEMILData) e.getFrame();
                            if (cemi.isRepetition()) {
                                errorsSinceStart++;
                                errorsSinceInterval++;

                                updateState(new ChannelUID(getThing().getUID(), KNXBindingConstants.ERRORS_STARTUP),
                                        new DecimalType(errorsSinceStart));
                                updateState(new ChannelUID(getThing().getUID(), KNXBindingConstants.ERRORS_INTERVAL),
                                        new DecimalType(errorsSinceInterval));

                            }
                            break;
                        }
                    }
                }

                @Override
                public void confirmation(FrameEvent e) {

                    if (intervalTimestamp == 0) {
                        intervalTimestamp = System.currentTimeMillis();
                        updateState(new ChannelUID(getThing().getUID(), KNXBindingConstants.ERRORS_STARTUP),
                                new DecimalType(errorsSinceStart));
                        updateState(new ChannelUID(getThing().getUID(), KNXBindingConstants.ERRORS_INTERVAL),
                                new DecimalType(errorsSinceInterval));
                    } else if ((System.currentTimeMillis() - intervalTimestamp) > 60 * 1000 * ERROR_INTERVAL_MINUTES) {
                        intervalTimestamp = System.currentTimeMillis();
                        errorsSinceInterval = 0;
                        updateState(new ChannelUID(getThing().getUID(), KNXBindingConstants.ERRORS_INTERVAL),
                                new DecimalType(errorsSinceInterval));
                    }

                    int messageCode = e.getFrame().getMessageCode();
                    switch (messageCode) {
                        case CEMILData.MC_LDATA_CON: {
                            CEMILData cemi = (CEMILData) e.getFrame();
                            if (!cemi.isPositiveConfirmation()) {
                                errorsSinceStart++;
                                errorsSinceInterval++;

                                updateState(new ChannelUID(getThing().getUID(), KNXBindingConstants.ERRORS_STARTUP),
                                        new DecimalType(errorsSinceStart));
                                updateState(new ChannelUID(getThing().getUID(), KNXBindingConstants.ERRORS_INTERVAL),
                                        new DecimalType(errorsSinceInterval));
                            }
                            break;
                        }

                    }
                }
            };

            link.addLinkListener(linkListener);

            if (pc != null) {
                pc.removeProcessListener(this);
                pc.detach();
            }

            pc = new ProcessCommunicatorImpl(link);
            pc.setResponseTimeout(((BigDecimal) getConfig().get(RESPONSE_TIME_OUT)).intValue() / 1000);
            pc.addProcessListener(this);

            onConnectionResumed();

        } catch (

        KNXException e)

        {
            logger.error("Error connecting to KNX bus: {}", e.getMessage());
        }

    }

    public synchronized void disconnect() {
        shutdown = true;
        if (pc != null) {
            KNXNetworkLink link = pc.detach();
            pc.removeProcessListener(this);

            if (reconnectJob != null) {
                reconnectJob.cancel(true);
            }

            if (busJob != null) {
                busJob.cancel(true);
            }

            if (link != null) {
                logger.info("Closing KNX connection");
                link.close();
            }

            for (ScheduledFuture<?> readJob : readJobs) {
                if (!readJob.isDone()) {
                    readJob.cancel(true);
                }
            }
        }
    }

    public void onConnectionLost() {
        logger.debug("Updating thing status to OFFLINE.");
        updateStatus(ThingStatus.OFFLINE);

        for (GAStatusListener listener : gaStatusListeners) {
            listener.onBridgeDisconnected(this);
        }
    }

    public void onConnectionResumed() {

        if (readJobs != null) {
            for (ScheduledFuture<?> readJob : readJobs) {
                readJob.cancel(true);
            }
        }

        if (busJob != null) {
            busJob.cancel(true);
        }

        readDatapoints = new LinkedBlockingQueue<RetryDatapoint>();

        busJob = scheduler.scheduleWithFixedDelay(new BusRunnable(), 0,
                ((BigDecimal) getConfig().get(READING_PAUSE)).intValue(), TimeUnit.MILLISECONDS);

        readJobs = new ArrayList<ScheduledFuture<?>>();

        for (Channel channel : getThing().getChannels()) {

            Configuration channelConfiguration = channel.getConfiguration();
            String dpt = (String) channelConfiguration.get(DPT);
            String address = (String) channelConfiguration.get(ADDRESS);
            if (dpt != null && address != null) {
                Boolean read = false;
                if (channelConfiguration.get(READ) != null) {
                    read = ((Boolean) channelConfiguration.get(READ));
                }
                int readInterval = 0;
                if (channelConfiguration.get(INTERVAL) != null) {
                    readInterval = ((BigDecimal) channelConfiguration.get(INTERVAL)).intValue();
                }

                if (KNXCoreTypeMapper.toTypeClass(dpt) == null) {
                    logger.warn("DPT " + dpt + " is not supported by the KNX binding.");
                    return;
                }

                // create group address and datapoint
                try {
                    GroupAddress groupAddress = new GroupAddress(address);
                    Datapoint datapoint = new CommandDP(groupAddress, getThing().getUID().toString(), 0, dpt);

                    if (read && readInterval == 0) {
                        logger.debug("Scheduling reading out group address '{}'", address);
                        readJobs.add(scheduler.schedule(new ReadRunnable(datapoint, getReadRetriesLimit()), 0,
                                TimeUnit.SECONDS));
                    }

                    if (read && readInterval > 0) {
                        logger.debug("Scheduling reading out group address '{}' every '{}' seconds", address,
                                readInterval);
                        readJobs.add(scheduler.scheduleWithFixedDelay(
                                new ReadRunnable(datapoint, getReadRetriesLimit()), 0, readInterval, TimeUnit.SECONDS));
                    }

                } catch (KNXFormatException e) {
                    logger.warn("The datapoint for group address '{}' with DPT '{}' could not be initialised", address,
                            dpt);
                }
            }
        }

        for (GAStatusListener listener : gaStatusListeners) {
            listener.onBridgeConnected(this);
        }

        updateStatus(ThingStatus.ONLINE);

        errorsSinceStart = 0;
        errorsSinceInterval = 0;
    }

    @Override
    public void onBridgeConnected(KNXBridgeBaseThingHandler bridge) {
        // Do nothing, since we are the bridge
    }

    @Override
    public void onBridgeDisconnected(KNXBridgeBaseThingHandler bridge) {
        // Do nothing, since we are the bridge
    }

    @Override
    public boolean listensTo(GroupAddress destination) {
        // Bridges are allowed to listen to any GA that flies by on the bus, even if they do not have channel that
        // actively
        // uses that GA
        return true;
    }

    public boolean registerGAStatusListener(GAStatusListener gaStatusListener) {
        if (gaStatusListener == null) {
            throw new NullPointerException("It's not allowed to pass a null GAStatusListener.");
        }
        boolean result = gaStatusListeners.add(gaStatusListener);

        return result;
    }

    public boolean unregisterGAStatusListener(GAStatusListener gaStatusListener) {
        if (gaStatusListener == null) {
            throw new NullPointerException("It's not allowed to pass a null GAStatusListener.");
        }
        boolean result = gaStatusListeners.remove(gaStatusListener);

        return result;
    }

    public void addKNXTypeMapper(KNXTypeMapper typeMapper) {
        typeMappers.add(typeMapper);
    }

    public void removeKNXTypeMapper(KNXTypeMapper typeMapper) {
        typeMappers.remove(typeMapper);
    }

    public void registerKNXBusListener(KNXBusListener knxBusListener) {
        if (knxBusListener != null) {
            knxBusListeners.add(knxBusListener);
        }
    }

    public void unregisterKNXBusListener(KNXBusListener knxBusListener) {
        if (knxBusListener != null) {
            knxBusListeners.remove(knxBusListener);
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {

        if (channelUID != null) {
            Channel channel = this.getThing().getChannel(channelUID.getId());
            if (channel != null) {
                Configuration channelConfiguration = channel.getConfiguration();
                if (ignoreEventList.contains(channelUID.toString() + command.toString())) {
                    logger.trace("Ooops... this event should be ignored");
                    ignoreEventList.remove(channelUID.toString() + command.toString());
                } else {
                    this.writeToKNX((String) channelConfiguration.get(ADDRESS), (String) channelConfiguration.get(DPT),
                            command);
                }
            } else {
                logger.error("No channel is associated with channelUID {}", channelUID);
            }
        }

    }

    @Override
    public void handleUpdate(ChannelUID channelUID, State newState) {

        if (channelUID != null) {
            Channel channel = this.getThing().getChannel(channelUID.getId());
            if (channel != null) {
                Configuration channelConfiguration = channel.getConfiguration();

                if (ignoreEventList.contains(channelUID.toString() + newState.toString())) {
                    logger.trace("Ooops... this event should be ignored");
                    ignoreEventList.remove(channelUID.toString() + newState.toString());
                } else {
                    this.writeToKNX((String) channelConfiguration.get(ADDRESS), (String) channelConfiguration.get(DPT),
                            newState);
                }
            }
        }

    }

    public class BusRunnable implements Runnable {

        @Override
        public void run() {

            if (getThing().getStatus() == ThingStatus.ONLINE && pc != null) {

                RetryDatapoint datapoint = readDatapoints.poll();

                if (datapoint != null) {
                    datapoint.incrementRetries();

                    boolean success = false;
                    try {
                        logger.trace("Sending read request on the KNX bus for datapoint {}",
                                datapoint.getDatapoint().getMainAddress());
                        pc.read(datapoint.getDatapoint());
                        success = true;
                    } catch (KNXException e) {
                        logger.warn("Cannot read value for datapoint '{}' from KNX bus: {}",
                                datapoint.getDatapoint().getMainAddress(), e.getMessage());
                    } catch (KNXIllegalArgumentException e) {
                        logger.warn("Error sending KNX read request for datapoint '{}': {}",
                                datapoint.getDatapoint().getMainAddress(), e.getMessage());
                    } catch (InterruptedException e) {
                        logger.warn("Error sending KNX read request for datapoint '{}': {}",
                                datapoint.getDatapoint().getMainAddress(), e.getMessage());
                    }
                    if (!success) {
                        if (datapoint.getRetries() < datapoint.getLimit()) {
                            logger.debug(
                                    "Adding the read request (after attempt '{}') for datapoint '{}' at position '{}' in the queue",
                                    datapoint.getRetries(), datapoint.getDatapoint().getMainAddress(),
                                    readDatapoints.size() + 1);
                            readDatapoints.add(datapoint);
                        } else {
                            logger.debug("Giving up reading datapoint {} - nubmer of maximum retries ({}) reached.",
                                    datapoint.getDatapoint().getMainAddress(), datapoint.getLimit());
                        }
                    }
                }
            }
        }
    };

    public class ReadRunnable implements Runnable {

        private Datapoint datapoint;
        private int retries;

        public ReadRunnable(Datapoint datapoint, int retries) {
            this.datapoint = datapoint;
            this.retries = retries;
        }

        @Override
        public void run() {
            try {
                readDatapoint(datapoint, retries);
            } catch (Exception e) {
                logger.debug("Exception during poll : {}", e);
            }
        }
    };

    public class RetryDatapoint {

        private Datapoint datapoint;
        private int retries;
        private int limit;

        public Datapoint getDatapoint() {
            return datapoint;
        }

        public int getRetries() {
            return retries;
        }

        public void incrementRetries() {
            this.retries++;
        }

        public int getLimit() {
            return limit;
        }

        public RetryDatapoint(Datapoint datapoint, int limit) {
            this.datapoint = datapoint;
            this.retries = 0;
            this.limit = limit;
        }
    }

    public void readDatapoint(Datapoint datapoint, int retriesLimit) {
        synchronized (this) {
            if (datapoint != null) {
                RetryDatapoint retryDatapoint = new RetryDatapoint(datapoint, retriesLimit);
                logger.debug("Adding the read request for datapoint '{}' at position '{}' in the queue",
                        datapoint.getMainAddress(), readDatapoints.size() + 1);
                readDatapoints.add(retryDatapoint);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void groupWrite(ProcessEvent e) {
        readFromKNX(e);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void detached(DetachEvent e) {
        logger.error("Received detach Event.");
    }

    /**
     * Handles the given {@link ProcessEvent}. If the KNX ASDU is valid
     * it is passed on to the {@link GAStatusListener}s that are interested
     * in the destination Group Address, and subsequently to the
     * {@link KNXBusListener}s that are interested in all KNX bus activity
     *
     * @param e the {@link ProcessEvent} to handle.
     */
    private void readFromKNX(ProcessEvent e) {
        try {
            GroupAddress destination = e.getDestination();
            byte[] asdu = e.getASDU();
            if (asdu.length == 0) {
                return;
            }

            // logger.trace("Received a KNX telegram from '{}' for destination '{}'",e.getSourceAddr(),destination);

            for (GAStatusListener listener : gaStatusListeners) {
                if (listener.listensTo(destination)) {
                    listener.onDataReceived(this, destination, asdu);
                }
            }

            for (KNXBusListener listener : knxBusListeners) {
                listener.onActivity(e.getSourceAddr(), destination, asdu);
            }

        } catch (RuntimeException re) {
            logger.error("Error while receiving event from KNX bus: " + re.toString());
        }
    }

    public void writeToKNX(String address, String dpt, Type value) {

        if (dpt != null && address != null) {
            GroupAddress groupAddress = null;
            try {
                groupAddress = new GroupAddress(address);
            } catch (Exception e) {
                logger.error("An exception occurred while creating a Group Address : '{}'", e.getMessage());
            }
            Datapoint datapoint = new CommandDP(groupAddress, getThing().getUID().toString(), 0, dpt);

            writeToKNX(datapoint, value);
        }
    }

    public void writeToKNX(Datapoint datapoint, Type value) {

        ProcessCommunicator pc = getCommunicator();

        if (pc != null) {
            try {
                String dpt = toDPTValue(value, datapoint.getDPT());
                if (dpt != null) {
                    pc.write(datapoint, dpt);
                    logger.debug("Wrote value '{}' to datapoint '{}'", value, datapoint);
                } else {
                    logger.debug("Value '{}' can not be mapped to datapoint '{}'", value, datapoint);
                }
            } catch (KNXException e) {
                logger.debug("Value '{}' could not be sent to the KNX bus using datapoint '{}' - retrying one time: {}",
                        new Object[] { value, datapoint, e.getMessage() });
                try {
                    // do a second try, maybe the reconnection was successful
                    pc = getCommunicator();
                    pc.write(datapoint, toDPTValue(value, datapoint.getDPT()));
                    logger.debug("Wrote value '{}' to datapoint '{}' on second try", value, datapoint);
                } catch (KNXException e1) {
                    logger.error(
                            "Value '{}' could not be sent to the KNX bus using datapoint '{}' - giving up after second try: {}",
                            new Object[] { value, datapoint, e1.getMessage() });
                }
            }
        } else {
            logger.error("Could not get hold of KNX Process Communicator");
        }
    }

    @Override
    public void onDataReceived(KNXBridgeBaseThingHandler bridge, GroupAddress destination, byte[] asdu) {

        for (Channel channel : getThing().getChannels()) {

            // first process the data for the "main" address associated with each channel
            Configuration channelConfiguration = channel.getConfiguration();
            processDataReceived(destination, asdu, (String) channelConfiguration.get(DPT),
                    (String) channelConfiguration.get(ADDRESS), channel.getUID());

            // secondly, process the data for the "auxiliary" addresses associated with the channel, if of the right
            // type
            switch (channel.getAcceptedItemType()) {
                case "dimmer": {
                    processDataReceived(destination, asdu, (String) channelConfiguration.get(DPT),
                            (String) channelConfiguration.get(STATE_ADDRESS), channel.getUID());
                    break;
                }
            }

        }
    }

    private void processDataReceived(GroupAddress destination, byte[] asdu, String dpt, String channelAddress,
            ChannelUID channelUID) {

        if (channelAddress != null && dpt != null) {
            GroupAddress channelGroupAddress = null;
            try {
                channelGroupAddress = new GroupAddress(channelAddress);
            } catch (KNXFormatException e) {
                logger.error("An exception occurred while creating a Group Address : '{}'", e.getMessage());
            }

            if (channelGroupAddress != null && channelGroupAddress.equals(destination)) {

                Datapoint datapoint = new CommandDP(destination, getThing().getUID().toString(), 0, dpt);
                Type type = getType(datapoint, asdu);

                if (type != null) {
                    // we need to make sure that we won't send out this event to
                    // the knx bus again, when receiving it on the openHAB bus

                    Set<String> itemSet = this.getLinkedItems(channelUID.getId());

                    for (String anItem : itemSet) {
                        logger.trace("The channel '{}' is bound to item '{}' ", channelUID, anItem);
                        for (ChannelUID cUID : itemChannelLinkRegistry.getBoundChannels(anItem)) {
                            logger.trace("Item '{}' has a channel with id '{}'", anItem, cUID);
                            if (cUID.getBindingId().equals(KNXBindingConstants.BINDING_ID)
                                    && !cUID.toString().equals(channelUID.toString())) {
                                logger.trace("Added event (channel='{}', type='{}') to the ignore event list", cUID,
                                        type.toString());
                                ignoreEventList.add(cUID.toString() + type.toString());
                            }
                        }
                    }

                    if (type instanceof State) {
                        updateState(channelUID, (State) type);
                    } else {
                        postCommand(channelUID, (Command) type);
                    }
                    logger.trace("Processed event (channel='{}', value='{}', destination='{}')",
                            new Object[] { channelUID, type.toString(), destination.toString() });
                } else {
                    final char[] hexCode = "0123456789ABCDEF".toCharArray();
                    StringBuilder sb = new StringBuilder(2 + asdu.length * 2);
                    sb.append("0x");
                    for (byte b : asdu) {
                        sb.append(hexCode[(b >> 4) & 0xF]);
                        sb.append(hexCode[(b & 0xF)]);
                    }

                    logger.warn(
                            "Ignoring KNX bus data: couldn't transform to an openHAB type (not supported). Destination='{}', datapoint='{}', data='{}'",
                            new Object[] { destination.toString(), datapoint.toString(), sb.toString() });
                    return;
                }
            }
        }
    }

    /**
     * Transforms an openHAB type (command or state) into a datapoint type value for the KNX bus.
     *
     * @param type
     *            the openHAB command or state to transform
     * @param dpt
     *            the datapoint type to which should be converted
     *
     * @return the corresponding KNX datapoint type value as a string
     */
    public String toDPTValue(Type type, String dpt) {
        for (KNXTypeMapper typeMapper : typeMappers) {
            String value = typeMapper.toDPTValue(type, dpt);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    public String toDPTid(Class<? extends Type> type) {
        return KNXCoreTypeMapper.toDPTid(type);
    }

    /**
     * Transforms the raw KNX bus data of a given datapoint into an openHAB type (command or state)
     *
     * @param datapoint
     *            the datapoint to which the data belongs
     * @param asdu
     *            the byte array of the raw data from the KNX bus
     * @return the openHAB command or state that corresponds to the data
     */
    public Type getType(Datapoint datapoint, byte[] asdu) {
        for (KNXTypeMapper typeMapper : typeMappers) {
            Type type = typeMapper.toType(datapoint, asdu);
            if (type != null) {
                return type;
            }
        }
        return null;
    }

    public Type getType(GroupAddress destination, String dpt, byte[] asdu) {
        Datapoint datapoint = new CommandDP(destination, getThing().getUID().toString(), 0, dpt);
        return getType(datapoint, asdu);
    }

    protected Set<String> getLinkedItems(String channelId) {
        Channel channel = getThing().getChannel(channelId);
        if (channel != null) {
            return itemChannelLinkRegistry.getLinkedItems(channel.getUID());
        } else {
            throw new IllegalArgumentException("Channel with ID '" + channelId + "' does not exists.");
        }
    }
}
