/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.knx.generic.internal.factory;

import static org.openhab.binding.knx.generic.KNXGenericBindingConstants.*;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang.RandomStringUtils;
import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.config.discovery.DiscoveryService;
import org.eclipse.smarthome.core.i18n.LocaleProvider;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingProvider;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.thing.link.ItemChannelLinkRegistry;
import org.eclipse.smarthome.core.thing.type.ThingType;
import org.eclipse.smarthome.core.thing.type.ThingTypeRegistry;
import org.openhab.binding.knx.KNXTypeMapper;
import org.openhab.binding.knx.generic.KNXProjectProvider;
import org.openhab.binding.knx.generic.discovery.IndividualAddressDiscoveryService;
import org.openhab.binding.knx.generic.internal.handler.KNXGenericThingHandler;
import org.openhab.binding.knx.generic.internal.handler.KNXImportHandler;
import org.openhab.binding.knx.generic.internal.handler.KNXScanHandler;
import org.openhab.binding.knx.generic.internal.provider.KNXProjectThingProvider;
import org.osgi.framework.ServiceRegistration;

import com.google.common.collect.Lists;

/**
 * The {@link KNXGenericHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author Karel Goderis - Initial contribution
 */
@SuppressWarnings("rawtypes")
public class KNXGenericHandlerFactory extends BaseThingHandlerFactory {

    private static final String[] PROVIDER_INTERFACES = new String[] { KNXProjectProvider.class.getName(),
            ThingProvider.class.getName() };

    public static final Collection<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Lists.newArrayList(THING_TYPE_GENERIC,
            THING_TYPE_PARSER, THING_TYPE_SCANNER);

    private final Map<ThingUID, ServiceRegistration> discoveryServiceRegs = new HashMap<>();
    private final Map<ThingUID, ServiceRegistration> knxProjectProviderServiceRegs = new HashMap<>();
    private final Collection<KNXTypeMapper> typeMappers = new HashSet<KNXTypeMapper>();
    private ItemChannelLinkRegistry itemChannelLinkRegistry;
    private ThingTypeRegistry thingTypeRegistry;
    private LocaleProvider localeProvider;

    protected void setItemChannelLinkRegistry(ItemChannelLinkRegistry registry) {
        itemChannelLinkRegistry = registry;
    }

    protected void unsetItemChannelLinkRegistry(ItemChannelLinkRegistry registry) {
        itemChannelLinkRegistry = null;
    }

    protected void setThingTypeRegistry(ThingTypeRegistry thingTypeRegistry) {
        this.thingTypeRegistry = thingTypeRegistry;
    }

    protected void unsetThingTypeRegistry(ThingTypeRegistry thingTypeRegistry) {
        this.thingTypeRegistry = null;
    }

    protected void setLocaleProvider(LocaleProvider localeProvider) {
        this.localeProvider = localeProvider;
    }

    protected void unsetLocaleProvider(LocaleProvider localeProvider) {
        this.localeProvider = null;
    }

    public void addKNXTypeMapper(KNXTypeMapper typeMapper) {
        typeMappers.add(typeMapper);
    }

    public void removeKNXTypeMapper(KNXTypeMapper typeMapper) {
        typeMappers.remove(typeMapper);
    }

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    public Thing createThing(ThingTypeUID thingTypeUID, Configuration configuration, ThingUID thingUID,
            ThingUID bridgeUID) {
        if (THING_TYPE_GENERIC.equals(thingTypeUID)) {
            ThingUID gaUID = getGenericThingUID(thingTypeUID, thingUID, configuration, bridgeUID);
            return super.createThing(thingTypeUID, configuration, gaUID, bridgeUID);
        } else if (THING_TYPE_PARSER.equals(thingTypeUID)) {
            return super.createThing(thingTypeUID, configuration, thingUID, bridgeUID);
        } else if (THING_TYPE_SCANNER.equals(thingTypeUID)) {
            return super.createThing(thingTypeUID, configuration, thingUID, bridgeUID);
        }
        throw new IllegalArgumentException("The thing type " + thingTypeUID + " is not supported by the KNX binding.");
    }

    @Override
    protected ThingHandler createHandler(Thing thing) {
        if (thing.getThingTypeUID().equals(THING_TYPE_GENERIC)) {
            return new KNXGenericThingHandler(thing, itemChannelLinkRegistry);
        } else if (thing.getThingTypeUID().equals(THING_TYPE_PARSER)) {
            return new KNXImportHandler((Bridge) thing);
        } else if (thing.getThingTypeUID().equals(THING_TYPE_SCANNER)) {
            return new KNXScanHandler((Bridge) thing);
        }
        return null;
    }

    private ThingUID getGenericThingUID(ThingTypeUID thingTypeUID, ThingUID thingUID, Configuration configuration,
            ThingUID bridgeUID) {
        if (thingUID != null) {
            return thingUID;
        }
        String address = ((String) configuration.get(ADDRESS));
        if (address != null) {
            return new ThingUID(thingTypeUID, address.replace(".", "_"), bridgeUID.getId());
        } else {
            String randomID = RandomStringUtils.randomAlphabetic(16).toLowerCase(Locale.ENGLISH);
            return new ThingUID(thingTypeUID, randomID, bridgeUID.getId());
        }
    }

    private synchronized void registerAddressDiscoveryService(KNXScanHandler handler) {
        IndividualAddressDiscoveryService service = new IndividualAddressDiscoveryService(handler);
        Hashtable<String, Object> properties = new Hashtable<String, Object>();
        ServiceRegistration reg = bundleContext.registerService(DiscoveryService.class.getName(), service, properties);
        this.discoveryServiceRegs.put(handler.getThing().getUID(), reg);
    }

    private synchronized void unregisterAddressDiscoveryService(Thing thing) {
        ServiceRegistration reg = discoveryServiceRegs.remove(thing.getUID());
        if (reg != null) {
            reg.unregister();
        }
    }

    private synchronized void registerProjectProviderService(KNXImportHandler bridgeHandler) {
        KNXProjectThingProvider provider = new KNXProjectThingProvider(bridgeHandler.getThing(), this);
        Hashtable<String, Object> properties = new Hashtable<String, Object>();
        ServiceRegistration reg = bundleContext.registerService(PROVIDER_INTERFACES, provider, properties);
        this.knxProjectProviderServiceRegs.put(bridgeHandler.getThing().getUID(), reg);
    }

    private synchronized void unregisterProjectProviderService(Thing thing) {
        ServiceRegistration reg = knxProjectProviderServiceRegs.remove(thing.getUID());
        if (reg != null) {
            reg.unregister();
        }
    }

    public ThingType getThingType(ThingTypeUID thingTypeUID) {
        return thingTypeRegistry.getThingType(thingTypeUID, localeProvider.getLocale());
    }

    @Override
    public ThingHandler registerHandler(Thing thing) {
        ThingHandler handler = super.registerHandler(thing);
        if (handler instanceof KNXScanHandler) {
            registerAddressDiscoveryService((KNXScanHandler) handler);
        }
        if (handler instanceof KNXImportHandler) {
            registerProjectProviderService((KNXImportHandler) handler);
        }
        return handler;
    }

    @Override
    public void unregisterHandler(Thing thing) {
        unregisterAddressDiscoveryService(thing);
        unregisterProjectProviderService(thing);
        super.unregisterHandler(thing);
    }

    public Collection<KNXTypeMapper> getTypeMappers() {
        return Collections.unmodifiableCollection(typeMappers);
    }

}
