package com.sequenceiq.it.cloudbreak.assertion.audit;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.it.cloudbreak.EnvironmentClient;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;

@Component
public class EnvironmentAuditGrpcServiceAssertion extends AuditGrpcServiceAssertion<EnvironmentTestDto, EnvironmentClient> {

    @Override
    protected OperationInfo getStopOperationInfo() {
        return OperationInfo.builder()
                .withEventName("StopEnvironment")
                .withFirstState("STOP_DATAHUB_STATE")
                .withLastState("ENV_STOP_FINISHED_STATE")
                .build();
    }

    @Override
    protected OperationInfo getDeleteOperationInfo() {
        return OperationInfo.builder()
                .withEventName("DeleteEnvironment")
                .withFirstState("FREEIPA_DELETE_STARTED_STATE")
                .withLastState("ENV_DELETE_FINISHED_STATE")
                .build();
    }

    @Override
    protected OperationInfo getStartOperationInfo() {
        return OperationInfo.builder()
                .withEventName("StartEnvironment")
                .withFirstState("START_FREEIPA_STATE")
                .withLastState("ENV_START_FINISHED_STATE")
                .build();
    }

    @Override
    protected OperationInfo getCreateOperationInfo() {
        return OperationInfo.builder()
                .withEventName("CreateEnvironment")
                .withFirstState("ENVIRONMENT_INITIALIZATION_STATE")
                .withLastState("ENV_CREATION_FINISHED_STATE")
                .build();
    }

    @Override
    protected Crn.Service getService() {
        return Crn.Service.ENVIRONMENTS;
    }
}
