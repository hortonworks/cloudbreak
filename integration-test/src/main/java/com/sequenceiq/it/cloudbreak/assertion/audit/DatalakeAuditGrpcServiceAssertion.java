package com.sequenceiq.it.cloudbreak.assertion.audit;

import java.util.List;

import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.audit.AuditProto;
import com.sequenceiq.cloudbreak.audit.model.ActorCrn;
import com.sequenceiq.cloudbreak.audit.model.ListAuditEvent;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.it.cloudbreak.SdxClient;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;

@Component
public class DatalakeAuditGrpcServiceAssertion extends AuditGrpcServiceAssertion<SdxTestDto, SdxClient> {

    public SdxTestDto rotateAutotlsCertificates(TestContext testContext, SdxTestDto testDto, SdxClient client) {
        OperationInfo operationInfo = OperationInfo.builder()
                .withEventName("RotateDatalakeClusterCertificates")
                .withFirstState("CLUSTER_CMCA_ROTATION_STATE")
                .withLastState("CLUSTER_CERTIFICATES_ROTATION_FINISHED_STATE")
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
                .withFirstState("DATALAKE_UPGRADE_START_STATE")
                .withLastState("DATALAKE_UPGRADE_FINISHED_STATE")
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
                .withFirstState("SDX_STOP_SYNC_STATE")
                .withLastState("SDX_STOP_FINISHED_STATE")
                .build();
    }

    @Override
    protected OperationInfo getDeleteOperationInfo() {
        return OperationInfo.builder()
                .withEventName("DeleteDatalakeCluster")
                .withFirstState("SDX_DELETION_START_STATE")
                .withLastState("SDX_DELETION_FINISHED_STATE")
                .build();
    }

    @Override
    protected OperationInfo getStartOperationInfo() {
        return OperationInfo.builder()
                .withEventName("StartDatalakeCluster")
                .withFirstState("SDX_START_RDS_START_STATE")
                .withLastState("SDX_START_FINISHED_STATE")
                .build();
    }

    @Override
    protected OperationInfo getCreateOperationInfo() {
        return OperationInfo.builder()
                .withEventName("CreateDatalakeCluster")
                .withFirstState("SDX_CREATION_WAIT_RDS_STATE")
                .withLastState("SDX_CREATION_FINISHED_STATE")
                .build();
    }

    @Override
    protected Crn.Service getService() {
        return Crn.Service.DATALAKE;
    }
}
