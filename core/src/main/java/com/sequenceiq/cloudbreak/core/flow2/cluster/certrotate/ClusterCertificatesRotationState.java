package com.sequenceiq.cloudbreak.core.flow2.cluster.certrotate;

import com.sequenceiq.flow.core.FlowState;

public enum ClusterCertificatesRotationState implements FlowState {
    INIT_STATE,
    CLUSTER_CMCA_ROTATION_STATE,
    CLUSTER_HOST_CERTIFICATES_ROTATION_STATE,
    CLUSTER_CERTIFICATES_ROTATION_FINISHED_STATE,
    CLUSTER_CERTIFICATES_ROTATION_FAILED_STATE,
    FINAL_STATE;
}
