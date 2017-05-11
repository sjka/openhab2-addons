/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.knx.handler;

import static org.openhab.binding.knx.KNXBindingConstants.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.common.registry.ProviderChangeListener;
import org.eclipse.smarthome.core.items.GenericItem;
import org.eclipse.smarthome.core.library.items.ColorItem;
import org.eclipse.smarthome.core.library.items.ContactItem;
import org.eclipse.smarthome.core.library.items.DateTimeItem;
import org.eclipse.smarthome.core.library.items.DimmerItem;
import org.eclipse.smarthome.core.library.items.NumberItem;
import org.eclipse.smarthome.core.library.items.RollershutterItem;
import org.eclipse.smarthome.core.library.items.StringItem;
import org.eclipse.smarthome.core.library.items.SwitchItem;
import org.eclipse.smarthome.core.library.types.DateTimeType;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.HSBType;
import org.eclipse.smarthome.core.library.types.IncreaseDecreaseType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.OpenClosedType;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.eclipse.smarthome.core.library.types.StopMoveType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.library.types.UpDownType;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingProvider;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.thing.binding.builder.ChannelBuilder;
import org.eclipse.smarthome.core.thing.type.ChannelKind;
import org.eclipse.smarthome.core.thing.type.ChannelTypeUID;
import org.eclipse.smarthome.core.thing.util.ThingHelper;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.core.types.Type;
import org.openhab.binding.knx.GroupAddressListener;
import org.openhab.binding.knx.IndividualAddressListener;
import org.openhab.binding.knx.KNXBindingConstants;
import org.openhab.binding.knx.KNXBusListener;
import org.openhab.binding.knx.KNXProjectProvider;
import org.openhab.binding.knx.TelegramListener;
import org.openhab.binding.knx.internal.dpt.KNXCoreTypeMapper;
import org.openhab.binding.knx.internal.dpt.KNXTypeMapper;
import org.openhab.binding.knx.internal.factory.KNXHandlerFactory;
import org.openhab.binding.knx.internal.factory.KNXThreadPoolFactory;
import org.openhab.binding.knx.internal.logging.LogAdapter;
import org.openhab.binding.knx.internal.parser.KNXProject13Parser;
import org.openhab.binding.knx.internal.parser.KNXProjectParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

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
import tuwien.auto.calimero.exception.KNXRemoteException;
import tuwien.auto.calimero.exception.KNXTimeoutException;
import tuwien.auto.calimero.link.KNXLinkClosedException;
import tuwien.auto.calimero.link.KNXNetworkLink;
import tuwien.auto.calimero.link.NetworkLinkListener;
import tuwien.auto.calimero.log.LogManager;
import tuwien.auto.calimero.mgmt.Destination;
import tuwien.auto.calimero.mgmt.KNXDisconnectException;
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
public abstract class KNXBridgeBaseThingHandler extends BaseBridgeHandler implements KNXProjectProvider, ThingProvider {

    public final static int ERROR_INTERVAL_MINUTES = 5;

    private final Logger logger = LoggerFactory.getLogger(KNXBridgeBaseThingHandler.class);

    // Data structures related to the communication infrastructure
    private Set<GroupAddressListener> groupAddressListeners = new ConcurrentHashMap<GroupAddressListener, Boolean>()
            .keySet(Boolean.TRUE);
    private Set<IndividualAddressListener> individualAddressListeners = new ConcurrentHashMap<IndividualAddressListener, Boolean>()
            .keySet(Boolean.TRUE);
    private Set<KNXBusListener> knxBusListeners = new ConcurrentHashMap<KNXBusListener, Boolean>().keySet(Boolean.TRUE);
    private Collection<KNXTypeMapper> typeMappers;
    private LinkedBlockingQueue<RetryDatapoint> readDatapoints = new LinkedBlockingQueue<RetryDatapoint>();
    protected ConcurrentHashMap<IndividualAddress, Destination> destinations = new ConcurrentHashMap<IndividualAddress, Destination>();
    private static Map<Class<? extends Type>, Class<? extends GenericItem>> typeItemMap = new HashMap<Class<? extends Type>, Class<? extends GenericItem>>();

    // Data structures related to the knxproj parsing and thingprovider
    private KNXHandlerFactory factory;
    private KNXProjectParser knxParser;
    private Set<File> knxProjects = new CopyOnWriteArraySet<File>();
    private HashMap<File, HashMap<ThingUID, Thing>> providedThings = new HashMap<File, HashMap<ThingUID, Thing>>();
    protected List<ProviderChangeListener<Thing>> providerListeners = new CopyOnWriteArrayList<ProviderChangeListener<Thing>>();

    // Data structures related to the KNX protocol stack
    private ProcessCommunicator pc = null;
    private ProcessListenerEx pl = null;
    private NetworkLinkListener nll = null;
    private ManagementProcedures mp;
    private ManagementClient mc;
    protected KNXNetworkLink link;
    private final LogAdapter logAdapter = new LogAdapter();

    // Data structures related to the various jobs
    private ScheduledFuture<?> connectJob;
    private ScheduledFuture<?> busJob;
    private List<ScheduledFuture<?>> readFutures = new ArrayList<ScheduledFuture<?>>();
    private Boolean connectLock = false;

    private ScheduledExecutorService knxScheduler;

    public boolean shutdown = false;
    private long intervalTimestamp;
    private long errorsSinceStart;
    private long errorsSinceInterval;

    public KNXBridgeBaseThingHandler(Bridge bridge, Collection<KNXTypeMapper> typeMappers, KNXHandlerFactory factory) {
        super(bridge);
        this.typeMappers = typeMappers;
        this.factory = factory;

        typeItemMap.put(DateTimeType.class, DateTimeItem.class);
        typeItemMap.put(DecimalType.class, NumberItem.class);
        typeItemMap.put(HSBType.class, ColorItem.class);
        typeItemMap.put(IncreaseDecreaseType.class, DimmerItem.class);
        typeItemMap.put(OnOffType.class, SwitchItem.class);
        typeItemMap.put(OpenClosedType.class, ContactItem.class);
        typeItemMap.put(PercentType.class, NumberItem.class);
        typeItemMap.put(StopMoveType.class, RollershutterItem.class);
        typeItemMap.put(StringType.class, StringItem.class);
        typeItemMap.put(UpDownType.class, RollershutterItem.class);
    }

    @Override
    public void initialize() {

        errorsSinceStart = 0;
        errorsSinceInterval = 0;

        LogManager.getManager().addWriter(null, logAdapter);

        shutdown = false;

        if (knxScheduler == null) {
            knxScheduler = KNXThreadPoolFactory.getPrioritizedScheduledPool(getThing().getUID().getBindingId(), 5);
        }

        logger.trace("Scheduling the connection attempt to the KNX bus");
        connectJob = knxScheduler.schedule(connectRunnable,
                ((BigDecimal) getConfig().get(AUTO_RECONNECT_PERIOD)).intValue(), TimeUnit.SECONDS);
    }

    @Override
    public void dispose() {

        shutdown = true;

        if (connectJob != null) {
            connectJob.cancel(true);
        }

        disconnect();

        LogManager.getManager().removeWriter(null, logAdapter);
    }

    public boolean registerGroupAddressListener(GroupAddressListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("It's not allowed to pass a null GroupAddressListener.");
        }

        return groupAddressListeners.contains(listener) ? true : groupAddressListeners.add(listener);
    }

    public boolean unregisterGroupAddressListener(GroupAddressListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("It's not allowed to pass a null GroupAddressListener.");
        }

        return groupAddressListeners.remove(listener);
    }

    public boolean registerIndividualAddressListener(IndividualAddressListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("It's not allowed to pass a null IndividualAddressListener.");
        }

        return individualAddressListeners.contains(listener) ? true : individualAddressListeners.add(listener);
    }

    public boolean unregisterIndividualAddressListener(IndividualAddressListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("It's not allowed to pass a null IndividualAddressListener.");
        }

        return individualAddressListeners.remove(listener);
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
    public void addProviderChangeListener(ProviderChangeListener<Thing> listener) {
        providerListeners.add(listener);
    }

    @Override
    public void removeProviderChangeListener(ProviderChangeListener<Thing> listener) {
        providerListeners.remove(listener);
    }

    public int getReadRetriesLimit() {
        return ((BigDecimal) getConfig().get(READ_RETRIES_LIMIT)).intValue();
    }

    public boolean isDiscoveryEnabled() {
        return ((Boolean) getConfig().get(ENABLE_DISCOVERY));
    }

    public abstract void establishConnection() throws KNXException;

    Runnable connectRunnable = new Runnable() {
        @Override
        public void run() {
            if (shutdown) {
                connectJob.cancel(true);
                updateStatus(ThingStatus.ONLINE);
            } else {
                updateStatus(ThingStatus.OFFLINE);
                connect();
            }
        }
    };

    public void connect() {
        try {

            logger.trace("Connecting to the KNX bus");

            if (mp != null) {
                mp.detach();
            }

            if (mc != null) {
                mc.detach();
            }

            if (pc != null) {
                if (pl != null) {
                    pc.removeProcessListener(pl);
                }
                pc.detach();
            }

            if (link != null && link.isOpen()) {
                link.close();
            }

            establishConnection();

            nll = new NetworkLinkListener() {
                @Override
                public void linkClosed(CloseEvent e) {
                    if (!link.isOpen() && !(CloseEvent.USER_REQUEST == e.getInitiator()) && !shutdown) {
                        logger.warn("KNX link has been lost (reason: {} on object {}) - reconnecting...", e.getReason(),
                                e.getSource().toString());
                        if (((BigDecimal) getConfig().get(AUTO_RECONNECT_PERIOD)).intValue() > 0) {
                            logger.info("KNX link will be retried in "
                                    + ((BigDecimal) getConfig().get(AUTO_RECONNECT_PERIOD)).intValue() + " seconds");

                            if (connectJob.isDone()) {
                                logger.debug("NLL is scheduling a connection attempt");
                                connectJob = knxScheduler.schedule(connectRunnable,
                                        ((BigDecimal) getConfig().get(AUTO_RECONNECT_PERIOD)).intValue(),
                                        TimeUnit.SECONDS);
                            }
                        }
                    }
                }

                @Override
                public void indication(FrameEvent e) {
                    CEMILData cemid = (CEMILData) e.getFrame();

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
                    CEMILData cemid = (CEMILData) e.getFrame();

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

            pl = new ProcessListenerEx() {

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

            if (link != null) {
                mp = new ManagementProceduresImpl(link);

                mc = new ManagementClientImpl(link);
                mc.setResponseTimeout((((BigDecimal) getConfig().get(RESPONSE_TIME_OUT)).intValue() / 1000));

                pc = new ProcessCommunicatorImpl(link);
                pc.setResponseTimeout(((BigDecimal) getConfig().get(RESPONSE_TIME_OUT)).intValue() / 1000);
                pc.addProcessListener(pl);

                link.addLinkListener(nll);
            }

            if (busJob != null) {
                busJob.cancel(true);
            }

            if (readFutures != null) {
                for (ScheduledFuture<?> readJob : readFutures) {
                    readJob.cancel(true);
                }
            } else {
                readFutures = new ArrayList<ScheduledFuture<?>>();
            }

            readDatapoints = new LinkedBlockingQueue<RetryDatapoint>();

            errorsSinceStart = 0;
            errorsSinceInterval = 0;

            if (busJob == null) {
                busJob = knxScheduler.scheduleWithFixedDelay(new BusRunnable(), 0,
                        ((BigDecimal) getConfig().get(READING_PAUSE)).intValue(), TimeUnit.MILLISECONDS);
            }

            updateStatus(ThingStatus.ONLINE);

        } catch (KNXException e) {
            logger.error("An exception occurred while connecting to the KNX bus: {}", e.getMessage(), e);
            disconnect();
            updateStatus(ThingStatus.OFFLINE);
        }

        try {
            synchronized (connectLock) {
                connectLock.notifyAll();
            }
        } catch (Exception e) {
            logger.error("An exception occurred while connecting to the KNX bus : '{}'", e.getMessage(), e);
        }
    }

    public void disconnect() {

        if (busJob != null) {
            busJob.cancel(true);
            busJob = null;
        }

        if (readFutures != null) {
            for (ScheduledFuture<?> readJob : readFutures) {
                if (!readJob.isDone()) {
                    readJob.cancel(true);
                }
            }
        }

        if (pc != null) {
            if (pl != null) {
                pc.removeProcessListener(pl);
            }
            pc.detach();
        }

        if (mp != null) {
            mp.detach();
        }

        if (mc != null) {
            mc.detach();
        }

        if (nll != null) {
            link.removeLinkListener(nll);
        }

        if (link != null) {
            link.close();
        }

    }

    @Override
    public void handleUpdate(ChannelUID channelUID, State newState) {
        // Nothing to do here
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // Nothing to do here
    }

    public class BusRunnable implements Runnable {

        @Override
        public void run() {

            synchronized (connectLock) {
                while (!(getThing().getStatus() == ThingStatus.ONLINE)) {
                    if (connectJob.isDone()) {
                        connectJob = knxScheduler.schedule(connectRunnable,
                                ((BigDecimal) getConfig().get(AUTO_RECONNECT_PERIOD)).intValue(), TimeUnit.SECONDS);
                    }
                    try {
                        connectLock.wait();
                    } catch (InterruptedException e) {
                        // Nothing to do here - we move on
                    }
                }
            }

            if (getThing().getStatus() == ThingStatus.ONLINE) {

                RetryDatapoint datapoint = readDatapoints.poll();

                if (datapoint != null) {
                    datapoint.incrementRetries();

                    boolean success = false;
                    try {
                        logger.trace("Sending a Group Read Request telegram for destination '{}'",
                                datapoint.getDatapoint().getMainAddress());
                        pc.read(datapoint.getDatapoint());
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

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + getOuterType().hashCode();
            result = prime * result
                    + ((datapoint.getMainAddress() == null) ? 0 : datapoint.getMainAddress().hashCode());
            result = prime * result + limit;
            result = prime * result + retries;
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            RetryDatapoint other = (RetryDatapoint) obj;
            if (!getOuterType().equals(other.getOuterType())) {
                return false;
            }
            if (datapoint == null) {
                if (other.datapoint != null) {
                    return false;
                }
            } else if (!datapoint.getMainAddress().equals(other.datapoint.getMainAddress())) {
                return false;
            }

            return true;
        }

        private KNXBridgeBaseThingHandler getOuterType() {
            return KNXBridgeBaseThingHandler.this;
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

    private class OnGroupWriteRunnable implements Runnable {

        TelegramListener listener;
        KNXBridgeBaseThingHandler bridge;
        IndividualAddress source;
        GroupAddress destination;
        byte[] asdu;

        public OnGroupWriteRunnable(TelegramListener listener, KNXBridgeBaseThingHandler bridge,
                IndividualAddress source, GroupAddress destination, byte[] asdu) {
            this.listener = listener;
            this.bridge = bridge;
            this.source = source;
            this.destination = destination;
            this.asdu = asdu;
        }

        @Override
        public void run() {
            listener.onGroupWrite(bridge, source, destination, asdu);
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

            for (IndividualAddressListener listener : individualAddressListeners) {
                if (listener.listensTo(source)) {
                    knxScheduler.schedule(new OnGroupWriteRunnable(listener, this, source, destination, asdu), 0,
                            TimeUnit.SECONDS);
                }
            }

            for (GroupAddressListener listener : groupAddressListeners) {
                if (listener.listensTo(destination)) {
                    knxScheduler.schedule(new OnGroupWriteRunnable(listener, this, source, destination, asdu), 0,
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

            for (IndividualAddressListener listener : individualAddressListeners) {
                if (listener.listensTo(source)) {
                    if (listener instanceof GroupAddressListener
                            && ((GroupAddressListener) listener).listensTo(destination)) {
                        listener.onGroupRead(this, source, destination, asdu);
                    } else {
                        listener.onGroupRead(this, source, destination, asdu);
                    }
                }
            }

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

            for (IndividualAddressListener listener : individualAddressListeners) {
                if (listener.listensTo(source)) {
                    if (listener instanceof GroupAddressListener
                            && ((GroupAddressListener) listener).listensTo(destination)) {
                        listener.onGroupReadResponse(this, source, destination, asdu);
                    } else {
                        listener.onGroupReadResponse(this, source, destination, asdu);
                    }
                }
            }

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

            synchronized (connectLock) {
                while (!(getThing().getStatus() == ThingStatus.ONLINE)) {
                    if (connectJob.isDone()) {
                        connectJob = knxScheduler.schedule(connectRunnable,
                                ((BigDecimal) getConfig().get(AUTO_RECONNECT_PERIOD)).intValue(), TimeUnit.SECONDS);
                    }
                    try {
                        connectLock.wait();
                    } catch (InterruptedException e) {
                        // Nothing to do here
                    }
                }
            }

            if (datapoint != null && getThing().getStatus() == ThingStatus.ONLINE) {
                try {
                    String mappedValue = toDPTValue(value, datapoint.getDPT());
                    if (mappedValue != null) {
                        pc.write(datapoint, mappedValue);
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
                        pc.write(datapoint, toDPTValue(value, datapoint.getDPT()));
                        logger.debug("Wrote value '{}' to datapoint '{}' on second try", value, datapoint);
                    } catch (KNXException e1) {
                        logger.error(
                                "Value '{}' could not be sent to the KNX bus using datapoint '{}' - giving up after second try: {}",
                                new Object[] { value, datapoint, e1.getMessage() });
                        updateStatus(ThingStatus.OFFLINE);
                    }
                }
            } else {
                logger.error("Can not write to the KNX bus (pc {}, link {})", pc == null ? "Not OK" : "OK",
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

    synchronized public boolean isReachable(IndividualAddress address) {
        if (mp != null) {
            try {
                return mp.isAddressOccupied(address);
            } catch (KNXException | InterruptedException e) {
                logger.error("An exception occurred while trying to reach address '{}' : {}", address.toString(),
                        e.getMessage(), e);
            }
        }

        return false;
    }

    synchronized public void restartNetworkDevice(IndividualAddress address) {
        if (address != null) {
            Destination destination = mc.createDestination(address, true);
            try {
                mc.restart(destination);
            } catch (KNXTimeoutException | KNXLinkClosedException e) {
                logger.error("An exception occurred while resetting the device with address {} : {}", address,
                        e.getMessage(), e);
            }
        }
    }

    synchronized public IndividualAddress[] scanNetworkDevices(final int area, final int line) {
        try {
            return mp.scanNetworkDevices(area, line);
        } catch (final Exception e) {
            logger.error("An exception occurred while scanning the KNX bus : '{}'", e.getMessage(), e);
        }

        return null;
    }

    synchronized public IndividualAddress[] scanNetworkRouters() {
        try {
            return mp.scanNetworkRouters();
        } catch (final Exception e) {
            logger.error("An exception occurred while scanning the KNX bus : '{}'", e.getMessage(), e);
        }

        return null;
    }

    synchronized public byte[] readDeviceDescription(IndividualAddress address, int descType, boolean authenticate,
            long timeout) {

        Destination destination = null;
        boolean success = false;
        byte[] result = null;
        long now = System.currentTimeMillis();

        while (!success && (System.currentTimeMillis() - now) < timeout) {
            try {

                logger.debug("Reading Device Description of {} ", address);

                destination = mc.createDestination(address, true);

                if (authenticate) {
                    int access = mc.authorize(destination, (ByteBuffer.allocate(4)).put((byte) 0xFF).put((byte) 0xFF)
                            .put((byte) 0xFF).put((byte) 0xFF).array());
                }

                result = mc.readDeviceDesc(destination, descType);
                logger.debug("Reading Device Description of {} yields {} bytes", address,
                        result == null ? null : result.length);

                success = true;
            } catch (Exception e) {
                logger.error("An exception occurred while trying to read the device description for address '{}' : {}",
                        address.toString(), e.getMessage(), e);
            } finally {
                if (destination != null) {
                    destination.destroy();
                }
            }
        }

        return result;
    }

    synchronized public byte[] readDeviceMemory(IndividualAddress address, int startAddress, int bytes,
            boolean authenticate, long timeout) {

        boolean success = false;
        byte[] result = null;
        long now = System.currentTimeMillis();

        while (!success && (System.currentTimeMillis() - now) < timeout) {
            Destination destination = null;
            try {

                logger.debug("Reading {} bytes at memory location {} of device {}",
                        new Object[] { bytes, startAddress, address });

                destination = mc.createDestination(address, true);

                if (authenticate) {
                    int access = mc.authorize(destination, (ByteBuffer.allocate(4)).put((byte) 0xFF).put((byte) 0xFF)
                            .put((byte) 0xFF).put((byte) 0xFF).array());
                }

                result = mc.readMemory(destination, startAddress, bytes);
                logger.debug("Reading {} bytes at memory location {} of device {} yields {} bytes",
                        new Object[] { bytes, startAddress, address, result == null ? null : result.length });

                success = true;
            } catch (KNXTimeoutException e) {
                logger.error("An KNXTimeoutException occurred while trying to read the memory for address '{}' : {}",
                        address.toString(), e.getMessage(), e);
            } catch (KNXRemoteException e) {
                logger.error("An KNXRemoteException occurred while trying to read the memory for '{}' : {}",
                        address.toString(), e.getMessage(), e);
            } catch (KNXDisconnectException e) {
                logger.error("An KNXDisconnectException occurred while trying to read the memory for '{}' : {}",
                        address.toString(), e.getMessage(), e);
            } catch (KNXLinkClosedException e) {
                logger.error("An KNXLinkClosedException occurred while trying to read the memory for '{}' : {}",
                        address.toString(), e.getMessage(), e);
            } catch (KNXException e) {
                logger.error("An KNXException occurred while trying to read the memory for '{}' : {}",
                        address.toString(), e.getMessage(), e);
            } catch (InterruptedException e) {
                logger.error("An exception occurred while trying to read the memory for '{}' : {}", address.toString(),
                        e.getMessage(), e);
                e.printStackTrace();
            } finally {
                if (destination != null) {
                    destination.destroy();
                }
            }
        }

        return result;
    }

    synchronized public byte[] readDeviceProperties(IndividualAddress address, final int interfaceObjectIndex,
            final int propertyId, final int start, final int elements, boolean authenticate, long timeout) {

        boolean success = false;
        byte[] result = null;
        long now = System.currentTimeMillis();

        while (!success && (System.currentTimeMillis() - now) < timeout) {
            Destination destination = null;
            try {
                logger.debug("Reading device property {} at index {} for {}", new Object[] { propertyId,
                        interfaceObjectIndex, address, result == null ? null : result.length });

                destination = mc.createDestination(address, true);

                if (authenticate) {
                    int access = mc.authorize(destination, (ByteBuffer.allocate(4)).put((byte) 0xFF).put((byte) 0xFF)
                            .put((byte) 0xFF).put((byte) 0xFF).array());
                }

                result = mc.readProperty(destination, interfaceObjectIndex, propertyId, start, elements);

                logger.debug("Reading device property {} at index {} for {} yields {} bytes", new Object[] { propertyId,
                        interfaceObjectIndex, address, result == null ? null : result.length });
                success = true;
            } catch (final Exception e) {
                logger.error("An exception occurred while reading a device property : {}", e.getMessage(), e);
            } finally {
                if (destination != null) {
                    destination.destroy();
                }
            }
        }

        return result;
    }

    private enum EventType {
        ADDED,
        REMOVED,
        UPDATED;
    }

    private void notifyListeners(Thing oldElement, Thing element, EventType eventType) {
        for (ProviderChangeListener<Thing> listener : this.providerListeners) {
            try {
                switch (eventType) {
                    case ADDED:
                        listener.added(this, element);
                        break;
                    case REMOVED:
                        listener.removed(this, element);
                        break;
                    case UPDATED:
                        listener.updated(this, oldElement, element);
                        break;
                    default:
                        break;
                }
            } catch (Exception ex) {
                logger.error("Could not inform the listener '{}' about the '{}' event : '{}'", listener,
                        eventType.name(), ex.getMessage(), ex);
            }
        }
    }

    private void notifyListeners(Thing element, EventType eventType) {
        notifyListeners(null, element, eventType);
    }

    protected void notifyListenersAboutAddedElement(Thing element) {
        notifyListeners(element, EventType.ADDED);
    }

    protected void notifyListenersAboutRemovedElement(Thing element) {
        notifyListeners(element, EventType.REMOVED);
    }

    protected void notifyListenersAboutUpdatedElement(Thing oldElement, Thing element) {
        notifyListeners(oldElement, element, EventType.UPDATED);
    }

    @Override
    public Collection<Thing> getAll() {
        ArrayList<Thing> allThings = new ArrayList<Thing>();
        for (File aFile : providedThings.keySet()) {
            allThings.addAll(providedThings.get(aFile).values());
        }

        return allThings;
    }

    @Override
    public Iterable<File> getAllProjects() {
        return knxProjects;
    }

    @Override
    public void removeProject(File projectToRemove) {
        HashMap<ThingUID, Thing> projectThings = providedThings.remove(projectToRemove);

        for (ThingUID thingUID : projectThings.keySet()) {
            logger.trace("Removing thing '{}' from knxproject file '{}'.", thingUID, projectToRemove.getName());
            notifyListenersAboutRemovedElement(projectThings.get(thingUID));
        }
    }

    @Override
    public void addOrRefreshProject(File file) {

        FileInputStream openInputStream = null;
        try {
            openInputStream = FileUtils.openInputStream(file);

            String knxProj = (String) getConfig().get(KNX_PROJ);

            if (knxProj != null && knxProj.equals(file.getName())) {
                if (knxProjects.contains(file)) {
                    logger.trace("Ah... removing the project file {}", file);
                    removeProject(file);
                }

                if (knxScheduler == null) {
                    knxScheduler = KNXThreadPoolFactory.getPrioritizedScheduledPool(getThing().getUID().getBindingId(),
                            5);
                }

                knxScheduler.schedule(new ZipRunnable(file, openInputStream), 0, TimeUnit.SECONDS);
            }

        } catch (IOException e) {
            logger.error("An exception has occurred while opening the file {} : {}", file.getName(), e.getMessage(), e);
        }
    }

    private class ZipRunnable implements Runnable {

        private File file;
        private FileInputStream openInputStream;

        public ZipRunnable(File file, FileInputStream openInputStream) {
            this.file = file;
            this.openInputStream = openInputStream;
        }

        @Override
        public void run() {

            logger.trace("Unzipping the KNX Project file");

            byte[] buffer = new byte[1024];
            HashMap<Path, String> xmlFiles = new HashMap<Path, String>();

            try {
                Path tempDir = Files.createTempDirectory(file.getName());
                tempDir.toFile().deleteOnExit();

                ZipInputStream zis = new ZipInputStream(openInputStream);
                ZipEntry ze = zis.getNextEntry();

                while (ze != null) {

                    String fileName = ze.getName().substring(ze.getName().lastIndexOf(File.separator) + 1,
                            ze.getName().length());

                    if (!fileName.equals("Catalog.xml") && fileName.contains(".xml")) {

                        Path tempPath = Files.createTempFile(tempDir, fileName, ".tmp");
                        tempPath.toFile().deleteOnExit();
                        xmlFiles.put(tempPath, fileName);

                        FileOutputStream fos = new FileOutputStream(tempPath.toString());
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                        fos.close();
                    }
                    ze = zis.getNextEntry();
                }

                zis.closeEntry();
                zis.close();

                logger.trace("Processing the XML Repository");

                for (Path anXml : xmlFiles.keySet()) {
                    if (xmlFiles.get(anXml).equals("knx_master.xml")) {
                        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                        factory.setNamespaceAware(true);
                        DocumentBuilder builder = factory.newDocumentBuilder();
                        Document doc = builder.parse(new ByteArrayInputStream(Files.readAllBytes(anXml)));
                        Element root = doc.getDocumentElement();

                        switch (root.getNamespaceURI()) {
                            case KNX_PROJECT_12: {
                                // TODO : ETS4
                                logger.warn("ETS4 project files are not supported");
                                break;
                            }
                            case KNX_PROJECT_13:
                                // ETS5
                                knxParser = new KNXProject13Parser();
                                break;
                        }

                    }
                }

                if (knxParser != null) {
                    logger.trace("Feeding the XML Repository to the KNX Project Parser");

                    knxProjects.add(file);
                    for (Path anXml : xmlFiles.keySet()) {
                        if (xmlFiles.get(anXml).equals("knx_master.xml")) {
                            knxParser.addXML("knx_master.xml", new String(Files.readAllBytes(anXml)));
                            break;
                        }
                    }

                    for (Path anXml : xmlFiles.keySet()) {
                        if (xmlFiles.get(anXml).equals("project.xml")) {
                            knxParser.addXML("project.xml", new String(Files.readAllBytes(anXml)));
                            break;
                        }
                    }

                    for (Path anXml : xmlFiles.keySet()) {
                        if (xmlFiles.get(anXml).equals("Hardware.xml")) {
                            knxParser.addXML("Hardware.xml", new String(Files.readAllBytes(anXml)));
                        }
                    }

                    for (Path anXml : xmlFiles.keySet()) {
                        if (!xmlFiles.get(anXml).equals("knx_master.xml") && !xmlFiles.get(anXml).equals("project.xml")
                                && !xmlFiles.get(anXml).equals("Hardware.xml")
                                && !xmlFiles.get(anXml).equals("Baggages.xml")) {
                            knxParser.addXML(xmlFiles.get(anXml), new String(Files.readAllBytes(anXml)));
                        }
                    }

                    knxParser.postProcess();

                    if (factory != null) {
                        Set<String> devices = knxParser.getIndividualAddresses();
                        HashMap<ThingUID, Thing> newThings = new HashMap<ThingUID, Thing>();
                        for (String device : devices) {

                            Configuration configuration = knxParser.getDeviceConfiguration(device);
                            Thing theThing = factory.createThing(THING_TYPE_GENERIC, configuration, null,
                                    getThing().getUID());

                            Map<String, String> properties = knxParser.getDeviceProperties(device);
                            theThing.setProperties(properties);

                            logger.info("Added a Thing {} for an actor of type {} made by {}", theThing.getUID(),
                                    properties.get(MANUFACTURER_HARDWARE_TYPE), properties.get(MANUFACTURER_NAME));

                            List<Channel> channelsToAdd = new ArrayList<Channel>();

                            Set<String> groupAddresses = knxParser.getGroupAddresses(device);
                            for (String groupAddress : groupAddresses) {
                                String dpt = knxParser.getDPT(groupAddress);

                                if (dpt != null) {
                                    Class<? extends Type> type = toTypeClass(dpt);
                                    Class<? extends GenericItem> itemType = typeItemMap.get(type);
                                    String id = groupAddress.replace("/", "_");

                                    if (itemType != null) {
                                        Configuration channelConfiguration = knxParser
                                                .getGroupAddressConfiguration(groupAddress, device);
                                        channelConfiguration.put(GROUPADDRESS, groupAddress);
                                        channelConfiguration.put(DPT, dpt);
                                        configuration.put(INTERVAL, new BigDecimal(3600));

                                        ChannelBuilder channelBuilder = ChannelBuilder
                                                .create(new ChannelUID(theThing.getUID(), id),
                                                        StringUtils.substringBefore(itemType.getSimpleName(), "Item"))
                                                .withKind(ChannelKind.STATE).withConfiguration(channelConfiguration)
                                                .withType(new ChannelTypeUID(getThing().getUID().getBindingId(),
                                                        CHANNEL_GENERIC))
                                                .withDescription((String) channelConfiguration.get(DESCRIPTION));

                                        Channel theChannel = channelBuilder.build();
                                        channelsToAdd.add(theChannel);

                                        logger.info("Added the Channel {} (Type {}, DPT {}, {}/{}/{}/{}, '{}')",
                                                theChannel.getUID(), theChannel.getAcceptedItemType(),
                                                theChannel.getConfiguration().get(DPT),
                                                theChannel.getConfiguration().get(READ),
                                                theChannel.getConfiguration().get(WRITE),
                                                theChannel.getConfiguration().get(TRANSMIT),
                                                theChannel.getConfiguration().get(UPDATE), theChannel.getDescription());
                                    } else {
                                        logger.warn(
                                                "The Type for the Channel with ID '{}' and DPT '{}' can not be resolved",
                                                new ChannelUID(theThing.getUID(), id), dpt);
                                    }
                                } else {
                                    logger.warn("The DPT for the Group Address '{}' can not be resolved", groupAddress);
                                }
                            }

                            ThingHelper.addChannelsToThing(theThing, channelsToAdd);

                            newThings.put(theThing.getUID(), theThing);
                        }

                        HashMap<ThingUID, Thing> oldThings = providedThings.get(file);
                        if (oldThings != null) {
                            for (ThingUID thingUID : newThings.keySet()) {
                                if (oldThings.keySet().contains(thingUID)) {
                                    oldThings.put(thingUID, newThings.get(thingUID));
                                    logger.debug("Updating Thing '{}' from knxproject file '{}'.", thingUID,
                                            file.getName());
                                    notifyListenersAboutUpdatedElement(oldThings.get(thingUID),
                                            newThings.get(thingUID));
                                } else {
                                    oldThings.put(thingUID, newThings.get(thingUID));
                                    logger.debug("Adding Thing '{}' from knxproject file '{}'.", thingUID,
                                            file.getName());
                                    notifyListenersAboutAddedElement(newThings.get(thingUID));
                                }
                            }

                            for (ThingUID thingUID : oldThings.keySet()) {
                                if (!newThings.keySet().contains(thingUID)) {
                                    oldThings.remove(thingUID);
                                    logger.debug("Removing Thing '{}' from knxproject file '{}'.", thingUID,
                                            file.getName());
                                    notifyListenersAboutRemovedElement(oldThings.get(thingUID));
                                }
                            }

                            providedThings.put(file, oldThings);
                        } else {
                            providedThings.put(file, newThings);
                            for (ThingUID thingUID : newThings.keySet()) {
                                logger.debug("Adding Thing '{}' from knxproject file '{}'.", thingUID, file.getName());
                                notifyListenersAboutAddedElement(newThings.get(thingUID));
                            }
                        }
                    }

                    HashMap<ThingUID, Thing> oldThings = providedThings.get(file);
                    knxScheduler = KNXThreadPoolFactory.getPrioritizedScheduledPool(getThing().getUID().getBindingId(),
                            oldThings.keySet().size() / 10);

                } else {
                    logger.warn("The KNX Project does not contain any master data");
                }

                knxParser = null;

            } catch (Exception e) {
                logger.error("An exception occurred while parsing the KNX Project file : '{}' : {}", file.getName(),
                        e.getMessage(), e);
            }
        }

    };

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

    LinkedBlockingDeque<LoggedEvent> loggedEvents = new LinkedBlockingDeque<LoggedEvent>(100);

    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS");

    public void logEvent(ChannelUID channelUID, Type type) {
        logEvent(EventSource.EMPTY, channelUID, type);
    }

    public void logEvent(EventSource source, ChannelUID channelUID, Type type) {
        LoggedEvent newEvent = new LoggedEvent();
        newEvent.timeStamp = System.currentTimeMillis();
        newEvent.source = source;
        newEvent.channel = channelUID;
        newEvent.type = type;
        synchronized (loggedEvents) {
            if (loggedEvents.remainingCapacity() == 0) {
                loggedEvents.removeLast();
            }
            loggedEvents.addFirst(newEvent);
        }

    }

    public boolean hasEvent(ChannelUID channelUID, Type command, EventSource source, long to) {
        return hasEvent(channelUID, command, source, 0, to);
    }

    public boolean hasEvent(ChannelUID channelUID, Type command, long to) {
        return hasEvent(channelUID, command, EventSource.EMPTY, 0, to);
    }

    public boolean hasEvent(ChannelUID channelUID, Type command, long from, long to) {
        return hasEvent(channelUID, command, EventSource.EMPTY, from, to);
    }

    public boolean hasEvent(ChannelUID channelUID, Type command, EventSource source, long from, long to) {
        synchronized (loggedEvents) {

            Iterator<LoggedEvent> iterator = loggedEvents.iterator();
            while (iterator.hasNext()) {
                LoggedEvent event = iterator.next();
                long now = System.currentTimeMillis();
                if ((now - event.timeStamp >= from) && (now - event.timeStamp <= to)) {
                    if (event.channel == channelUID) {
                        return true;
                    }
                }
                if (now - event.timeStamp > to) {
                    break;
                }
            }
        }
        return false;
    }

    public ScheduledExecutorService getScheduler() {
        return knxScheduler;
    }
}
