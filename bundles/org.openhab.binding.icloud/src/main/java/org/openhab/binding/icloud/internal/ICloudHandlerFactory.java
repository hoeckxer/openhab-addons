/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.icloud.internal;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.discovery.DiscoveryService;
import org.eclipse.smarthome.core.i18n.LocaleProvider;
import org.eclipse.smarthome.core.i18n.TranslationProvider;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerFactory;
import org.eclipse.smarthome.io.net.http.ExtensibleTrustManager;
import org.eclipse.smarthome.io.net.http.TlsCertificateProvider;
import org.openhab.binding.icloud.internal.discovery.ICloudDeviceDiscovery;
import org.openhab.binding.icloud.internal.handler.ICloudAccountBridgeHandler;
import org.openhab.binding.icloud.internal.handler.ICloudDeviceHandler;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import static org.openhab.binding.icloud.internal.ICloudBindingConstants.SUPPORTED_THING_TYPES_UIDS;
import static org.openhab.binding.icloud.internal.ICloudBindingConstants.THING_TYPE_ICLOUD;
import static org.openhab.binding.icloud.internal.ICloudBindingConstants.THING_TYPE_ICLOUDDEVICE;

/**
 * The {@link ICloudHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * Note: Adding @NonNullByDefault to this class will not correctly take into account the binding of
 * the ExtensibleTrustManager and ICloudTlsCertificateProvider using the OSGi @Reference.
 *
 * @author Patrik Gfeller - Initial contribution
 */
@Component(service = ThingHandlerFactory.class, configurationPid = "binding.icloud")
public class ICloudHandlerFactory extends BaseThingHandlerFactory {
    private final Logger logger = LoggerFactory.getLogger(ICloudHandlerFactory.class);

    private final Map<ThingUID, @Nullable ServiceRegistration<?>> discoveryServiceRegistrations = new HashMap<>();
    private LocaleProvider localeProvider;
    private TranslationProvider i18nProvider;
    private ExtensibleTrustManager extensibleTrustManager;
    private ICloudTlsCertificateProvider iCloudTlsCertificateProvider;

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    protected @Nullable ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();
        logger.debug("Creating handler");

        if (thingTypeUID.equals(THING_TYPE_ICLOUD)) {
            logger.debug("Creating handler for type icloud");
            ICloudAccountBridgeHandler bridgeHandler = new ICloudAccountBridgeHandler((Bridge) thing, extensibleTrustManager, iCloudTlsCertificateProvider);
            registerDeviceDiscoveryService(bridgeHandler);
            return bridgeHandler;
        }

        if (thingTypeUID.equals(THING_TYPE_ICLOUDDEVICE)) {
            logger.debug("Creating handler for type device");
            return new ICloudDeviceHandler(thing);
        }
        return null;
    }

    @Override
    protected void removeHandler(ThingHandler thingHandler) {
        if (thingHandler instanceof ICloudAccountBridgeHandler) {
            unregisterDeviceDiscoveryService((ICloudAccountBridgeHandler) thingHandler);
        }
    }

    private synchronized void registerDeviceDiscoveryService(ICloudAccountBridgeHandler bridgeHandler) {
        ICloudDeviceDiscovery discoveryService = new ICloudDeviceDiscovery(bridgeHandler, bundleContext.getBundle(),
                i18nProvider, localeProvider);
        discoveryService.activate();
        this.discoveryServiceRegistrations.put(bridgeHandler.getThing().getUID(), bundleContext
                .registerService(DiscoveryService.class.getName(), discoveryService, new Hashtable<String, Object>()));
    }

    private synchronized void unregisterDeviceDiscoveryService(ICloudAccountBridgeHandler bridgeHandler) {
        ServiceRegistration<?> serviceRegistration = this.discoveryServiceRegistrations
                .get(bridgeHandler.getThing().getUID());
        if (serviceRegistration != null) {
            ICloudDeviceDiscovery discoveryService = (ICloudDeviceDiscovery) bundleContext
                    .getService(serviceRegistration.getReference());
            if (discoveryService != null) {
                discoveryService.deactivate();
            }
            serviceRegistration.unregister();
            discoveryServiceRegistrations.remove(bridgeHandler.getThing().getUID());
        }
    }

    @Reference
    protected void setLocaleProvider(LocaleProvider localeProvider) {
        this.localeProvider = localeProvider;
    }

    protected void unsetLocaleProvider(LocaleProvider localeProvider) {
        this.localeProvider = null;
    }

    @Reference
    public void setTranslationProvider(TranslationProvider i18nProvider) {
        this.i18nProvider = i18nProvider;
    }

    public void unsetTranslationProvider(TranslationProvider i18nProvider) {
        this.i18nProvider = null;
    }

    @Reference
    public void setiCloudTlsCertificateProvider(TlsCertificateProvider iCloudTlsCertificateProvider) {
        this.iCloudTlsCertificateProvider = (ICloudTlsCertificateProvider)iCloudTlsCertificateProvider;
    }

    public void unseticloudTlsCertificateProvider(TlsCertificateProvider iCloudTlsCertificateProvider) {
        this.iCloudTlsCertificateProvider = null;
    }

    @Reference
    public void setExtensibleTrustManager(ExtensibleTrustManager extensibleTrustManager) {
        this.extensibleTrustManager = extensibleTrustManager;
    }

    public void unsetExtensibleTrustManager(ExtensibleTrustManager extensibleTrustManager) {
        this.extensibleTrustManager = null;
    }

}
