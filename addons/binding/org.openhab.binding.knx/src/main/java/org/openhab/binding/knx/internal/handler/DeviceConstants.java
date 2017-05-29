package org.openhab.binding.knx.internal.handler;

public class DeviceConstants {

    private DeviceConstants() {
        // prevent instantiation
    }

    // Memory addresses for device information
    public static final int MEM_DOA = 0x0102; // length 2
    public static final int MEM_MANUFACTURERID = 0x0104;
    public static final int MEM_DEVICETYPE = 0x0105; // length 2
    public static final int MEM_VERSION = 0x0107;
    public static final int MEM_PEI = 0x0109;
    public static final int MEM_RUNERROR = 0x010d;
    public static final int MEM_GROUPOBJECTABLEPTR = 0x0112;
    public static final int MEM_PROGRAMPTR = 0x0114;
    public static final int MEM_GROUPADDRESSTABLE = 0x0116; // max. length 233

    // Interface Object indexes
    public static final int DEVICE_OBJECT = 0; // Device Object
    public static final int ADDRESS_TABLE_OBJECT = 1; // Addresstable Object
    public static final int ASSOCIATION_TABLE_OBJECT = 2; // Associationtable Object
    public static final int APPLICATION_PROGRAM_TABLE = 3; // Application Program Object
    public static final int INTERFACE_PROGRAM_OBJECT = 4; // Interface Program Object
    public static final int GROUPOBJECT_OBJECT = 9; // Group Object Object
    public static final int KNXNET_IP_OBJECT = 11; // KNXnet/IP Parameter Object

    // Property IDs for device information;
    public static final int HARDWARE_TYPE = 78;

}
