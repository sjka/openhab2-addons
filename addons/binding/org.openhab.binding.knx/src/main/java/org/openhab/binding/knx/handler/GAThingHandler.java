/**
 *
 */
package org.openhab.binding.knx.handler;

import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.link.ItemChannelLinkRegistry;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.core.types.Type;

import tuwien.auto.calimero.GroupAddress;

/**
 * The {@link GAThingHandler} is responsible for handling commands, which are
 * sent to one of the channels. It is a stub for Group Addresses that are
 * discovered on the KNX bus, and converts any data that is sent to the
 * Group Address into a StringType
 *
 * @author Karel Goderis - Initial contribution
 */
public class GAThingHandler extends KNXBaseThingHandler {

    // List of all Channel ids
    public final static String CHANNEL_STRING = "string";

    // List of all Configuration parameters
    public static final String ADDRESS = "address";
    public static final String DPT = "dpt";

    public GAThingHandler(Thing thing, ItemChannelLinkRegistry itemChannelLinkRegistry) {
        super(thing, itemChannelLinkRegistry);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openhab.binding.knx.handler.GAStatusListener#listensTo(tuwien.auto.calimero.GroupAddress)
     */
    @Override
    public boolean listensTo(GroupAddress destination) {

        try {
            GroupAddress address = new GroupAddress((String) getConfig().get(ADDRESS));

            if (address.equals(destination)) {
                return true;
            }
        } catch (Exception e) {
            // do nothing, we move on (either config parameter null, or wrong address format)
        }

        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openhab.binding.knx.handler.KNXBaseThingHandler#processDataReceived(tuwien.auto.calimero.GroupAddress,
     * org.eclipse.smarthome.core.types.Type)
     */
    @Override
    void processDataReceived(GroupAddress destination, Type state) {
        State newState = new StringType(state.toString());
        updateStateAndIgnore(new ChannelUID(getThing().getUID(), CHANNEL_STRING), newState);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openhab.binding.knx.handler.KNXBaseThingHandler#getDPT(tuwien.auto.calimero.GroupAddress)
     */
    @Override
    String getDPT(GroupAddress destination) {
        return (String) getConfig().get(DPT);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openhab.binding.knx.handler.KNXBaseThingHandler#intializeDatapoints()
     */
    @Override
    void initializeReadAddresses() {
        // nothing to do here
    }

    @Override
    String getDPT(ChannelUID channelUID, Type command) {
        return (String) getConfig().get(DPT);
    }

    @Override
    String getAddress(ChannelUID channelUID, Type command) {
        return (String) getConfig().get(ADDRESS);
    }

    @Override
    Type getType(ChannelUID channelUID, Type command) {
        // TODO Auto-generated method stub
        return command;
    }
}
