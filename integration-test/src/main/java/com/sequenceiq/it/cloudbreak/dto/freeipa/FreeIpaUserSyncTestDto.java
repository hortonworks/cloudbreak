package com.sequenceiq.it.cloudbreak.dto.freeipa;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.emptyRunningParameter;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SetPasswordRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SyncOperationStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SynchronizeAllUsersRequest;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationState;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractFreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;

@Prototype
public class FreeIpaUserSyncTestDto extends AbstractFreeIpaTestDto<SynchronizeAllUsersRequest, SyncOperationStatus, FreeIpaUserSyncTestDto> {

    private String operationId;

    public FreeIpaUserSyncTestDto(TestContext testContext) {
        super(new SynchronizeAllUsersRequest(), testContext);
    }

    @Override
    public FreeIpaUserSyncTestDto valid() {
        getRequest().setEnvironments(Collections.singleton(getEnvironmentCrn()));
        getRequest().setAccountId(Crn.fromString(getEnvironmentCrn()).getAccountId());
        return this;
    }

    public String getEnvironmentCrn() {
        return getTestContext().get(EnvironmentTestDto.class).getResponse().getCrn();
    }

    public String getOperationId() {
        return operationId;
    }

    public void setOperationId(String operationId) {
        this.operationId = operationId;
    }

    public SetPasswordRequest setPassword(Set<String> environmentCrns, String newPassword) {
        return new SetPasswordRequest(environmentCrns, newPassword);
    }

    public FreeIpaUserSyncTestDto await(OperationState operationState) {
        return getTestContext().await(this, Map.of("status", operationState), emptyRunningParameter());
    }

    @Override
    public FreeIpaUserSyncTestDto awaitForFlow(RunningParameter runningParameter) {
        return throwUnsupportedExceptionForAwait();
    }

    public FreeIpaUserSyncTestDto throwUnsupportedExceptionForAwait() {
        throw new UnsupportedOperationException("use await without parameters");
    }

    @Override
    public String getCrn() {
        return getEnvironmentCrn();
    }

}
