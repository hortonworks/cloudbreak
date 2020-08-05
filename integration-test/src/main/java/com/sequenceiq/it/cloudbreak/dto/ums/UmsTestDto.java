package com.sequenceiq.it.cloudbreak.dto.ums;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.emptyRunningParameter;

import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.UmsClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.action.ums.AssignResourceRequest;
import com.sequenceiq.it.cloudbreak.assign.Assignable;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractTestDto;

@Prototype
public class UmsTestDto extends AbstractTestDto<AssignResourceRequest, Object, UmsTestDto, UmsClient> {

    private static final String UMS = "UMS";

    private static final String DH_CREATOR_CRN = "crn:altus:iam:us-west-1:altus:resourceRole:DataHubCreator";

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
}
