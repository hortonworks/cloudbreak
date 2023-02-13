package com.sequenceiq.it.cloudbreak.dto.ums;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.emptyRunningParameter;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.sequenceiq.cloudbreak.auth.altus.service.UmsResourceRole;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.assign.Assignable;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractTestDto;
import com.sequenceiq.it.cloudbreak.microservice.UmsClient;
import com.sequenceiq.it.cloudbreak.request.ums.AssignResourceRequest;

@Prototype
public class UmsTestDto extends AbstractTestDto<AssignResourceRequest, UserManagementProto.User, UmsTestDto, UmsClient> {

    public UmsTestDto(TestContext testContext) {
        super(new AssignResourceRequest(), testContext);
    }

    public UmsTestDto withDatahubCreator() {
        getRequest().setUmsResourceRole(UmsResourceRole.DATAHUB_CREATOR);
        return this;
    }

    public UmsTestDto withOwner() {
        getRequest().setUmsResourceRole(UmsResourceRole.OWNER);
        return this;
    }

    public UmsTestDto withEnvironmentUser() {
        getRequest().setUmsResourceRole(UmsResourceRole.ENVIRONMENT_USER);
        return this;
    }

    public UmsTestDto withEnvironmentAdmin() {
        getRequest().setUmsResourceRole(UmsResourceRole.ENVIRONMENT_ADMIN);
        return this;
    }

    public UmsTestDto withDataSteward() {
        getRequest().setUmsResourceRole(UmsResourceRole.DATA_STEWARD);
        return this;
    }

    public UmsTestDto withSharedResourceUser() {
        getRequest().setUmsResourceRole(UmsResourceRole.SHARED_RESOURCE_USER);
        return this;
    }

    public UmsTestDto withDatahubAdmin() {
        getRequest().setUmsResourceRole(UmsResourceRole.DATAHUB_ADMIN);
        return this;
    }

    public UmsTestDto withDatahubUser() {
        getRequest().setUmsResourceRole(UmsResourceRole.DATAHUB_USER);
        return this;
    }

    public UmsTestDto withGroupAdmin() {
        getRequest().setUmsResourceRole(UmsResourceRole.IAM_GROUP_ADMIN);
        return this;
    }

    public UmsTestDto withEnvironmentPrivilegedUser() {
        getRequest().setUmsResourceRole(UmsResourceRole.ENVIRONMENT_PRIVILEGED_USER);
        return this;
    }

    public UmsTestDto assignTarget(String key) {
        try {
            Assignable dto = getTestContext().get(key);
            getRequest().setResourceCrn(dto.getCrn());
        } catch (ClassCastException e) {
            throw new IllegalArgumentException(String.format("TestContext member with key %s does not implement %s interface",
                    key, Assignable.class.getCanonicalName()), e);
        }
        return this;
    }

    public UmsTestDto assignTargetByCrn(String targetCrn) {
        getRequest().setResourceCrn(targetCrn);
        return this;
    }

    public UmsTestDto valid() {
        return this;
    }

    @Override
    public UmsTestDto when(Action<UmsTestDto, UmsClient> action) {
        return getTestContext().when((UmsTestDto) this, UmsClient.class, action, emptyRunningParameter());
    }

    @Override
    public UmsTestDto then(Assertion<UmsTestDto, UmsClient> assertion) {
        return then(assertion, emptyRunningParameter());
    }

    @Override
    public UmsTestDto then(Assertion<UmsTestDto, UmsClient> assertion, RunningParameter runningParameter) {
        return getTestContext().then((UmsTestDto) this, UmsClient.class, assertion, runningParameter);
    }
}
