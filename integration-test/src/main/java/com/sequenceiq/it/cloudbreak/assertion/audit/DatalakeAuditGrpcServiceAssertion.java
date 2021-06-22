package com.sequenceiq.it.cloudbreak.assertion.audit;

import java.util.List;

import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.audit.AuditProto;
import com.sequenceiq.cloudbreak.audit.model.ActorCrn;
import com.sequenceiq.cloudbreak.audit.model.ListAuditEvent;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.it.cloudbreak.SdxClient;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;

@Component
public class DatalakeAuditGrpcServiceAssertion extends AuditGrpcServiceAssertion<SdxTestDto, SdxClient> {

    public SdxTestDto rotateAutotlsCertificates(TestContext testContext, SdxTestDto testDto, SdxClient client) {
        OperationInfo operationInfo = OperationInfo.builder()
                .withEventName("RotateDatalakeClusterCertificates")
                .withFirstStates("CLUSTER_CMCA_ROTATION_STATE")
                .withLastStates("CLUSTER_CERTIFICATES_ROTATION_FINISHED_STATE")
                .build();
        List<AuditProto.CdpAuditEvent> cdpAuditEvents = getAuditClient().listEvents(ListAuditEvent.builder()
                .actor(ActorCrn.builder().withActorCrn(testContext.getActingUserCrn().toString()).build())
                .eventSource(getService()).build());
        validateEventList(cdpAuditEvents, testDto, operationInfo);
        return testDto;
    }

    public SdxTestDto upgradeClusterByNameInternal(TestContext testContext, SdxTestDto testDto, SdxClient client) {
        OperationInfo operationInfo = OperationInfo.builder()
                .withEventName("UpgradeDatalakeCluster")
                .withFirstStates("DATALAKE_UPGRADE_START_STATE")
                .withLastStates("DATALAKE_UPGRADE_FINISHED_STATE")
                .build();
        List<AuditProto.CdpAuditEvent> cdpAuditEvents = getAuditClient().listEvents(ListAuditEvent.builder()
                .actor(ActorCrn.builder().withActorCrn(testContext.getActingUserCrn().toString()).build())
                .eventSource(getService()).build());
        validateEventList(cdpAuditEvents, testDto, operationInfo);
        return testDto;
    }

    @Override
    protected OperationInfo getStopOperationInfo() {
        return OperationInfo.builder()
                .withEventName("StopDatalakeCluster")
                .withFirstStates("SDX_STOP_SYNC_STATE")
                .withLastStates("SDX_STOP_FINISHED_STATE")
                .build();
    }

    @Override
    protected OperationInfo getDeleteOperationInfo() {
        return OperationInfo.builder()
                .withEventName("DeleteDatalakeCluster")
                .withFirstStates("SDX_DELETION_START_STATE")
                .withLastStates("SDX_DELETION_FINISHED_STATE")
                .build();
    }

    @Override
    protected OperationInfo getStartOperationInfo() {
        return OperationInfo.builder()
                .withEventName("StartDatalakeCluster")
                .withFirstStates("SDX_START_RDS_START_STATE")
                .withLastStates("SDX_START_FINISHED_STATE")
                .build();
    }

    @Override
    protected OperationInfo getCreateOperationInfo() {
        return OperationInfo.builder()
                .withEventName("CreateDatalakeCluster")
                .withFirstStates("SDX_CREATION_WAIT_RDS_STATE")
                .withLastStates("SDX_CREATION_FINISHED_STATE")
                .build();
    }

    @Override
    protected Crn.Service getService() {
        return Crn.Service.DATALAKE;
    }
}
