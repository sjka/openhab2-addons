package org.openhab.binding.knx.handler;

import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.link.ItemChannelLinkRegistry;
import org.eclipse.smarthome.core.types.Type;

import tuwien.auto.calimero.GroupAddress;
import tuwien.auto.calimero.IndividualAddress;

abstract class VirtualActorThingHandler extends KNXBaseThingHandler {

    public VirtualActorThingHandler(Thing thing, ItemChannelLinkRegistry itemChannelLinkRegistry) {
        super(thing, itemChannelLinkRegistry);
        // TODO Auto-generated constructor stub
    }

    @Override
    public void onGroupWrite(KNXBridgeBaseThingHandler bridge, IndividualAddress source, GroupAddress destination,
            byte[] asdu) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onGroupRead(KNXBridgeBaseThingHandler bridge, IndividualAddress source, GroupAddress destination,
            byte[] asdu) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onGroupReadResponse(KNXBridgeBaseThingHandler bridge, IndividualAddress source,
            GroupAddress destination, byte[] asdu) {
        // TODO Auto-generated method stub

    }

    @Override
    public String getDPT(GroupAddress destination) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getDPT(ChannelUID channelUID, Type command) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getAddress(ChannelUID channelUID, Type command) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Type getType(ChannelUID channelUID, Type command) {
        // TODO Auto-generated method stub
        return null;
    }
}
