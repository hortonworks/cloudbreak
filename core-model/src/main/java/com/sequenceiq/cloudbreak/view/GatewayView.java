package com.sequenceiq.cloudbreak.view;

import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNull;

import java.util.Optional;
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

    Secret getSignCertSecret();

    Secret getSignPubSecret();

    String getSignCertDeprecated();

    String getSignPubDeprecated();

    Secret getKnoxMasterSecret();

    Set<GatewayTopology> getTopologies();

    Integer getGatewayPort();

    String getTokenCert();

    default String getSignKey() {
        return getIfNotNull(getSignKeySecret(), Secret::getRaw);
    }

    default String getSignCert() {
        return Optional.ofNullable(getIfNotNull(getSignCertSecret(), Secret::getRaw)).orElse(getSignCertDeprecated());
    }

    default String getSignPub() {
        return Optional.ofNullable(getIfNotNull(getSignPubSecret(), Secret::getRaw)).orElse(getSignPubDeprecated());
    }

    default String getKnoxMaster() {
        return getIfNotNull(getKnoxMasterSecret(), Secret::getRaw);
    }
}
