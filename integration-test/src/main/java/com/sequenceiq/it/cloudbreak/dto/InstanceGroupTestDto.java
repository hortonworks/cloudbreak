package com.sequenceiq.it.cloudbreak.dto;

import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.HostGroupModelDescription.RECOVERY_MODE;
import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.COMPUTE;
import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.GATEWAY;
import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.IDBROKER;
import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.MASTER;
import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.WORKER;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.RecoveryMode;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.cloud.HostGroupType;
import com.sequenceiq.it.cloudbreak.context.TestContext;

@Prototype
public class InstanceGroupTestDto extends AbstractCloudbreakTestDto<InstanceGroupV4Request, InstanceGroupV4Response, InstanceGroupTestDto> {

    private static final String AUTO = "auto";

    private static final String MANUAL = "manual";

    protected InstanceGroupTestDto(InstanceGroupV4Request request, TestContext testContext) {
        super(request, testContext);
    }

    protected InstanceGroupTestDto(TestContext testContext) {
        super(new InstanceGroupV4Request(), testContext);
    }

    public InstanceGroupTestDto() {
        super(InstanceGroupTestDto.class.getSimpleName().toUpperCase());
    }

    public InstanceGroupTestDto valid() {
        return withHostGroup(MASTER);
    }

    public InstanceGroupTestDto withHostGroup(HostGroupType hostGroupType) {
        final String instanceTemplateName = String.format("%s-%s-%s",
                InstanceTemplateV4TestDto.class.getSimpleName(), getCloudPlatform(), hostGroupType.getName());
        return withRecoveryMode(getRecoveryModeParam(hostGroupType))
                .withNodeCount(hostGroupType.determineInstanceCount(getTestParameter()))
                .withGroup(hostGroupType.getName())
                .withSecurityGroup(getTestContext().init(SecurityGroupTestDto.class))
                .withType(hostGroupType.getInstanceGroupType())
                .withName(hostGroupType.getName())
                .withTemplate(getTestContext().given(instanceTemplateName, InstanceTemplateV4TestDto.class, getCloudPlatform()));
    }

    public InstanceGroupTestDto withNetwork() {
        getRequest().setNetwork(getCloudProvider().instanceGroupNetworkV4Request(SubnetId.ordinals()));
        return this;
    }

    public InstanceGroupTestDto withNetwork(SubnetId ids) {
        getRequest().setNetwork(getCloudProvider().instanceGroupNetworkV4Request(ids));
        return this;
    }

    public InstanceGroupTestDto withoutNetwork() {
        getRequest().setNetwork(null);
        return this;
    }

    public static InstanceGroupTestDto hostGroup(TestContext testContext, HostGroupType hostGroupType) {
        return create(testContext, hostGroupType);
    }

    public static List<InstanceGroupTestDto> defaultHostGroup(TestContext testContext) {
        return withHostGroup(testContext, MASTER, COMPUTE, WORKER, GATEWAY);
    }

    public static List<InstanceGroupTestDto> sdxHostGroup(TestContext testContext) {
        return withHostGroup(testContext, MASTER, IDBROKER);
    }

    public static List<InstanceGroupTestDto> withHostGroup(TestContext testContext, HostGroupType... groupTypes) {
        return Stream.of(groupTypes)
                .map(groupType -> create(testContext, groupType))
                .collect(Collectors.toList());
    }

    public static InstanceGroupTestDto withHostGroup(TestContext testContext, HostGroupType groupType, int nodeCount) {
        return create(testContext, groupType, nodeCount);
    }

    private static InstanceGroupTestDto create(TestContext testContext, HostGroupType hostGroupType) {
        InstanceGroupTestDto entity = testContext.init(InstanceGroupTestDto.class);
        return create(testContext, hostGroupType, hostGroupType.determineInstanceCount(entity.getTestParameter()));
    }

    private static InstanceGroupTestDto create(TestContext testContext, HostGroupType hostGroupType, int nodeCount) {
        InstanceGroupTestDto entity = testContext.init(InstanceGroupTestDto.class);
        return entity
                .withRecoveryMode(entity.getRecoveryModeParam(hostGroupType))
                .withNodeCount(nodeCount)
                .withGroup(hostGroupType.getName())
                .withSecurityGroup(testContext.given(SecurityGroupTestDto.class))
                .withType(hostGroupType.getInstanceGroupType())
                .withName(hostGroupType.getName())
                .withTemplate(testContext.given(InstanceTemplateV4TestDto.class));
    }

    public InstanceGroupTestDto withNodeCount(int nodeCount) {
        getRequest().setNodeCount(nodeCount);
        return this;
    }

    public InstanceGroupTestDto withRecipes(String... recipeNames) {
        for (String recipeName : recipeNames) {
            getRequest().getRecipeNames().add(recipeName);
        }
        return this;
    }

    public InstanceGroupTestDto withGroup(String group) {
        getRequest().setName(group);
        return this;
    }

    public InstanceGroupTestDto withType(InstanceGroupType instanceGroupType) {
        getRequest().setType(instanceGroupType);
        return this;
    }

    public InstanceGroupTestDto withSecurityGroup(String key) {
        SecurityGroupTestDto securityGroupEntity = getTestContext().get(key);
        return withSecurityGroup(securityGroupEntity);
    }

    public InstanceGroupTestDto withSecurityGroup(SecurityGroupTestDto securityGroup) {
        getRequest().setSecurityGroup(securityGroup.getRequest());
        return this;
    }

    public InstanceGroupTestDto withTemplate(String key) {
        InstanceTemplateV4TestDto template = getTestContext().get(key);
        if (template == null) {
            throw new IllegalArgumentException("Instance template does not exist for key: " + key + " in test context!");
        }
        return withTemplate(template);
    }

    public InstanceGroupTestDto withTemplate(InstanceTemplateV4TestDto template) {
        getRequest().setTemplate(template.getRequest());
        return this;
    }

    public InstanceGroupTestDto withRecoveryMode(RecoveryMode recoveryMode) {
        getRequest().setRecoveryMode(recoveryMode);
        return this;
    }

    public InstanceGroupTestDto withName(String name) {
        getRequest().setName(name);
        return this;
    }

    @Override
    public String getName() {
        return getRequest().getName();
    }

    private RecoveryMode getRecoveryModeParam(HostGroupType hostGroupType) {
        String argumentName = String.join("", hostGroupType.getName(), RECOVERY_MODE);
        String argumentValue = getTestParameter().getWithDefault(argumentName, MANUAL);
        return argumentValue.equals(AUTO) ? RecoveryMode.AUTO : RecoveryMode.MANUAL;
    }
}