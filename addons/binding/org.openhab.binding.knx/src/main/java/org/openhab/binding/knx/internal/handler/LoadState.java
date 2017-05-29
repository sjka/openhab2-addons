package org.openhab.binding.knx.internal.handler;

public enum LoadState {
    L0(0, "Unloaded"),
    L1(1, "Loaded"),
    L2(2, "Loading"),
    L3(3, "Error"),
    L4(4, "Unloading"),
    L5(5, "Load Completing");

    private int code;
    private String name;

    private LoadState(int code, String name) {
        this.code = code;
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    public static String getName(int code) {
        for (LoadState c : LoadState.values()) {
            if (c.code == code) {
                return c.name;
            }
        }
        return null;
    }

};
