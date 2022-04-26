package com.sequenceiq.cloudbreak.domain.stack.cluster.gateway;

import java.util.Set;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.GatewayType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.SSOType;

public interface GatewayDto {

    Long getId();

    GatewayType getGatewayType();

    String getPath();

    SSOType getSsoType();

    String getSsoProvider();

    String getSignKey();

    String getSignCert();

    String getSignPub();

    String getTokenCert();

    String getKnoxMasterSecret();

    Set<GatewayTopology> getTopologies();

    Integer getGatewayPort();
}
