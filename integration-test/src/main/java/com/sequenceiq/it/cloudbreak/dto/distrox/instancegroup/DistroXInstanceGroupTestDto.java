package com.sequenceiq.it.cloudbreak.dto.distrox.instancegroup;

import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.BROKER;
import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.COMPUTE;
import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.CONNECT;
import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.COORDINATOR;
import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.CORE_BROKER;
import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.CORE_ZOOKEEPER;
import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.EXECUTOR;
import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.GATEWAY;
import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.KRAFT;
import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.MANAGER;
import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.MASTER;
import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.MASTER_STREAMS;
import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.SRM;
import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.WORKER;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.CollectionUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.RecoveryMode;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.InstanceGroupV1Request;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.cloud.HostGroupType;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractCloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.dto.SubnetId;

@Prototype
public class DistroXInstanceGroupTestDto extends AbstractCloudbreakTestDto<InstanceGroupV1Request, InstanceGroupV4Response, DistroXInstanceGroupTestDto> {

    private static final String AUTO = "auto";

    private static final String MANUAL = "manual";

    public DistroXInstanceGroupTestDto(TestContext testContext) {
        super(new InstanceGroupV1Request(), testContext);
    }

    public static List<DistroXInstanceGroupTestDto> dataEngHostGroups(TestContext testContext, CloudPlatform cloudPlatform) {
        return withHostGroup(testContext, cloudPlatform, MASTER, COMPUTE, WORKER, GATEWAY);
    }

    public static List<DistroXInstanceGroupTestDto> dataMartHostGroups(TestContext testContext, CloudPlatform cloudPlatform) {
        return withHostGroup(testContext, cloudPlatform, MASTER, COORDINATOR, EXECUTOR);
    }

    public static List<DistroXInstanceGroupTestDto> streamsHAHostGroups(TestContext testContext, CloudPlatform cloudPlatform) {
        return withHostGroup(testContext, cloudPlatform, MANAGER, MASTER_STREAMS, CORE_ZOOKEEPER, CORE_BROKER, BROKER, SRM, CONNECT, KRAFT);
    }

    public static List<DistroXInstanceGroupTestDto> dataEngHostGroups(TestContext testContext) {
        return withHostGroup(testContext, testContext.getCloudPlatform(), MASTER, COMPUTE, WORKER, GATEWAY);
    }

    public static List<DistroXInstanceGroupTestDto> withHostGroup(TestContext testContext, HostGroupType... groupTypes) {
        return withHostGroup(testContext, testContext.getCloudPlatform(), groupTypes);
    }

    public static List<DistroXInstanceGroupTestDto> withHostGroup(TestContext testContext, CloudPlatform cloudPlatform, HostGroupType... groupTypes) {
        return Stream.of(groupTypes)
                .map(groupType -> create(testContext, cloudPlatform, groupType))
                .collect(Collectors.toList());
    }

    private static DistroXInstanceGroupTestDto create(TestContext testContext, CloudPlatform cloudPlatform, HostGroupType hostGroupType) {
        return create(testContext, cloudPlatform, hostGroupType, hostGroupType.determineInstanceCount());
    }

    private static DistroXInstanceGroupTestDto create(TestContext testContext, CloudPlatform cloudPlatform, HostGroupType hostGroupType, int nodeCount) {
        DistroXInstanceGroupTestDto entity = testContext.init(DistroXInstanceGroupTestDto.class);
        return entity
                .withNodeCount(nodeCount)
                .withGroup(hostGroupType.getName())
                .withType(hostGroupType.getInstanceGroupType())
                .withName(hostGroupType.getName())
                .withNetwork(SubnetId.all())
                .withRecipes(entity.getRequest().getRecipeNames())
                .withTemplate(testContext.given(DistroXInstanceTemplateTestDto.class, cloudPlatform));
    }

    public DistroXInstanceGroupTestDto valid() {
        return withHostGroup(MASTER);
    }

    public DistroXInstanceGroupTestDto withHostGroup(HostGroupType hostGroupType) {
        DistroXInstanceTemplateTestDto template = getTestContext()
                .given("DistroxInstanceGroupTestDto" + hostGroupType.getName(), DistroXInstanceTemplateTestDto.class, getCloudPlatform());
        return withRecoveryMode(RecoveryMode.MANUAL)
                .withNodeCount(hostGroupType.determineInstanceCount())
                .withGroup(hostGroupType.getName())
                .withType(hostGroupType.getInstanceGroupType())
                .withName(hostGroupType.getName())
                .withTemplate(template);
    }

    public DistroXInstanceGroupTestDto withNodeCount(int nodeCount) {
        getRequest().setNodeCount(nodeCount);
        return this;
    }

    public DistroXInstanceGroupTestDto withRecipes(String... recipeNames) {
        Set<String> recipes = new HashSet<String>();
        for (String recipeName : recipeNames) {
            recipes = getRequest().getRecipeNames();
            recipes.add(recipeName);
        }
        getRequest().setRecipeNames(recipes);
        return this;
    }

    public DistroXInstanceGroupTestDto withRecipes(Set<String> recipeNames) {
        if (CollectionUtils.isNotEmpty(recipeNames)) {
            Set<String> recipes = getRequest().getRecipeNames();
            if (CollectionUtils.isNotEmpty(recipes)) {
                recipes.addAll(recipeNames);
                getRequest().setRecipeNames(recipes);
            } else {
                getRequest().setRecipeNames(recipeNames);
            }
        }
        return this;
    }

    public DistroXInstanceGroupTestDto withGroup(String group) {
        getRequest().setName(group);
        return this;
    }

    public DistroXInstanceGroupTestDto withNetwork(SubnetId subnetId) {
        getRequest().setNetwork(getCloudProvider().instanceGroupNetworkV1Request(subnetId));
        return this;
    }

    public DistroXInstanceGroupTestDto withType(InstanceGroupType instanceGroupType) {
        getRequest().setType(instanceGroupType);
        return this;
    }

    public DistroXInstanceGroupTestDto withTemplate(String key) {
        return withTemplate((DistroXInstanceTemplateTestDto) getTestContext().get(key));
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
}