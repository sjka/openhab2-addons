/**
 * Copyright (c) 2014 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.knx.discovery;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.openhab.binding.knx.KNXBindingConstants;
import org.openhab.binding.knx.KNXBridgeListener;
import org.openhab.binding.knx.handler.KNXBridgeBaseThingHandler;
import org.openhab.binding.knx.handler.physical.GroupAddressThingHandler;

import com.google.common.collect.Sets;

import tuwien.auto.calimero.IndividualAddress;

/**
 * The {@link IndividualAddressDiscoveryService} class provides a discovery
 * mechanism for KNX Individual Addresses
 *
 * @author Karel Goderis - Initial contribution
 */
public class IndividualAddressDiscoveryService extends AbstractDiscoveryService implements KNXBridgeListener {

    public final static Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Sets
            .newHashSet(KNXBindingConstants.THING_TYPE_GENERIC);

    private final static int SEARCH_TIME = 600;
    private boolean searchOngoing = false;

    private KNXBridgeBaseThingHandler bridgeHandler;

    public IndividualAddressDiscoveryService(KNXBridgeBaseThingHandler bridgeHandler) throws IllegalArgumentException {
        super(SUPPORTED_THING_TYPES_UIDS, SEARCH_TIME, false);
        this.bridgeHandler = bridgeHandler;
    }

    @Override
    protected void startScan() {
        searchOngoing = true;
        for (int area = 0; area < 16; area++) {
            for (int line = 0; line < 16; line++) {
                if (searchOngoing) {
                    IndividualAddress[] addresses = bridgeHandler.scanNetworkDevices(area, line);

                    for (int i = 0; i < addresses.length; i++) {

                        ThingUID bridgeUID = bridgeHandler.getThing().getUID();
                        ThingUID thingUID = new ThingUID(KNXBindingConstants.THING_TYPE_GENERIC,
                                addresses[i].toString().replaceAll(".", "_"), bridgeUID.getId());

                        Map<String, Object> properties = new HashMap<>(1);
                        properties.put(GroupAddressThingHandler.ADDRESS, addresses[i].toString());
                        DiscoveryResult discoveryResult = DiscoveryResultBuilder.create(thingUID)
                                .withProperties(properties).withBridge(bridgeUID)
                                .withLabel("Individual Address " + addresses[i].toString()).build();

                        thingDiscovered(discoveryResult);
                    }
                }
            }
        }
    }

    @Override
    protected void stopScan() {
        searchOngoing = false;
    }

    public void activate() {
        bridgeHandler.registerKNXBridgeListener(this);
    }

    @Override
    public void deactivate() {
        bridgeHandler.unregisterKNXBridgeListener(this);
    }

    @Override
    public void onBridgeDisconnected(KNXBridgeBaseThingHandler bridge) {
        stopScan();
    }

    @Override
    public void onBridgeConnected(KNXBridgeBaseThingHandler bridge) {
        // When a bridge connects, it is up the user to trigger a search
    }

}
