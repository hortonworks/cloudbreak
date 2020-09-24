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
    protected String getStopEventName() {
        return "StopFreeipa";
    }

    @NotNull
    @Override
    protected String getDeleteEventName() {
        return "DeleteFreeipa";
    }

    @NotNull
    @Override
    protected String getStartEventName() {
        return "StartFreeipa";
    }

    @NotNull
    @Override
    protected String getCreateEventName() {
        return "CreateFreeipa";
    }

    @Override
    protected Crn.Service getService() {
        return Crn.Service.FREEIPA;
    }
}
