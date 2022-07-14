package com.sequenceiq.cloudbreak.view;

import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNull;

import java.util.Set;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.GatewayType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.SSOType;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.GatewayTopology;
import com.sequenceiq.cloudbreak.service.secret.domain.Secret;

public interface GatewayView {

    Long getId();

    GatewayType getGatewayType();

    String getPath();

    SSOType getSsoType();

    String getSsoProvider();

    Secret getSignKeySecret();

    String getSignCert();

    String getSignPub();

    String getTokenCert();

    Secret getKnoxMasterSecret();

    Set<GatewayTopology> getTopologies();

    Integer getGatewayPort();

    default String getSignKey() {
        return getIfNotNull(getSignKeySecret(), Secret::getRaw);
    }

    default String getKnoxMaster() {
        return getIfNotNull(getKnoxMasterSecret(), Secret::getRaw);
    }
}
