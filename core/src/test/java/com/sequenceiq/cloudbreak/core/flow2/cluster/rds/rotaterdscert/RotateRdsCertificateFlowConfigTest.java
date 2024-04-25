package com.sequenceiq.cloudbreak.core.flow2.cluster.rds.rotaterdscert;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.cloudera.thunderhead.service.common.usage.UsageProto;

class RotateRdsCertificateFlowConfigTest {

    private final RotateRdsCertificateFlowConfig underTest = new RotateRdsCertificateFlowConfig();

    @ParameterizedTest(name = "Test environment usage status use case mapping with flow state: {0}")
    @EnumSource(RotateRdsCertificateState.class)
    void testGetUseCaseForFlowState(RotateRdsCertificateState flowState) {
        UsageProto.CDPClusterStatus.Value actual = underTest.getUseCaseForFlowState(flowState);

        if (RotateRdsCertificateState.INIT_STATE.equals(flowState)) {
            assertEquals(UsageProto.CDPClusterStatus.Value.ROTATE_RDS_CERTIFICATE_STARTED, actual);
        } else if (RotateRdsCertificateState.ROTATE_RDS_CERTIFICATE_FINISHED_STATE.equals(flowState)) {
            assertEquals(UsageProto.CDPClusterStatus.Value.ROTATE_RDS_CERTIFICATE_FINISHED, actual);
        } else if (RotateRdsCertificateState.ROTATE_RDS_CERTIFICATE_FAILED_STATE.equals(flowState)) {
            assertEquals(UsageProto.CDPClusterStatus.Value.ROTATE_RDS_CERTIFICATE_FAILED, actual);
        } else {
            assertEquals(UsageProto.CDPClusterStatus.Value.UNSET, actual);
        }
    }
}
