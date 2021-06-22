package com.sequenceiq.it.cloudbreak.assertion.audit;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.it.cloudbreak.EnvironmentClient;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;

@Component
public class EnvironmentAuditGrpcServiceAssertion extends AuditGrpcServiceAssertion<EnvironmentTestDto, EnvironmentClient> {

    @Override
    protected OperationInfo getStopOperationInfo() {
        return OperationInfo.builder()
                .withEventName("StopEnvironment")
                .withFirstStates("STOP_DATAHUB_STATE")
                .withLastStates("ENV_STOP_FINISHED_STATE")
                .build();
    }

    @Override
    protected OperationInfo getDeleteOperationInfo() {
        return OperationInfo.builder()
                .withEventName("DeleteEnvironment")
                .withFirstStates("FREEIPA_DELETE_STARTED_STATE")
                .withLastStates("ENV_DELETE_FINISHED_STATE")
                .build();
    }

    @Override
    protected OperationInfo getStartOperationInfo() {
        return OperationInfo.builder()
                .withEventName("StartEnvironment")
                .withFirstStates("START_FREEIPA_STATE")
                .withLastStates("ENV_START_FINISHED_STATE")
                .build();
    }

    @Override
    protected OperationInfo getCreateOperationInfo() {
        return OperationInfo.builder()
                .withEventName("CreateEnvironment")
                .withFirstStates("ENVIRONMENT_INITIALIZATION_STATE")
                .withLastStates("ENV_CREATION_FINISHED_STATE")
                .build();
    }

    @Override
    protected Crn.Service getService() {
        return Crn.Service.ENVIRONMENTS;
    }
}
