package com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ccm;

import java.time.LocalDateTime;

import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.common.api.type.Tunnel;

public interface UpgradeCcmBaseEvent extends Payload {

    Long getResourceId();

    Long getClusterId();

    Tunnel getOldTunnel();

    LocalDateTime getRevertTime();
}
