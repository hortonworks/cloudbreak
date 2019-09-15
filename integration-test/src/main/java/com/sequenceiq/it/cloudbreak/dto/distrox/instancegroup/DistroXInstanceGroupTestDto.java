package com.sequenceiq.it.cloudbreak.dto.distrox.instancegroup;

import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.HostGroupModelDescription.RECOVERY_MODE;
import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.COMPUTE;
import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.MASTER;
import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.WORKER;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.RecoveryMode;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.InstanceGroupV1Request;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.cloud.HostGroupType;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractCloudbreakTestDto;

@Prototype
public class DistroXInstanceGroupTestDto extends AbstractCloudbreakTestDto<InstanceGroupV1Request, InstanceGroupV4Response, DistroXInstanceGroupTestDto> {

    private static final String AUTO = "auto";

    private static final String MANUAL = "manual";

    public DistroXInstanceGroupTestDto(TestContext testContext) {
        super(new InstanceGroupV1Request(), testContext);
    }

    public DistroXInstanceGroupTestDto valid() {
        return withHostGroup(MASTER);
    }

    public DistroXInstanceGroupTestDto withHostGroup(HostGroupType hostGroupType) {
        return withRecoveryMode(getRecoveryModeParam(hostGroupType))
                .withNodeCount(hostGroupType.determineInstanceCount(getTestParameter()))
                .withGroup(hostGroupType.getName())
                .withType(hostGroupType.getInstanceGroupType())
                .withName(hostGroupType.getName())
                .withTemplate(getTestContext().given("DistroxInstanceGroupTestDto" + hostGroupType.getName(), DistroXInstanceTemplateTestDto.class));
    }

    public static DistroXInstanceGroupTestDto hostGroup(TestContext testContext, HostGroupType hostGroupType) {
        return create(testContext, hostGroupType);
    }

    public static List<DistroXInstanceGroupTestDto> defaultHostGroup(TestContext testContext) {
        return withHostGroup(testContext, MASTER, COMPUTE, WORKER);
    }

    public static List<DistroXInstanceGroupTestDto> withHostGroup(TestContext testContext, HostGroupType... groupTypes) {
        return Stream.of(groupTypes)
                .map(groupType -> create(testContext, groupType))
                .collect(Collectors.toList());
    }

    public static DistroXInstanceGroupTestDto withHostGroup(TestContext testContext, HostGroupType groupType, int nodeCount) {
        return create(testContext, groupType, nodeCount);
    }

    private static DistroXInstanceGroupTestDto create(TestContext testContext, HostGroupType hostGroupType) {
        DistroXInstanceGroupTestDto entity = testContext.init(DistroXInstanceGroupTestDto.class);
        return create(testContext, hostGroupType, hostGroupType.determineInstanceCount(entity.getTestParameter()));
    }

    private static DistroXInstanceGroupTestDto create(TestContext testContext, HostGroupType hostGroupType, int nodeCount) {
        DistroXInstanceGroupTestDto entity = testContext.init(DistroXInstanceGroupTestDto.class);
        return entity
                .withRecoveryMode(entity.getRecoveryModeParam(hostGroupType))
                .withNodeCount(nodeCount)
                .withGroup(hostGroupType.getName())
                .withType(hostGroupType.getInstanceGroupType())
                .withName(hostGroupType.getName())
                .withTemplate(testContext.given(DistroXInstanceTemplateTestDto.class));
    }

    public DistroXInstanceGroupTestDto withNodeCount(int nodeCount) {
        getRequest().setNodeCount(nodeCount);
        return this;
    }

    public DistroXInstanceGroupTestDto withRecipes(String... recipeNames) {
        for (String recipeName : recipeNames) {
            getRequest().getRecipeNames().add(recipeName);
        }
        return this;
    }

    public DistroXInstanceGroupTestDto withGroup(String group) {
        getRequest().setName(group);
        return this;
    }

    public DistroXInstanceGroupTestDto withType(InstanceGroupType instanceGroupType) {
        getRequest().setType(instanceGroupType);
        return this;
    }

    public DistroXInstanceGroupTestDto withTemplate(DistroXInstanceTemplateTestDto template) {
        getRequest().setTemplate(template.getRequest());
        return this;
    }

    public DistroXInstanceGroupTestDto withRecoveryMode(RecoveryMode recoveryMode) {
        getRequest().setRecoveryMode(recoveryMode);
        return this;
    }

    public DistroXInstanceGroupTestDto withName(String name) {
        getRequest().setName(name);
        return this;
    }

    private RecoveryMode getRecoveryModeParam(HostGroupType hostGroupType) {
        String argumentName = String.join("", hostGroupType.getName(), RECOVERY_MODE);
        String argumentValue = getTestParameter().getWithDefault(argumentName, MANUAL);
        return argumentValue.equals(AUTO) ? RecoveryMode.AUTO : RecoveryMode.MANUAL;
    }
}