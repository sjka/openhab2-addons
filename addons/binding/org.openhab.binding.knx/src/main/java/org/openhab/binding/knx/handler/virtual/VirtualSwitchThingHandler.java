/**
 *
 */
package org.openhab.binding.knx.handler.virtual;

import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.link.ItemChannelLinkRegistry;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.State;
import org.openhab.binding.knx.handler.KNXBridgeBaseThingHandler;
import org.openhab.binding.knx.handler.VirtualActorThingHandler;

import tuwien.auto.calimero.GroupAddress;
import tuwien.auto.calimero.IndividualAddress;

/**
 * The {@link VirtualSwitchThingHandler} is responsible for handling commands, which are
 * sent to one of the channels. It implements a KNX switch actor, and fully interacts on the KNX bus
 *
 * @author Karel Goderis - Initial contribution
 */
public class VirtualSwitchThingHandler extends VirtualActorThingHandler {

    // List of all Channel ids
    public final static String CHANNEL_SWITCH = "switch";

    // List of all Configuration parameters
    public static final String SWITCH_GA = "switchGA";
    public static final String STATUS_GA = "statusGA";
    public static final String ADDRESS = "address";

    private State state = null;

    public VirtualSwitchThingHandler(Thing thing, ItemChannelLinkRegistry itemChannelLinkRegistry) {
        super(thing, itemChannelLinkRegistry);
    }

    @Override
    public void initialize() {
        super.initialize();

        try {
            if ((String) getConfig().get(ADDRESS) != null) {
                address = new IndividualAddress((String) getConfig().get(ADDRESS));
                updateStatus(ThingStatus.ONLINE);
            } else {
                updateStatus(ThingStatus.OFFLINE);
            }
        } catch (Exception e) {
            logger.error("An exception occurred while setting the Individual Address : '{}'", e.getMessage());
        }

        try {
            if ((String) getConfig().get(SWITCH_GA) != null) {
                GroupAddress address = new GroupAddress((String) getConfig().get(SWITCH_GA));
                if (address != null) {
                    groupAddresses.add(address);
                }
            }
        } catch (Exception e) {
            logger.error("An exception occurred while creating a Group Address : '{}'", e.getMessage());
        }
    }

    @Override
    public void onGroupWrite(KNXBridgeBaseThingHandler bridge, IndividualAddress source, GroupAddress destination,
            byte[] asdu) {

        try {
            if ((String) getConfig().get(SWITCH_GA) != null) {
                GroupAddress address = new GroupAddress((String) getConfig().get(SWITCH_GA));
                if (address.equals(destination)) {
                    updateState(new ChannelUID(getThing().getUID(), CHANNEL_SWITCH), state);
                }
            }
        } catch (Exception e) {
            // do nothing, we move on (either config parameter null, or wrong address format)
        }
    }

    @Override
    public void onGroupRead(KNXBridgeBaseThingHandler bridge, IndividualAddress source, GroupAddress destination,
            byte[] asdu) {

        try {
            if ((String) getConfig().get(STATUS_GA) != null && state != null) {
                if (address.equals(destination)) {
                    bridgeHandler.writeToKNX((String) getConfig().get(STATUS_GA), "1.001", state);
                }
            }
        } catch (Exception e) {
            logger.error("An exception occurred while creating a Group Address : '{}'", e.getMessage());
        }
    }

    @Override
    public void onGroupReadResponse(KNXBridgeBaseThingHandler bridge, IndividualAddress source,
            GroupAddress destination, byte[] asdu) {
        // Nothing to do here
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {

        if (bridgeHandler == null) {
            logger.warn("KNX bridge handler not found. Cannot handle command without bridge.");
            return;
        }

        state = (State) command;

        try {
            if ((String) getConfig().get(STATUS_GA) != null) {
                bridgeHandler.writeToKNX((String) getConfig().get(STATUS_GA), "1.001", state);
            }
        } catch (Exception e) {
            logger.error("An exception occurred while creating a Group Address : '{}'", e.getMessage());
        }
    }
}
