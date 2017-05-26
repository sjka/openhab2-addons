package org.openhab.binding.knx.generic.internal.handler;

import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.knx.handler.KNXBridgeBaseThingHandler;

public class KNXScanHandler extends BaseBridgeHandler {

    public KNXScanHandler(Bridge bridge) {
        super(bridge);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
    }

    public KNXBridgeBaseThingHandler getMainBridgeHandler() {
        return (KNXBridgeBaseThingHandler) getBridge().getHandler();
    }

}
