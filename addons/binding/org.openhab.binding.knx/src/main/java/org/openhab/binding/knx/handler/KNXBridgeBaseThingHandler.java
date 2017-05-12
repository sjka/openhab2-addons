/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.knx.handler;

import java.nio.ByteBuffer;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.core.types.Type;
import org.openhab.binding.knx.GroupAddressListener;
import org.openhab.binding.knx.IndividualAddressListener;
import org.openhab.binding.knx.KNXBindingConstants;
import org.openhab.binding.knx.KNXBusListener;
import org.openhab.binding.knx.internal.dpt.KNXCoreTypeMapper;
import org.openhab.binding.knx.internal.dpt.KNXTypeMapper;
import org.openhab.binding.knx.internal.factory.KNXThreadPoolFactory;
import org.openhab.binding.knx.internal.handler.BridgeConfiguration;
import org.openhab.binding.knx.internal.handler.RetryDatapoint;
import org.openhab.binding.knx.internal.logging.LogAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tuwien.auto.calimero.CloseEvent;
import tuwien.auto.calimero.DetachEvent;
import tuwien.auto.calimero.FrameEvent;
import tuwien.auto.calimero.GroupAddress;
import tuwien.auto.calimero.IndividualAddress;
import tuwien.auto.calimero.cemi.CEMILData;
import tuwien.auto.calimero.datapoint.CommandDP;
import tuwien.auto.calimero.datapoint.Datapoint;
import tuwien.auto.calimero.exception.KNXException;
import tuwien.auto.calimero.exception.KNXIllegalArgumentException;
import tuwien.auto.calimero.exception.KNXTimeoutException;
import tuwien.auto.calimero.link.KNXLinkClosedException;
import tuwien.auto.calimero.link.KNXNetworkLink;
import tuwien.auto.calimero.link.NetworkLinkListener;
import tuwien.auto.calimero.log.LogManager;
import tuwien.auto.calimero.mgmt.Destination;
import tuwien.auto.calimero.mgmt.ManagementClient;
import tuwien.auto.calimero.mgmt.ManagementClientImpl;
import tuwien.auto.calimero.mgmt.ManagementProcedures;
import tuwien.auto.calimero.mgmt.ManagementProceduresImpl;
import tuwien.auto.calimero.process.ProcessCommunicator;
import tuwien.auto.calimero.process.ProcessCommunicatorImpl;
import tuwien.auto.calimero.process.ProcessEvent;
import tuwien.auto.calimero.process.ProcessListenerEx;

/**
 * The {@link KNXBridgeBaseThingHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Karel Goderis - Initial contribution
 */
public abstract class KNXBridgeBaseThingHandler extends BaseBridgeHandler implements NetworkLinkListener {

    public final static int ERROR_INTERVAL_MINUTES = 5;

    private final Logger logger = LoggerFactory.getLogger(KNXBridgeBaseThingHandler.class);

    // Data structures related to the communication infrastructure
    private Set<GroupAddressListener> groupAddressListeners = new ConcurrentHashMap<GroupAddressListener, Boolean>()
            .keySet(Boolean.TRUE);
    private Set<IndividualAddressListener> individualAddressListeners = new ConcurrentHashMap<IndividualAddressListener, Boolean>()
            .keySet(Boolean.TRUE);
    private Set<KNXBusListener> knxBusListeners = new CopyOnWriteArraySet<>();
    private final Collection<KNXTypeMapper> typeMappers = new CopyOnWriteArraySet<>();

    private LinkedBlockingQueue<RetryDatapoint> readDatapoints = new LinkedBlockingQueue<RetryDatapoint>();
    protected ConcurrentHashMap<IndividualAddress, Destination> destinations = new ConcurrentHashMap<IndividualAddress, Destination>();

    // Data structures related to the KNX protocol stack
    private ProcessCommunicator processCommunicator = null;
    private ManagementProcedures managementProcedures;
    private ManagementClient managementClient;
    private KNXNetworkLink link;
    private final LogAdapter logAdapter = new LogAdapter();

    // Data structures related to the various jobs
    private ScheduledFuture<?> connectJob;
    private ScheduledFuture<?> busJob;
    private Boolean connectLock = false;

    private ScheduledExecutorService knxScheduler;

    public boolean shutdown = false;
    private long intervalTimestamp;
    private long errorsSinceStart;
    private long errorsSinceInterval;

    private BridgeConfiguration config;

    private final ProcessListenerEx processListener = new ProcessListenerEx() {

        @Override
        public void detached(DetachEvent e) {
            logger.error("The KNX network link was detached from the process communicator", e.getSource());
        }

        @Override
        public void groupWrite(ProcessEvent e) {
            onGroupWriteEvent(e);
        }

        @Override
        public void groupReadRequest(ProcessEvent e) {
            onGroupReadEvent(e);
        }

        @Override
        public void groupReadResponse(ProcessEvent e) {
            onGroupReadResponseEvent(e);
        }
    };

    @FunctionalInterface
    private interface ReadFunction<T, R> {
        R apply(T t) throws KNXException, InterruptedException;
    }

    public KNXBridgeBaseThingHandler(Bridge bridge) {
        super(bridge);
    }

    @Override
    public void initialize() {
        errorsSinceStart = 0;
        errorsSinceInterval = 0;

        shutdown = false;
        config = getConfigAs(BridgeConfiguration.class);
        registerLogAdapter();
        initializeScheduler();
        scheduleConnectJob();
    }

    @Override
    public void dispose() {
        shutdown = true;
        cancelConnectJob();
        disconnect();
        unregisterLogAdapter();
    }

    private void initializeScheduler() {
        if (knxScheduler == null) {
            knxScheduler = KNXThreadPoolFactory.getPrioritizedScheduledPool(getThing().getUID().getBindingId(), 5);
        }
    }

    private void scheduleConnectJob() {
        logger.trace("Scheduling the connection attempt to the KNX bus");
        connectJob = knxScheduler.schedule(() -> {
            if (!shutdown) {
                connect();
            }
        }, config.getAutoReconnectPeriod().intValue(), TimeUnit.SECONDS);
    }

    private void scheduleAndWaitForConnection() {
        synchronized (connectLock) {
            while (!(getThing().getStatus() == ThingStatus.ONLINE)) {
                if (connectJob.isDone()) {
                    scheduleConnectJob();
                }
                try {
                    connectLock.wait();
                } catch (InterruptedException e) {
                    // Nothing to do here - we move on
                }
            }
        }
    }

    private void cancelConnectJob() {
        if (connectJob != null) {
            connectJob.cancel(true);
        }
    }

    private void registerLogAdapter() {
        LogManager.getManager().addWriter(null, logAdapter);
    }

    private void unregisterLogAdapter() {
        LogManager.getManager().removeWriter(null, logAdapter);
    }

    protected final boolean registerGroupAddressListener(GroupAddressListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("GroupAddressListener must not be null");
        }
        return groupAddressListeners.contains(listener) ? true : groupAddressListeners.add(listener);
    }

    protected final boolean unregisterGroupAddressListener(GroupAddressListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("GroupAddressListener must not be null");
        }
        return groupAddressListeners.remove(listener);
    }

    protected final boolean registerIndividualAddressListener(IndividualAddressListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("IndividualAddressListener must not be null");
        }
        return individualAddressListeners.contains(listener) ? true : individualAddressListeners.add(listener);
    }

    protected final boolean unregisterIndividualAddressListener(IndividualAddressListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("IndividualAddressListener must not be null");
        }
        return individualAddressListeners.remove(listener);
    }

    public final void addKNXTypeMapper(KNXTypeMapper typeMapper) {
        typeMappers.add(typeMapper);
    }

    public final void removeKNXTypeMapper(KNXTypeMapper typeMapper) {
        typeMappers.remove(typeMapper);
    }

    public final void registerKNXBusListener(KNXBusListener knxBusListener) {
        if (knxBusListener != null) {
            knxBusListeners.add(knxBusListener);
        }
    }

    public final void unregisterKNXBusListener(KNXBusListener knxBusListener) {
        if (knxBusListener != null) {
            knxBusListeners.remove(knxBusListener);
        }
    }

    public final int getReadRetriesLimit() {
        return config.getReadRetriesLimit().intValue();
    }

    public final boolean isDiscoveryEnabled() {
        return config.getEnableDiscovery().booleanValue();
    }

    /**
     * Establish a communication channel to the KNX gateway.
     *
     */
    protected abstract KNXNetworkLink establishConnection() throws KNXException;

    private void connect() {
        try {
            closeConnection();

            logger.trace("Connecting to the KNX bus");
            link = establishConnection();

            if (link != null) {
                managementProcedures = new ManagementProceduresImpl(link);

                managementClient = new ManagementClientImpl(link);
                managementClient.setResponseTimeout(config.getResponseTimeout().intValue());

                processCommunicator = new ProcessCommunicatorImpl(link);
                processCommunicator.setResponseTimeout(config.getResponseTimeout().intValue());
                processCommunicator.addProcessListener(processListener);

                link.addLinkListener(this);
            }

            readDatapoints = new LinkedBlockingQueue<RetryDatapoint>();

            errorsSinceStart = 0;
            errorsSinceInterval = 0;

            busJob = knxScheduler.scheduleWithFixedDelay(() -> readInitialValues(), 0,
                    config.getReadingPause().intValue(), TimeUnit.MILLISECONDS);

            updateStatus(ThingStatus.ONLINE);
        } catch (KNXException e) {
            logger.error("Error connecting to the KNX bus: {}", e.getMessage());
            if (logger.isDebugEnabled()) {
                logger.error("", e);
            }
            closeConnection();
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getLocalizedMessage());
        }

        synchronized (connectLock) {
            connectLock.notifyAll();
        }
    }

    private void closeConnection() {
        if (busJob != null) {
            busJob.cancel(true);
            busJob = null;
        }
        if (managementProcedures != null) {
            managementProcedures.detach();
            managementProcedures = null;
        }
        if (managementClient != null) {
            managementClient.detach();
            managementClient = null;
        }
        if (processCommunicator != null) {
            processCommunicator.removeProcessListener(processListener);
            processCommunicator.detach();
            processCommunicator = null;
        }
        if (link != null) {
            link.close();
            link = null;
        }
    }

    private void disconnect() {
        closeConnection();
        updateStatus(ThingStatus.OFFLINE);
    }

    @Override
    public void handleUpdate(ChannelUID channelUID, State newState) {
        // Nothing to do here
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // Nothing to do here
    }

    public void readInitialValues() {
        scheduleAndWaitForConnection();

        if (getThing().getStatus() == ThingStatus.ONLINE) {

            RetryDatapoint datapoint = readDatapoints.poll();

            if (datapoint != null) {
                datapoint.incrementRetries();

                boolean success = false;
                try {
                    logger.trace("Sending a Group Read Request telegram for destination '{}'",
                            datapoint.getDatapoint().getMainAddress());
                    processCommunicator.read(datapoint.getDatapoint());
                    success = true;
                } catch (KNXException e) {
                    logger.warn("Cannot read value for datapoint '{}' from KNX bus: {}",
                            datapoint.getDatapoint().getMainAddress(), e.getMessage());

                } catch (KNXIllegalArgumentException e) {
                    logger.warn("Error sending KNX read request for datapoint '{}': {}",
                            datapoint.getDatapoint().getMainAddress(), e.getMessage());
                    updateStatus(ThingStatus.OFFLINE);
                } catch (InterruptedException e) {
                    logger.warn("Error sending KNX read request for datapoint '{}': {}",
                            datapoint.getDatapoint().getMainAddress(), e.getMessage());
                }
                if (!success) {
                    if (datapoint.getRetries() < datapoint.getLimit()) {
                        readDatapoints.add(datapoint);
                    } else {
                        logger.debug("Giving up reading datapoint {} - nubmer of maximum retries ({}) reached.",
                                datapoint.getDatapoint().getMainAddress(), datapoint.getLimit());
                    }
                }
            }
        }
    }

    public void readDatapoint(Datapoint datapoint, int retriesLimit) {
        synchronized (this) {
            if (datapoint != null) {
                RetryDatapoint retryDatapoint = new RetryDatapoint(datapoint, retriesLimit);

                if (!readDatapoints.contains(retryDatapoint)) {
                    readDatapoints.add(retryDatapoint);
                }
            }
        }
    }

    /**
     * Handles the given {@link ProcessEvent}. If the KNX ASDU is valid
     * it is passed on to the {@link IndividualAddressListener}s and {@link GroupAddressListener}s that are interested
     * in the telegram, and subsequently to the
     * {@link KNXBusListener}s that are interested in all KNX bus activity
     *
     * @param e the {@link ProcessEvent} to handle.
     */
    private void onGroupWriteEvent(ProcessEvent e) {
        try {
            GroupAddress destination = e.getDestination();
            IndividualAddress source = e.getSourceAddr();
            byte[] asdu = e.getASDU();
            if (asdu.length == 0) {
                return;
            }

            logger.trace("Received a Group Write telegram from '{}' for destination '{}'", e.getSourceAddr(),
                    destination);

            for (GroupAddressListener listener : groupAddressListeners) {
                if (listener.listensTo(destination)) {
                    knxScheduler.schedule(() -> listener.onGroupWrite(this, source, destination, asdu), 0,
                            TimeUnit.SECONDS);
                }
            }

            for (KNXBusListener listener : knxBusListeners) {
                listener.onActivity(e.getSourceAddr(), destination, asdu);
            }
        } catch (RuntimeException re) {
            logger.error("An exception occurred while receiving event from the KNX bus : '{}'", re.getMessage(), re);
        }
    }

    /**
     * Handles the given {@link ProcessEvent}. If the KNX ASDU is valid
     * it is passed on to the {@link IndividualAddressListener}s and {@link GroupAddressListener}s that are interested
     * in the telegram, and subsequently to the
     * {@link KNXBusListener}s that are interested in all KNX bus activity
     *
     * @param e the {@link ProcessEvent} to handle.
     */
    private void onGroupReadEvent(ProcessEvent e) {
        try {
            GroupAddress destination = e.getDestination();
            IndividualAddress source = e.getSourceAddr();

            logger.trace("Received a Group Read telegram from '{}' for destination '{}'", e.getSourceAddr(),
                    destination);

            byte[] asdu = e.getASDU();

            for (GroupAddressListener listener : groupAddressListeners) {
                if (listener.listensTo(destination)) {
                    if (listener instanceof IndividualAddressListener
                            && !((IndividualAddressListener) listener).listensTo(source)) {
                        listener.onGroupRead(this, source, destination, asdu);
                    } else {
                        listener.onGroupRead(this, source, destination, asdu);
                    }
                }
            }

            for (KNXBusListener listener : knxBusListeners) {
                listener.onActivity(e.getSourceAddr(), destination, asdu);
            }

        } catch (RuntimeException re) {
            logger.error("An exception occurred while receiving event from the KNX bus : '{}'", re.getMessage(), re);
        }
    }

    /**
     * Handles the given {@link ProcessEvent}. If the KNX ASDU is valid
     * it is passed on to the {@link IndividualAddressListener}s and {@link GroupAddressListener}s that are interested
     * in the telegram, and subsequently to the
     * {@link KNXBusListener}s that are interested in all KNX bus activity
     *
     * @param e the {@link ProcessEvent} to handle.
     */
    private void onGroupReadResponseEvent(ProcessEvent e) {
        try {
            GroupAddress destination = e.getDestination();
            IndividualAddress source = e.getSourceAddr();
            byte[] asdu = e.getASDU();
            if (asdu.length == 0) {
                return;
            }

            logger.trace("Received a Group Read Response telegram from '{}' for destination '{}'", e.getSourceAddr(),
                    destination);

            for (GroupAddressListener listener : groupAddressListeners) {
                if (listener.listensTo(destination)) {
                    if (listener instanceof IndividualAddressListener
                            && !((IndividualAddressListener) listener).listensTo(source)) {
                        listener.onGroupReadResponse(this, source, destination, asdu);
                    } else {
                        listener.onGroupReadResponse(this, source, destination, asdu);
                    }
                }
            }

            for (KNXBusListener listener : knxBusListeners) {
                listener.onActivity(e.getSourceAddr(), destination, asdu);
            }

        } catch (RuntimeException re) {
            logger.error("An exception occurred while receiving event from the KNX bus : '{}'", re.getMessage(), re);
        }
    }

    public void writeToKNX(GroupAddress address, String dpt, Type value) {

        if (dpt != null && address != null && value != null) {

            // ProcessCommunicator pc = getCommunicator();
            Datapoint datapoint = new CommandDP(address, getThing().getUID().toString(), 0, dpt);

            scheduleAndWaitForConnection();

            if (datapoint != null && getThing().getStatus() == ThingStatus.ONLINE) {
                try {
                    String mappedValue = toDPTValue(value, datapoint.getDPT());
                    if (mappedValue != null) {
                        processCommunicator.write(datapoint, mappedValue);
                        logger.debug("Wrote value '{}' to datapoint '{}'", value, datapoint);
                    } else {
                        logger.debug("Value '{}' can not be mapped to datapoint '{}'", value, datapoint);
                    }
                } catch (KNXException e) {
                    logger.debug(
                            "Value '{}' could not be sent to the KNX bus using datapoint '{}' - retrying one time: {}",
                            new Object[] { value, datapoint, e.getMessage() });
                    try {
                        // do a second try, maybe the reconnection was successful
                        // pc = getCommunicator();
                        processCommunicator.write(datapoint, toDPTValue(value, datapoint.getDPT()));
                        logger.debug("Wrote value '{}' to datapoint '{}' on second try", value, datapoint);
                    } catch (KNXException e1) {
                        logger.error(
                                "Value '{}' could not be sent to the KNX bus using datapoint '{}' - giving up after second try: {}",
                                new Object[] { value, datapoint, e1.getMessage() });
                        updateStatus(ThingStatus.OFFLINE);
                    }
                }
            } else {
                logger.error("Can not write to the KNX bus (pc {}, link {})",
                        processCommunicator == null ? "Not OK" : "OK",
                        link == null ? "Not OK" : (link.isOpen() ? "Open" : "Closed"));
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

    public Class<? extends Type> toTypeClass(String dpt) {
        return KNXCoreTypeMapper.toTypeClass(dpt);
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

    public synchronized boolean isReachable(IndividualAddress address) {
        if (managementProcedures != null) {
            try {
                return managementProcedures.isAddressOccupied(address);
            } catch (KNXException | InterruptedException e) {
                logger.error("Could not reach address '{}': {}", address.toString(), e.getMessage());
                if (logger.isDebugEnabled()) {
                    logger.error("", e);
                }
            }
        }
        return false;
    }

    public synchronized void restartNetworkDevice(IndividualAddress address) {
        if (address != null) {
            Destination destination = managementClient.createDestination(address, true);
            try {
                managementClient.restart(destination);
            } catch (KNXTimeoutException | KNXLinkClosedException e) {
                logger.error("Could not reset the device with address '{}': {}", address, e.getMessage());
                if (logger.isDebugEnabled()) {
                    logger.error("", e);
                }
            }
        }
    }

    public synchronized IndividualAddress[] scanNetworkDevices(final int area, final int line) {
        try {
            return managementProcedures.scanNetworkDevices(area, line);
        } catch (final Exception e) {
            logger.error("Error scanning the KNX bus: {}", e.getMessage());
            if (logger.isDebugEnabled()) {
                logger.error("", e);
            }
        }
        return null;
    }

    public synchronized IndividualAddress[] scanNetworkRouters() {
        try {
            return managementProcedures.scanNetworkRouters();
        } catch (final Exception e) {
            logger.error("An exception occurred while scanning the KNX bus: {}", e.getMessage());
            if (logger.isDebugEnabled()) {
                logger.error("", e);
            }
        }
        return null;
    }

    private byte[] readFromManagementClient(String task, long timeout, IndividualAddress address,
            ReadFunction<Destination, byte[]> function) {
        final long start = System.nanoTime();
        while ((System.nanoTime() - start) < TimeUnit.MILLISECONDS.toNanos(timeout)) {
            Destination destination = null;
            try {
                logger.debug("Going to {} of {} ", task, address);
                destination = managementClient.createDestination(address, true);
                byte[] result = function.apply(destination);
                logger.debug("Finished to {} of {}, result: {}", task, address, result == null ? null : result.length);
                return result;
            } catch (KNXException e) {
                logger.error("Could not {} of {}: {}", task, address, e.getMessage());
                if (logger.isDebugEnabled()) {
                    logger.error("", e);
                }
            } catch (InterruptedException e) {
                logger.debug("Interrupted to {}", task);
                return null;
            } finally {
                if (destination != null) {
                    destination.destroy();
                }
            }
        }
        return null;
    }

    private void authorize(boolean authenticate, Destination destination) throws KNXException, InterruptedException {
        if (authenticate) {
            managementClient.authorize(destination, (ByteBuffer.allocate(4)).put((byte) 0xFF).put((byte) 0xFF)
                    .put((byte) 0xFF).put((byte) 0xFF).array());
        }
    }

    public synchronized byte[] readDeviceDescription(IndividualAddress address, int descType, boolean authenticate,
            long timeout) {
        String task = "read the device description";
        return readFromManagementClient(task, timeout, address, destination -> {
            authorize(authenticate, destination);
            return managementClient.readDeviceDesc(destination, descType);
        });
    }

    public synchronized byte[] readDeviceMemory(IndividualAddress address, int startAddress, int bytes,
            boolean authenticate, long timeout) {
        String task = MessageFormat.format("read {0} bytes at memory location {1}", bytes, startAddress);
        return readFromManagementClient(task, timeout, address, destination -> {
            authorize(authenticate, destination);
            return managementClient.readMemory(destination, startAddress, bytes);
        });
    }

    public synchronized byte[] readDeviceProperties(IndividualAddress address, final int interfaceObjectIndex,
            final int propertyId, final int start, final int elements, boolean authenticate, long timeout) {
        String task = MessageFormat.format("read device property {} at index {}", propertyId, interfaceObjectIndex);
        return readFromManagementClient(task, timeout, address, destination -> {
            authorize(authenticate, destination);
            return managementClient.readProperty(destination, interfaceObjectIndex, propertyId, start, elements);
        });
    }

    public enum EventSource {
        RUNTIME,
        BUS,
        EMPTY;
    }

    protected class LoggedEvent {
        long timeStamp;
        EventSource source;
        ChannelUID channel;
        Type type;
    }

    public ScheduledExecutorService getScheduler() {
        return knxScheduler;
    }

    @Override
    public void linkClosed(CloseEvent e) {
        if (!link.isOpen() && !(CloseEvent.USER_REQUEST == e.getInitiator()) && !shutdown) {
            logger.warn("KNX link has been lost (reason: {} on object {}) - reconnecting...", e.getReason(),
                    e.getSource().toString());
            if (config.getAutoReconnectPeriod().intValue() > 0) {
                logger.info("KNX link will be retried in " + config.getAutoReconnectPeriod().intValue() + " seconds");
                if (connectJob.isDone()) {
                    scheduleConnectJob();
                }
            }
        }
    }

    @Override
    public void indication(FrameEvent e) {
        handleFrameEvent(e);
    }

    @Override
    public void confirmation(FrameEvent e) {
        handleFrameEvent(e);
    }

    private void handleFrameEvent(FrameEvent e) {
        checkErrorCounterTimeouts();
        int messageCode = e.getFrame().getMessageCode();
        switch (messageCode) {
            case CEMILData.MC_LDATA_IND:
                if (((CEMILData) e.getFrame()).isRepetition()) {
                    incrementErrorCounter();
                }
                break;
            case CEMILData.MC_LDATA_CON:
                if (!((CEMILData) e.getFrame()).isPositiveConfirmation()) {
                    incrementErrorCounter();
                }
                break;
        }
    }

    private void checkErrorCounterTimeouts() {
        if (intervalTimestamp == 0) {
            intervalTimestamp = System.nanoTime();
            updateErrorCounterChannels();
        } else if ((System.nanoTime() - intervalTimestamp) > TimeUnit.MINUTES.toNanos(ERROR_INTERVAL_MINUTES)) {
            intervalTimestamp = System.nanoTime();
            errorsSinceInterval = 0;
            updateErrorCounterChannels();
        }
    }

    private void incrementErrorCounter() {
        errorsSinceStart++;
        errorsSinceInterval++;
        updateErrorCounterChannels();
    }

    private void updateErrorCounterChannels() {
        updateState(new ChannelUID(getThing().getUID(), KNXBindingConstants.ERRORS_STARTUP),
                new DecimalType(errorsSinceStart));
        updateState(new ChannelUID(getThing().getUID(), KNXBindingConstants.ERRORS_INTERVAL),
                new DecimalType(errorsSinceInterval));
    }

    public String getETSProjectFilename() {
        return config.getKnxProj();
    }
}
