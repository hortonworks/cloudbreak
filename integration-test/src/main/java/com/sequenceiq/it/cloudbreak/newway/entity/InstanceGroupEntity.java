package com.sequenceiq.it.cloudbreak.newway.entity;

import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.HostGroupModelDescription.RECOVERY_MODE;
import static com.sequenceiq.it.cloudbreak.newway.cloud.HostGroupType.COMPUTE;
import static com.sequenceiq.it.cloudbreak.newway.cloud.HostGroupType.MASTER;
import static com.sequenceiq.it.cloudbreak.newway.cloud.HostGroupType.WORKER;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceGroupType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.RecoveryMode;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.it.cloudbreak.newway.Prototype;
import com.sequenceiq.it.cloudbreak.newway.cloud.HostGroupType;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

@Prototype
public class InstanceGroupEntity extends AbstractCloudbreakEntity<InstanceGroupV4Request, InstanceGroupV4Response, InstanceGroupEntity> {

    private static final String AUTO = "auto";

    private static final String MANUAL = "manual";

    protected InstanceGroupEntity(InstanceGroupV4Request request, TestContext testContext) {
        super(request, testContext);
    }

    protected InstanceGroupEntity(TestContext testContext) {
        super(new InstanceGroupV4Request(), testContext);
    }

    public InstanceGroupEntity() {
        super(InstanceGroupEntity.class.getSimpleName().toUpperCase());
    }

    public InstanceGroupEntity valid() {
        return withHostGroup(MASTER);
    }

    public InstanceGroupEntity withHostGroup(HostGroupType hostGroupType) {
        return withRecoveryMode(getRecoveryModeParam(hostGroupType))
                .withNodeCount(hostGroupType.determineInstanceCount(getTestParameter()))
                .withGroup(hostGroupType.getName())
                .withSecurityGroup(getTestContext().init(SecurityGroupEntity.class))
                .withType(hostGroupType.getInstanceGroupType())
                .withName(hostGroupType.getName().toLowerCase())
                .withTemplate(getTestContext().given(InstanceTemplateV4Entity.class));
    }

    public static InstanceGroupEntity hostGroup(TestContext testContext, HostGroupType hostGroupType) {
        return create(testContext, hostGroupType);
    }

    public static List<InstanceGroupEntity> defaultHostGroup(TestContext testContext) {
        return withHostGroup(testContext, MASTER, COMPUTE, WORKER);
    }

    public static List<InstanceGroupEntity> withHostGroup(TestContext testContext, HostGroupType... groupTypes) {
        return Stream.of(groupTypes)
                .map(groupType -> create(testContext, groupType))
                .collect(Collectors.toList());
    }

    private static InstanceGroupEntity create(TestContext testContext, HostGroupType hostGroupType) {
        InstanceGroupEntity entity = testContext.init(InstanceGroupEntity.class);
        return entity
                .withRecoveryMode(entity.getRecoveryModeParam(hostGroupType))
                .withNodeCount(hostGroupType.determineInstanceCount(entity.getTestParameter()))
                .withGroup(hostGroupType.getName())
                .withSecurityGroup(testContext.init(SecurityGroupEntity.class))
                .withType(hostGroupType.getInstanceGroupType())
                .withName(hostGroupType.getName().toLowerCase())
                .withTemplate(testContext.given(InstanceTemplateV4Entity.class));
    }

    public InstanceGroupEntity withNodeCount(int nodeCount) {
        getRequest().setNodeCount(nodeCount);
        return this;
    }

    public InstanceGroupEntity withRecipes(String... recipeNames) {
        for (String recipeName : recipeNames) {
            getRequest().getRecipeNames().add(recipeName);
        }
        return this;
    }

    public InstanceGroupEntity withGroup(String group) {
        getRequest().setName(group);
        return this;
    }

    public InstanceGroupEntity withType(InstanceGroupType instanceGroupType) {
        getRequest().setType(instanceGroupType);
        return this;
    }

    public InstanceGroupEntity withSecurityGroup(String key) {
        SecurityGroupEntity securityGroupEntity = getTestContext().get(key);
        return withSecurityGroup(securityGroupEntity);
    }

    public InstanceGroupEntity withSecurityGroup(SecurityGroupEntity securityGroup) {
        getRequest().setSecurityGroup(securityGroup.getRequest());
        return this;
    }

    public InstanceGroupEntity withTemplate(InstanceTemplateV4Entity template) {
        getRequest().setTemplate(template.getRequest());
        return this;
    }

    public InstanceGroupEntity withRecoveryMode(RecoveryMode recoveryMode) {
        getRequest().setRecoveryMode(recoveryMode);
        return this;
    }

    public InstanceGroupEntity withName(String name) {
        getRequest().setName(name);
        return this;
    }

    private RecoveryMode getRecoveryModeParam(HostGroupType hostGroupType) {
        String argumentName = String.join("", hostGroupType.getName(), RECOVERY_MODE);
        String argumentValue = getTestParameter().getWithDefault(argumentName, MANUAL);
        return argumentValue.equals(AUTO) ? RecoveryMode.AUTO : RecoveryMode.MANUAL;
    }
}