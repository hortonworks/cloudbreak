package com.sequenceiq.it.cloudbreak.assertion.audit;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.it.cloudbreak.FreeIpaClient;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;

@Component
public class FreeIpaAuditGrpcServiceAssertion extends AuditGrpcServiceAssertion<FreeIpaTestDto, FreeIpaClient> {

    @NotNull
    @Override
    protected OperationInfo getStopOperationInfo() {
        return OperationInfo.builder()
                .withEventName("StopFreeipa")
                .withFirstState("STOP_STATE")
                .withLastState("STOP_FINISHED_STATE")
                .build();
    }

    @NotNull
    @Override
    protected OperationInfo getDeleteOperationInfo() {
        return OperationInfo.builder()
                .withEventName("DeleteFreeipa")
                .withFirstState("STOP_TELEMETRY_AGENT_STATE")
                .withLastState("TERMINATION_FINISHED_STATE")
                .build();
    }

    @NotNull
    @Override
    protected OperationInfo getStartOperationInfo() {
        return OperationInfo.builder()
                .withEventName("StartFreeipa")
                .withFirstState("START_STATE")
                .withLastState("START_FINISHED_STATE")
                .build();
    }

    @NotNull
    @Override
    protected OperationInfo getCreateOperationInfo() {
        return OperationInfo.builder()
                .withEventName("CreateFreeipa")
                .withFirstState("BOOTSTRAPPING_MACHINES_STATE")
                .withLastState("FREEIPA_PROVISION_FINISHED_STATE")
                .build();
    }

    @Override
    protected Crn.Service getService() {
        return Crn.Service.FREEIPA;
    }
}
