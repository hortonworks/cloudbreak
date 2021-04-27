package com.sequenceiq.it.cloudbreak.dto.ums;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.emptyRunningParameter;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.UmsClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractTestDto;
import com.sequenceiq.it.cloudbreak.request.ums.CreateUserGroupRequest;

@Prototype
public class UmsGroupTestDto extends AbstractTestDto<CreateUserGroupRequest, UserManagementProto.Group, UmsGroupTestDto, UmsClient> {

    public static final String UMS_GROUP = "UMS_GROUP";

    private static final String UMS_GROUP_NAME = "umsGroupName";

    public UmsGroupTestDto(TestContext testContext) {
        super(new CreateUserGroupRequest(), testContext);
    }

    public UmsGroupTestDto(CreateUserGroupRequest createUserGroupRequest, TestContext testContext) {
        super(createUserGroupRequest, testContext);
    }

    public UmsGroupTestDto() {
        super(UMS_GROUP);
        setRequest(new CreateUserGroupRequest());
    }

    @Override
    public UmsGroupTestDto valid() {
        return withName(getResourcePropertyProvider().getName(getCloudPlatform()))
                .withAccountId(getTestContext().getActingUserCrn().getAccountId());
    }

    public UmsGroupTestDto withName(String name) {
        getRequest().setGroupName(name);
        setName(name);
        return this;
    }

    public UmsGroupTestDto withAccountId(String accountId) {
        getRequest().setAccountId(accountId);
        return this;
    }

    public UmsGroupTestDto withMember(String memberCrn) {
        getRequest().setMember(memberCrn);
        return this;
    }

    @Override
    public String getResourceNameType() {
        return UMS_GROUP_NAME;
    }

    @Override
    public String getCrn() {
        if (getResponse() == null) {
            throw new IllegalStateException("UMS Group response hasn't been set, therefore 'getCrn' cannot be fulfilled.");
        }
        return getResponse().getCrn();
    }

    @Override
    public UmsGroupTestDto when(Action<UmsGroupTestDto, UmsClient> action) {
        return getTestContext().when((UmsGroupTestDto) this, UmsClient.class, action, emptyRunningParameter());
    }

    @Override
    public <E extends Exception> UmsGroupTestDto whenException(Action<UmsGroupTestDto, UmsClient> action, Class<E> expectedException,
            RunningParameter runningParameter) {
        return getTestContext().whenException((UmsGroupTestDto) this, UmsClient.class, action, expectedException, runningParameter);
    }

    @Override
    public UmsGroupTestDto then(Assertion<UmsGroupTestDto, UmsClient> assertion) {
        return then(assertion, emptyRunningParameter());
    }

    @Override
    public UmsGroupTestDto then(Assertion<UmsGroupTestDto, UmsClient> assertion, RunningParameter runningParameter) {
        return getTestContext().then((UmsGroupTestDto) this, UmsClient.class, assertion, runningParameter);
    }
}
