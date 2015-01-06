package org.openhab.binding.knx.handler;

import tuwien.auto.calimero.GroupAddress;

/**
 * The {@link GAStatusListener} is an interface that needs to be
 * implemented by classes that want to listen to Group Addresses
 * on the KNX bus
 * 
 * @author Karel Goderis - Initial contribution
 */
public interface GAStatusListener {

	
	/**
	 * 
	 * Called when the KNX bridge receives data
	 * 
	 * @param bridge
	 * @param destination
	 * @param asdu
	 */
	public void onDataReceived(KNXBridgeBaseThingHandler bridge, GroupAddress destination, byte[] asdu);
	
	/**
	 * 
	 * Called when the connection with the KNX bridge is lost
	 * 
	 * @param bridge
	 */
	public void onBridgeDisconnected(KNXBridgeBaseThingHandler bridge);
	
	/**
	 * Called when the connection with the KNX bridge is established
	 * 
	 * @param bridge
	 */
	public void onBridgeConnected(KNXBridgeBaseThingHandler bridge);

	/**
	 * Called to verify if the GAStatusListener has an interest in the given GroupAddress
	 * 
	 * @param destination
	 */
	public boolean listensTo(GroupAddress destination);

	
}
