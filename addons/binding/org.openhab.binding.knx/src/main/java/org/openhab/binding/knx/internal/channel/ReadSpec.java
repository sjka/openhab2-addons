/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.knx.internal.channel;

import static java.util.stream.Collectors.toList;

import java.util.Collections;
import java.util.List;

import org.eclipse.jdt.annotation.Nullable;

import tuwien.auto.calimero.GroupAddress;
import tuwien.auto.calimero.exception.KNXFormatException;

/**
 * Read meta-data.
 *
 * @author Simon Kaufmann - initial contribution and API.
 *
 */
public class ReadSpec {

    private final String dpt;
    private final List<GroupAddress> readAddresses;

    public ReadSpec(@Nullable ChannelConfiguration channelConfiguration, String defaultDPT) {
        if (channelConfiguration != null) {
            this.dpt = channelConfiguration.getDPT() != null ? channelConfiguration.getDPT() : defaultDPT;
            this.readAddresses = channelConfiguration.getReadGAs().stream().map(this::toGroupAddress).collect(toList());
        } else {
            this.dpt = defaultDPT;
            this.readAddresses = Collections.emptyList();
        }
    }

    public String getDPT() {
        return dpt;
    }

    public List<GroupAddress> getReadAddresses() {
        return readAddresses;
    }

    private GroupAddress toGroupAddress(GroupAddressConfiguration ga) {
        try {
            return new GroupAddress(ga.getGA());
        } catch (KNXFormatException e) {
            return null;
        }
    }

}
