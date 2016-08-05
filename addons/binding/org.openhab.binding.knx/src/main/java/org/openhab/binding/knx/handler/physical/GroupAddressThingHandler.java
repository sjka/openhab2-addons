/**
 *
 */
package org.openhab.binding.knx.handler.physical;

import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.link.ItemChannelLinkRegistry;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.core.types.Type;
import org.openhab.binding.knx.handler.PhysicalActorThingHandler;

import tuwien.auto.calimero.GroupAddress;
import tuwien.auto.calimero.IndividualAddress;

/**
 * The {@link GroupAddressThingHandler} is responsible for handling commands, which are
 * sent to one of the channels. It is a stub for Group Addresses that are
 * discovered on the KNX bus, and converts any data that is sent to the
 * Group Address into a StringType
 *
 * @author Karel Goderis - Initial contribution
 */
public class GroupAddressThingHandler extends PhysicalActorThingHandler {

    // List of all Channel ids
    public final static String CHANNEL_STRING = "string";

    // List of all Configuration parameters
    public static final String ADDRESS = "address";
    public static final String AUTO_UPDATE = "autoupdate";
    public static final String DPT = "dpt";

    boolean autoUpdated = false;

    public GroupAddressThingHandler(Thing thing, ItemChannelLinkRegistry itemChannelLinkRegistry) {
        super(thing, itemChannelLinkRegistry);
    }

    @Override
    public void initialize() {

        try {
            GroupAddress groupaddress = new GroupAddress((String) getConfig().get(ADDRESS));

            if (address != null) {
                groupAddresses.add(groupaddress);
                readAddresses.add(groupaddress);
            }
        } catch (Exception e) {
            logger.error("An exception occurred while creating a Group Address : '{}'", e.getMessage());
        }

        super.initialize();
    }

    @Override
    public boolean listensTo(IndividualAddress source) {
        // we listen to telegrams coming from any source
        return true;
    }

    @Override
    public void handleUpdate(ChannelUID channelUID, State newState) {

        if (autoUpdated) {
            autoUpdated = false;
        } else {
            super.handleUpdate(channelUID, newState);
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {

        super.handleCommand(channelUID, command);

        if (getConfig().get(AUTO_UPDATE) != null && (boolean) getConfig().get(AUTO_UPDATE)) {
            autoUpdated = true;
            updateState(channelUID, (State) command);
        }
    }

    @Override
    public void processDataReceived(GroupAddress destination, Type state) {
        State newState = new StringType(state.toString());
        updateState(new ChannelUID(getThing().getUID(), CHANNEL_STRING), newState);
    }

    @Override
    public String getDPT(GroupAddress destination) {
        return (String) getConfig().get(DPT);
    }

    @Override
    public String getDPT(ChannelUID channelUID, Type command) {
        return (String) getConfig().get(DPT);
    }

    @Override
    public String getAddress(ChannelUID channelUID, Type command) {
        return (String) getConfig().get(ADDRESS);
    }

    @Override
    public Type getType(ChannelUID channelUID, Type command) {
        return command;
    }

}
