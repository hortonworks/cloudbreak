package com.sequenceiq.cloudbreak.domain.projection;

import javax.persistence.Convert;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.domain.StatusConverter;
import com.sequenceiq.common.api.type.CertExpirationState;

public interface StackClusterStatusView {

    Long getId();

    @Convert(converter = StatusConverter.class)
    Status getStatus();

    String getStatusReason();

    @Convert(converter = StatusConverter.class)
    Status getClusterStatus();

    String getClusterStatusReason();

    String getCrn();

    CertExpirationState getCertExpirationState();
}
