package com.sequenceiq.cloudbreak.reactor.api.event.cluster.certrotate;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.CertificateRotationType;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.flow.core.FlowParameters;

public class ClusterCertificateRotationContext extends CommonContext {

    private final CertificateRotationType certificateRotationType;

    public ClusterCertificateRotationContext(FlowParameters flowParameters, CertificateRotationType certificateRotationType) {
        super(flowParameters);
        this.certificateRotationType = certificateRotationType;
    }

    public CertificateRotationType getCertificateRotationType() {
        return certificateRotationType;
    }
}
