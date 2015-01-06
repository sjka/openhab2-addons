/**
 * 
 */
package org.openhab.binding.knx.handler;

import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.link.ItemChannelLinkRegistry;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.core.types.Type;
import tuwien.auto.calimero.GroupAddress;

/**
 * The {@link DimmerThingHandler} is responsible for handling commands, which are
 * sent to one of the channels. It implements a KNX switch actor that is capable
 * of reporting energy consumption
 * 
 * @author Karel Goderis - Initial contribution
 */
public class EnergySwitchThingHandler extends SwitchThingHandler {

	// List of all Channel ids
	public final static String CHANNEL_SWITCH = "switch"; 
	public final static String CHANNEL_OPERATING_HOURS = "hours"; 
	public final static String CHANNEL_CURRENT = "current"; 
	public final static String CHANNEL_ENERGY = "energy"; 
	
	// List of all Configuration parameters
	public static final String OPERATING_HOURS_GA = "operatingGA";
	public static final String CURRENT_GA = "currentGA";
	public static final String ENERGY_GA = "energyGA";

	public EnergySwitchThingHandler(Thing thing,
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
			GroupAddress address = new GroupAddress((String)getConfig().get(OPERATING_HOURS_GA));

			if(address.equals(destination)) { 
					return true;
			}
		} catch (Exception e) {
			// do nothing, we move on (either config parameter null, or wrong address format)
		}
		
		try {
			GroupAddress address = new GroupAddress((String)getConfig().get(CURRENT_GA));

			if(address.equals(destination)) { 
					return true;
			}
		} catch (Exception e) {
			// do nothing, we move on (either config parameter null, or wrong address format)
		}
		
		try {
			GroupAddress address = new GroupAddress((String)getConfig().get(ENERGY_GA));

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
				updateStateAndIgnore(new ChannelUID(getThing().getUID(), CHANNEL_SWITCH), (State) state);
			}
		} catch (Exception e) {
			// do nothing, we move on (either config parameter null, or wrong address format)
		}
		
		try {
			GroupAddress address = new GroupAddress((String)getConfig().get(OPERATING_HOURS_GA));

			if(address.equals(destination)) { 
				updateStateAndIgnore(new ChannelUID(getThing().getUID(), CHANNEL_OPERATING_HOURS), (State) state);
			}
		} catch (Exception e) {
			// do nothing, we move on (either config parameter null, or wrong address format)
		}
		
		try {
			GroupAddress address = new GroupAddress((String)getConfig().get(CURRENT_GA));

			if(address.equals(destination)) { 
				updateStateAndIgnore(new ChannelUID(getThing().getUID(), CHANNEL_CURRENT), (State) state);
			}
		} catch (Exception e) {
			// do nothing, we move on (either config parameter null, or wrong address format)
		}
		
		try {
			GroupAddress address = new GroupAddress((String)getConfig().get(ENERGY_GA));

			if(address.equals(destination)) { 
				updateStateAndIgnore(new ChannelUID(getThing().getUID(), CHANNEL_ENERGY), (State) state);
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
			GroupAddress address = new GroupAddress((String)getConfig().get(OPERATING_HOURS_GA));

			if(address.equals(destination)) { 
					return "7.001";
			}
		} catch (Exception e) {
			// do nothing, we move on (either config parameter null, or wrong address format)
		}
		
		try {
			GroupAddress address = new GroupAddress((String)getConfig().get(CURRENT_GA));

			if(address.equals(destination)) { 
					return "7.012";
			}
		} catch (Exception e) {
			// do nothing, we move on (either config parameter null, or wrong address format)
		}
		
		try {
			GroupAddress address = new GroupAddress((String)getConfig().get(ENERGY_GA));

			if(address.equals(destination)) { 
					return "13.001";
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

			if((String)getConfig().get(STATUS_GA) !=null) {
				addresses.add((String)(String)getConfig().get(STATUS_GA));
			}
			if((String)getConfig().get(CURRENT_GA) != null) {
				addresses.add((String)(String)getConfig().get(CURRENT_GA));
			}
			if((String)getConfig().get(OPERATING_HOURS_GA) != null) {
				addresses.add((String)(String)getConfig().get(OPERATING_HOURS_GA));
			}
			if((String)getConfig().get(ENERGY_GA) != null) {
				addresses.add((String)(String)getConfig().get(ENERGY_GA));
			}
		}
	}

	@Override
	String getDPT(ChannelUID channelUID, Type command) {
		return ((KNXBridgeBaseThingHandler)getBridge().getHandler()).toDPTid(command.getClass());
	}

	@Override
	String getAddress(ChannelUID channelUID, Type command) {
		if(command instanceof OnOffType) {
			return (String)getConfig().get(SWITCH_GA);
		}

		return null;		
	}
}
