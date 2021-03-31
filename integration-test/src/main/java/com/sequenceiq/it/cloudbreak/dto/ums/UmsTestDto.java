package com.sequenceiq.it.cloudbreak.dto.ums;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.emptyRunningParameter;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.UmsClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.action.ums.AssignResourceRequest;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.assign.Assignable;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractTestDto;

@Prototype
public class UmsTestDto extends AbstractTestDto<AssignResourceRequest, UserManagementProto.User, UmsTestDto, UmsClient> {

    private static final String UMS = "UMS";

    private static final String DH_CREATOR_CRN = "crn:altus:iam:us-west-1:altus:resourceRole:DataHubCreator";

    private static final String DH_ADMIN_CRN = "crn:altus:iam:us-west-1:altus:resourceRole:DataHubAdmin";

    private static final String DH_USER_CRN = "crn:altus:iam:us-west-1:altus:resourceRole:DataHubUser";

    private static final String ENV_USER_CRN = "crn:altus:iam:us-west-1:altus:resourceRole:EnvironmentUser";

    private static final String ENV_ADMIN_CRN = "crn:altus:iam:us-west-1:altus:resourceRole:EnvironmentAdmin";

    private static final String DATA_STEWARD_CRN = "crn:altus:iam:us-west-1:altus:resourceRole:DataSteward";

    private static final String SHARED_RESOURCE_USER = "crn:altus:iam:us-west-1:altus:resourceRole:SharedResourceUser";

    public UmsTestDto(TestContext testContext) {
        super(new AssignResourceRequest(), testContext);
    }

    public UmsTestDto() {
        super(UmsTestDto.class.getSimpleName().toUpperCase());
        setRequest(new AssignResourceRequest());
    }

    public UmsTestDto withDatahubCreator() {
        getRequest().setRoleCrn(DH_CREATOR_CRN);
        return this;
    }

    public UmsTestDto withEnvironmentUser() {
        getRequest().setRoleCrn(ENV_USER_CRN);
        return this;
    }

    public UmsTestDto withEnvironmentAdmin() {
        getRequest().setRoleCrn(ENV_ADMIN_CRN);
        return this;
    }

    public UmsTestDto withDataSteward() {
        getRequest().setRoleCrn(DATA_STEWARD_CRN);
        return this;
    }

    public UmsTestDto withSharedResourceUser() {
        getRequest().setRoleCrn(SHARED_RESOURCE_USER);
        return this;
    }

    public UmsTestDto withDatahubAdmin() {
        getRequest().setRoleCrn(DH_ADMIN_CRN);
        return this;
    }

    public UmsTestDto withDatahubUser() {
        getRequest().setRoleCrn(DH_USER_CRN);
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

    public UmsTestDto valid() {
        return new UmsTestDto();
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
