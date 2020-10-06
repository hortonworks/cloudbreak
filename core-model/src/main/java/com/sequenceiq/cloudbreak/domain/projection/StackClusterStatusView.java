package com.sequenceiq.cloudbreak.domain.projection;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.common.api.type.CertExpirationState;

public interface StackClusterStatusView {

    Long getId();

    Status getStatus();

    String getStatusReason();

    Status getClusterStatus();

    String getClusterStatusReason();

    String getCrn();

    CertExpirationState getCertExpirationState();
}
