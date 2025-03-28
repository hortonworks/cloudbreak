package com.sequenceiq.cloudbreak.domain.projection;

import java.util.Set;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.common.api.type.CertExpirationState;
import com.sequenceiq.common.model.ProviderSyncState;

public interface StackClusterStatusView {

    Long getId();

    String getName();

    Status getStatus();

    String getStatusReason();

    Long getClusterId();

    Status getClusterStatus();

    String getClusterStatusReason();

    String getCrn();

    CertExpirationState getCertExpirationState();

    String getCertExpirationDetails();

    Set<ProviderSyncState> getProviderSyncStates();
}