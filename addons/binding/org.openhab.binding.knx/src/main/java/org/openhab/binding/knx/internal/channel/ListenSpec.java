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

/**
 * Listen meta-data.
 *
 * @author Simon Kaufmann - initial contribution and API.
 *
 */
public class ListenSpec extends AbstractSpec {

    private final List<GroupAddress> listenAddresses;

    public ListenSpec(@Nullable ChannelConfiguration channelConfiguration, String defaultDPT) {
        super(channelConfiguration, defaultDPT);
        if (channelConfiguration != null) {
            this.listenAddresses = channelConfiguration.getListenGAs().stream().map(this::toGroupAddress)
                    .collect(toList());
        } else {
            this.listenAddresses = Collections.emptyList();
        }
    }

    public List<GroupAddress> getListenAddresses() {
        return listenAddresses;
    }

}
