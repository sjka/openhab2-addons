/**
 * 
 */
package org.openhab.binding.knx.handler;

import org.eclipse.smarthome.core.library.types.IncreaseDecreaseType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.link.ItemChannelLinkRegistry;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.core.types.Type;

import tuwien.auto.calimero.GroupAddress;

/**
 * The {@link DimmerThingHandler} is responsible for handling commands, which are
 * sent to one of the channels. It implements a KNX dimmer actor
 * 
 * @author Karel Goderis - Initial contribution
 */
public class DimmerThingHandler extends KNXBaseThingHandler {

	// List of all Channel ids
	public final static String CHANNEL_DIMMER = "dimmer"; 

	// List of all Configuration parameters
	public static final String SWITCH_GA = "switchGA";
	public static final String STATUS_GA = "statusGA";
	public final static String INCREASE_DECREASE_GA = "increasedecreaseGA"; 
	public final static String POSITION_GA = "positionGA"; 
	public static final String DIM_VALUE_GA = "dimvalueGA";

	public DimmerThingHandler(Thing thing,
			ItemChannelLinkRegistry itemChannelLinkRegistry) {
		super(thing, itemChannelLinkRegistry);
	}

	/* (non-Javadoc)
	 * @see org.openhab.binding.knx.handler.GAStatusListener#listensTo(tuwien.auto.calimero.GroupAddress)
	 */
	@Override
	public boolean listensTo(GroupAddress destination) {

		try {
			GroupAddress address = new GroupAddress((String)getConfig().get(SWITCH_GA));

			if(address.equals(destination)) { 
				return true;
			}
		} catch (Exception e) {
			// do nothing, we move on (either config parameter null, or wrong address format)
		}		

		try {
			GroupAddress address = new GroupAddress((String)getConfig().get(STATUS_GA));

			if(address.equals(destination)) { 
				return true;
			}
		} catch (Exception e) {
			// do nothing, we move on (either config parameter null, or wrong address format)
		}

		try {
			GroupAddress address = new GroupAddress((String)getConfig().get(DIM_VALUE_GA));

			if(address.equals(destination)) { 
				return true;
			}
		} catch (Exception e) {
			// do nothing, we move on (either config parameter null, or wrong address format)
		}

		try {
			GroupAddress address = new GroupAddress((String)getConfig().get(INCREASE_DECREASE_GA));

			if(address.equals(destination)) { 
				return true;
			}
		} catch (Exception e) {
			// do nothing, we move on (either config parameter null, or wrong address format)
		}

		try {
			GroupAddress address = new GroupAddress((String)getConfig().get(POSITION_GA));

			if(address.equals(destination)) { 
				return true;
			}
		} catch (Exception e) {
			// do nothing, we move on (either config parameter null, or wrong address format)
		}

		return false;
	}

	/* (non-Javadoc)
	 * @see org.openhab.binding.knx.handler.KNXBaseThingHandler#processDataReceived(tuwien.auto.calimero.GroupAddress, org.eclipse.smarthome.core.types.Type)
	 */
	@Override
	void processDataReceived(GroupAddress destination, Type state) {
		
		try {
			GroupAddress address = new GroupAddress((String)getConfig().get(STATUS_GA));
			if(address.equals(destination)) {
				updateStateAndIgnore(new ChannelUID(getThing().getUID(), CHANNEL_DIMMER), (State) state);
			}
		} catch (Exception e) {
			// do nothing, we move on (either config parameter null, or wrong address format)
		}

		
		try {
			GroupAddress address = new GroupAddress((String)getConfig().get(DIM_VALUE_GA));
			if(address.equals(destination)) {
				updateStateAndIgnore(new ChannelUID(getThing().getUID(), CHANNEL_DIMMER), (State) state);
			}
		} catch (Exception e) {
			// do nothing, we move on (either config parameter null, or wrong address format)
		}

	}

	/* (non-Javadoc)
	 * @see org.openhab.binding.knx.handler.KNXBaseThingHandler#getDPT(tuwien.auto.calimero.GroupAddress)
	 */
	@Override
	String getDPT(GroupAddress destination) {

		try {
			GroupAddress address = new GroupAddress((String)getConfig().get(SWITCH_GA));

			if(address.equals(destination)) { 
				return "1.001";
			}
		} catch (Exception e) {
			// do nothing, we move on (either config parameter null, or wrong address format)
		}		

		try {
			GroupAddress address = new GroupAddress((String)getConfig().get(STATUS_GA));

			if(address.equals(destination)) { 
				return "1.001";
			}
		} catch (Exception e) {
			// do nothing, we move on (either config parameter null, or wrong address format)
		}

		try {
			GroupAddress address = new GroupAddress((String)getConfig().get(DIM_VALUE_GA));

			if(address.equals(destination)) { 
				return "5.001";
			}
		} catch (Exception e) {
			// do nothing, we move on (either config parameter null, or wrong address format)
		}

		try {
			GroupAddress address = new GroupAddress((String)getConfig().get(INCREASE_DECREASE_GA));

			if(address.equals(destination)) { 
				return "3.007";
			}
		} catch (Exception e) {
			// do nothing, we move on (either config parameter null, or wrong address format)
		}

		try {
			GroupAddress address = new GroupAddress((String)getConfig().get(POSITION_GA));

			if(address.equals(destination)) { 
				return "5.001";
			}
		} catch (Exception e) {
			// do nothing, we move on (either config parameter null, or wrong address format)
		}

		return null;
	}

	/* (non-Javadoc)
	 * @see org.openhab.binding.knx.handler.KNXBaseThingHandler#intializeDatapoints()
	 */
	@Override
	void initializeReadAddresses() {
		if((Boolean)getConfig().get(READ)) {

			if((String)getConfig().get(STATUS_GA) != null) {
				addresses.add((String)(String)getConfig().get(STATUS_GA));
			}

			if((String)getConfig().get(DIM_VALUE_GA) != null) {
				addresses.add((String)(String)getConfig().get(DIM_VALUE_GA));
			}
		}
	}

	@Override
	String getDPT(ChannelUID channelUID, Type command) {
		return ((KNXBridgeBaseThingHandler)getBridge().getHandler()).toDPTid(command.getClass());
	}

	@Override
	String getAddress(ChannelUID channelUID, Type command) {

		if(command instanceof PercentType) {
			return (String)getConfig().get(POSITION_GA);
		}

		if(command instanceof OnOffType) {
			return (String)getConfig().get(SWITCH_GA);
		}

		if(command instanceof IncreaseDecreaseType) {
			return (String)getConfig().get(INCREASE_DECREASE_GA);
		}

		return null;		
	}
	
    @Override
    Type getType(ChannelUID channelUID, Type command) {
        // TODO Auto-generated method stub
        return command;
    }
}
