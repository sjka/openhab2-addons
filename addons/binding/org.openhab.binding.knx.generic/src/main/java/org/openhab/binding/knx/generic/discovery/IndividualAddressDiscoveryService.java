/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.openhab.binding.knx.generic.discovery;

import static org.openhab.binding.knx.generic.KNXGenericBindingConstants.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.openhab.binding.knx.KNXBusListener;
import org.openhab.binding.knx.generic.KNXGenericBindingConstants;
import org.openhab.binding.knx.handler.KNXBridgeBaseThingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

import tuwien.auto.calimero.GroupAddress;
import tuwien.auto.calimero.IndividualAddress;

/**
 * The {@link IndividualAddressDiscoveryService} class provides a discovery
 * mechanism for KNX Individual Addresses
 *
 * @author Karel Goderis - Initial contribution
 */
public class IndividualAddressDiscoveryService extends AbstractDiscoveryService implements KNXBusListener {

    public static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Sets.newHashSet(THING_TYPE_GENERIC);

    private Logger logger = LoggerFactory.getLogger(KNXBridgeBaseThingHandler.class);

    private static final int SEARCH_TIME = 600;
    private boolean searchOngoing = false;

    private KNXBridgeBaseThingHandler bridgeHandler;
    protected Set<AreaLine> seenNetworks = new CopyOnWriteArraySet<>();

    public IndividualAddressDiscoveryService(KNXBridgeBaseThingHandler handler) throws IllegalArgumentException {
        super(SUPPORTED_THING_TYPES_UIDS, SEARCH_TIME, false);
        this.bridgeHandler = handler;
        bridgeHandler.registerKNXBusListener(this);
    }

    public class AreaLine {
        public int area;
        public int line;

        public AreaLine(int area, int line) {
            this.area = area;
            this.line = line;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + getOuterType().hashCode();
            result = prime * result + area;
            result = prime * result + line;
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
            AreaLine other = (AreaLine) obj;
            if (!getOuterType().equals(other.getOuterType())) {
                return false;
            }
            if (area != other.area) {
                return false;
            }
            if (line != other.line) {
                return false;
            }
            return true;
        }

        private IndividualAddressDiscoveryService getOuterType() {
            return IndividualAddressDiscoveryService.this;
        }
    }

    @Override
    public void activate(Map<String, Object> configProperties) {
        super.activate(configProperties);
    }

    @Override
    public void deactivate() {
        super.deactivate();
        bridgeHandler.unregisterKNXBusListener(this);
    }

    @Override
    protected void startScan() {
        searchOngoing = true;

        Set<AreaLine> scannedNetworks = new HashSet<>();

        for (AreaLine al : seenNetworks) {
            logger.debug("Scanning the already seen network {}.{} for KNX actors", al.area, al.line);
            scanNetwork(scannedNetworks, al);
        }

        for (int area = 0; area < 16; area++) {
            for (int line = 0; line < 16; line++) {
                logger.debug("Scanning {}.{} for KNX actors", area, line);
                scanNetwork(scannedNetworks, new AreaLine(area, line));
            }
        }
    }

    private void scanNetwork(Set<AreaLine> scannedNetworks, AreaLine al) {
        if (!searchOngoing) {
            return;
        }
        if (scannedNetworks.contains(al)) {
            return;
        }

        IndividualAddress[] addresses = bridgeHandler.scanNetworkDevices(al.area, al.line);
        processResults(addresses);
        scannedNetworks.add(al);

        if (bridgeHandler.getThing().getStatus() == ThingStatus.OFFLINE) {
            stopScan();
        }
    }

    private void processResults(IndividualAddress[] newAddresses) {
        if (newAddresses != null) {
            for (int i = 0; i < newAddresses.length; i++) {
                ThingUID bridgeUID = bridgeHandler.getThing().getUID();
                ThingUID thingUID = new ThingUID(KNXGenericBindingConstants.THING_TYPE_GENERIC,
                        newAddresses[i].toString().replace(".", "_"), bridgeUID.getId());

                Map<String, Object> properties = new HashMap<>(1);
                properties.put(ADDRESS, newAddresses[i].toString());
                DiscoveryResult discoveryResult = DiscoveryResultBuilder.create(thingUID).withProperties(properties)
                        .withBridge(bridgeUID).withLabel("Individual Address " + newAddresses[i].toString()).build();

                thingDiscovered(discoveryResult);
            }
        }
    }

    @Override
    protected void stopScan() {
        searchOngoing = false;
    }

    @Override
    public void onActivity(IndividualAddress source, GroupAddress destination, byte[] asdu) {
        if (source != null) {
            seenNetworks.add(new AreaLine(source.getArea(), source.getLine()));
        }
    }

}
