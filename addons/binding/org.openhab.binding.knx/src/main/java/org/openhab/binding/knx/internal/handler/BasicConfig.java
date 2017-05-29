package org.openhab.binding.knx.internal.handler;

import java.math.BigDecimal;

public class BasicConfig {

    private String address;
    private Boolean fetch;
    private BigDecimal interval;

    public String getAddress() {
        return address;
    }

    public Boolean getFetch() {
        return fetch;
    }

    public BigDecimal getInterval() {
        return interval;
    }

}
