package com.sequenceiq.it.cloudbreak.assertion.audit;

import java.util.List;

import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.audit.AuditProto;
import com.sequenceiq.cloudbreak.audit.model.ActorCrn;
import com.sequenceiq.cloudbreak.audit.model.ListAuditEvent;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;

@Component
public class DatahubAuditGrpcServiceAssertion extends AuditGrpcServiceAssertion<DistroXTestDto, CloudbreakClient> {

    @Override
    protected OperationInfo getStopOperationInfo() {
        return OperationInfo.builder()
                .withEventName("StopDatahubCluster")
                .withFirstState("CLUSTER_STOPPING_STATE")
                .withLastState("STOP_FINISHED_STATE")
                .build();
    }

    @Override
    protected OperationInfo getDeleteOperationInfo() {
        return OperationInfo.builder()
                .withEventName("DeleteDatahubCluster")
                .withFirstState("CLUSTER_TERMINATING_STATE")
                .withLastState("TERMINATION_FINISHED_STATE")
                .build();
    }

    @Override
    protected OperationInfo getStartOperationInfo() {
        return OperationInfo.builder()
                .withEventName("StartDatahubCluster")
                .withFirstState("START_STATE")
                .withLastState("CLUSTER_START_FINISHED_STATE")
                .build();
    }

    @Override
    protected OperationInfo getCreateOperationInfo() {
        return OperationInfo.builder()
                .withEventName("CreateDatahubCluster")
                .withFirstState("VALIDATION_STATE")
                .withLastState("CLUSTER_CREATION_FINISHED_STATE")
                .build();
    }

    public DistroXTestDto upgradeClusterByNameInternal(TestContext testContext, DistroXTestDto testDto) {
        OperationInfo operationInfo = OperationInfo.builder()
                .withEventName("UpgradeDatahubCluster")
                .withFirstState("UPDATE_SALT_STATE_FILES_STATE")
                .withLastState("STACK_IMAGE_UPDATE_FINISHED")
                .build();
        List<AuditProto.CdpAuditEvent> cdpAuditEvents = getAuditClient().listEvents(ListAuditEvent.builder()
                .actor(ActorCrn.builder().withActorCrn(testContext.getActingUserCrn().toString()).build())
                .eventSource(getService()).build());
        validateEventList(cdpAuditEvents, testDto, operationInfo);
        return testDto;
    }

    @Override
    protected Crn.Service getService() {
        return Crn.Service.DATAHUB;
    }
}
