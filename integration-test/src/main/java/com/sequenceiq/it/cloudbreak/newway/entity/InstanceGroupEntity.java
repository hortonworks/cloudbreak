package com.sequenceiq.it.cloudbreak.newway.entity;

import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.HostGroupModelDescription.RECOVERY_MODE;
import static com.sequenceiq.it.cloudbreak.newway.cloud.HostGroupType.COMPUTE;
import static com.sequenceiq.it.cloudbreak.newway.cloud.HostGroupType.MASTER;
import static com.sequenceiq.it.cloudbreak.newway.cloud.HostGroupType.WORKER;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.sequenceiq.cloudbreak.api.model.RecoveryMode;
import com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceGroupResponse;
import com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceGroupType;
import com.sequenceiq.cloudbreak.api.model.v2.InstanceGroupV2Request;
import com.sequenceiq.it.cloudbreak.newway.AbstractCloudbreakEntity;
import com.sequenceiq.it.cloudbreak.newway.Prototype;
import com.sequenceiq.it.cloudbreak.newway.cloud.HostGroupType;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

@Prototype
public class InstanceGroupEntity extends AbstractCloudbreakEntity<InstanceGroupV2Request, InstanceGroupResponse, InstanceGroupEntity> {

    private static final String AUTO = "auto";

    private static final String MANUAL = "manual";

    protected InstanceGroupEntity(InstanceGroupV2Request request, TestContext testContext) {
        super(request, testContext);
    }

    protected InstanceGroupEntity(TestContext testContext) {
        super(new InstanceGroupV2Request(), testContext);
    }

    public InstanceGroupEntity valid() {
        HostGroupType hostGroupType = MASTER;
        return withHostGroup(hostGroupType);
    }

    public InstanceGroupEntity withHostGroup(HostGroupType hostGroupType) {
        return withRecoveryMode(getRecoveryModeParam(hostGroupType))
                .withNodeCount(hostGroupType.determineInstanceCount(getTestParameter()))
                .withGroup(hostGroupType.getName())
                .withSecurityGroup(getTestContext().init(SecurityGroupEntity.class))
                .withType(hostGroupType.getInstanceGroupType())
                .withTemplate(getCloudProvider().template(getTestContext()));
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
                .withTemplate(entity.getCloudProvider().template(testContext));
    }

    public InstanceGroupEntity withNodeCount(int nodeCount) {
        getRequest().setNodeCount(nodeCount);
        return this;
    }

    public InstanceGroupEntity withGroup(String group) {
        getRequest().setGroup(group);
        return this;
    }

    public InstanceGroupEntity withType(InstanceGroupType instanceGroupType) {
        getRequest().setType(instanceGroupType);
        return this;
    }

    public InstanceGroupEntity withSecurityGroup(SecurityGroupEntity securityGroup) {
        getRequest().setSecurityGroup(securityGroup.getRequest());
        return this;
    }

    public InstanceGroupEntity withTemplate(TemplateEntity template) {
        getRequest().setTemplate(template.getRequest());
        return this;
    }

    public InstanceGroupEntity withRecoveryMode(RecoveryMode recoveryMode) {
        getRequest().setRecoveryMode(recoveryMode);
        return this;
    }

    private RecoveryMode getRecoveryModeParam(HostGroupType hostGroupType) {
        String argumentName = String.join("", hostGroupType.getName(), RECOVERY_MODE);
        String argumentValue = getTestParameter().getWithDefault(argumentName, MANUAL);
        if (argumentValue.equals(AUTO)) {
            return RecoveryMode.AUTO;
        } else {
            return RecoveryMode.MANUAL;
        }
    }
}
