package com.sequenceiq.it.cloudbreak.newway;

import static com.sequenceiq.it.cloudbreak.newway.v3.CloudbreakV3Util.waitAndCheckClusterStatus;
import static com.sequenceiq.it.cloudbreak.newway.v3.CloudbreakV3Util.waitAndCheckStackStatus;
import static com.sequenceiq.it.cloudbreak.newway.v3.CloudbreakV3Util.waitAndExpectClusterFailure;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.hardware.HardwareInfoGroupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.hardware.HardwareInfoV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image.StackImageV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.SshService;
import com.sequenceiq.it.cloudbreak.SshUtil;
import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.action.stack.StackTestAction;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.newway.v3.CloudbreakV3Util;
import com.sequenceiq.it.cloudbreak.newway.v3.StackPostV3Strategy;
import com.sequenceiq.it.cloudbreak.newway.v3.StackActionV4;

public class Stack extends StackTestDto {

    private static final Logger LOGGER = LoggerFactory.getLogger(Stack.class);

    Stack() {
    }

    public Stack(TestContext testContext) {
        super(testContext);
    }

    public static Function<IntegrationTestContext, Stack> getTestContextStack(String key) {
        return testContext -> testContext.getContextParam(key, Stack.class);
    }

    public static Function<IntegrationTestContext, Stack> getTestContextStack() {
        return getTestContextStack(StackTestDto.STACK);
    }

    static Function<IntegrationTestContext, Stack> getNewStack() {
        return testContext -> new Stack();
    }

    public static Stack request() {
        return new Stack();
    }

    public static Stack created() {
        Stack stack = new Stack();
        stack.setCreationStrategy(StackActionV4::createInGiven);
        return stack;
    }

    public static Action<StackTestDto> postV4() {
        return StackTestAction::create;
    }

    public static ResourceAction<Stack> postV3(String key) {
        return new ResourceAction<>(getTestContextStack(key), new StackPostV3Strategy());
    }

    public static ResourceAction<Stack> post(Strategy strategy) {
        return new ResourceAction<>(getTestContextStack(STACK), strategy);
    }

    public static ResourceAction<Stack> postV3() {
        return postV3(STACK);
    }

    public static ResourceAction<Stack> get(String key) {
        return new ResourceAction<>(getTestContextStack(key), StackActionV4::get);
    }

    public static ResourceAction<Stack> get(Strategy strategy) {
        return new ResourceAction<>(getTestContextStack(STACK), strategy);
    }

    public static ResourceAction<Stack> get() {
        return get(STACK);
    }

    public static ResourceAction<Stack> getAll() {
        return new ResourceAction<>(getNewStack(), StackActionV4::getAll);
    }

    public static ResourceAction<Stack> delete(String key, Strategy strategy) {
        return new ResourceAction<>(getTestContextStack(key), strategy);
    }

    public static Action<StackTestDto> deleteV4() {
        return StackTestAction::delete;
    }

    public static ResourceAction<Stack> delete(String key) {
        return delete(key, StackActionV4::delete);
    }

    public static ResourceAction<Stack> delete() {
        return delete(STACK);
    }

    public static ResourceAction<Stack> delete(Strategy strategy) {
        return delete(STACK, strategy);
    }

    public static ResourceAction<Stack> makeNodeUnhealthy(String hostgroup, int nodeCount) {
        return new ResourceAction<>(getTestContextStack(STACK), new UnhealthyNodeStrategy(hostgroup, nodeCount));
    }

    public static Assertion<Stack> assertThis(BiConsumer<Stack, IntegrationTestContext> check) {
        return new Assertion<>(getTestContextStack(GherkinTest.RESULT), check);
    }

    public static Assertion<Stack> waitAndCheckClusterAndStackAvailabilityStatus() {
        return assertThis((stack, t) -> {
            CloudbreakClient client = CloudbreakClient.getTestContextCloudbreakClient().apply(t);
            StackV4Response stackResponse = stack.getResponse();
            String stackName = stackResponse.getName();
            Long workspaceId = stackResponse.getWorkspace().getId();
            Assert.assertNotNull(stackResponse.getName());
            waitAndCheckStackStatus(client.getCloudbreakClient(), workspaceId, stackName, Status.AVAILABLE);
            waitAndCheckClusterStatus(client.getCloudbreakClient(), workspaceId, stackName, Status.AVAILABLE);
            waitAndCheckStackStatus(client.getCloudbreakClient(), workspaceId, stackName, Status.AVAILABLE);
        });
    }

    public static Assertion<Stack> waitAndCheckClusterIsAvailable() {
        return assertThis((stack, t) -> {
            CloudbreakClient client = CloudbreakClient.getTestContextCloudbreakClient().apply(t);
            StackV4Response stackResponse = stack.getResponse();
            String stackName = stackResponse.getName();
            Long workspaceId = stackResponse.getWorkspace().getId();
            Assert.assertNotNull(stackResponse.getName());
            waitAndCheckClusterStatus(client.getCloudbreakClient(), workspaceId, stackName, Status.AVAILABLE);
        });
    }

    public static Assertion<Stack> waitAndCheckClusterFailure(String keyword) {
        return assertThis((stack, t) -> {
            CloudbreakClient client = CloudbreakClient.getTestContextCloudbreakClient().apply(t);
            StackV4Response stackResponse = stack.getResponse();
            String stackName = stackResponse.getName();
            Long workspaceId = stackResponse.getWorkspace().getId();
            Assert.assertNotNull(stackResponse.getName());
            waitAndCheckStackStatus(client.getCloudbreakClient(), workspaceId, stackName, Status.AVAILABLE);
            waitAndExpectClusterFailure(client.getCloudbreakClient(), workspaceId, stackName, Status.CREATE_FAILED, keyword);
        });
    }

    public static Assertion<Stack> waitAndCheckClusterAndStackStoppedStatus() {
        return assertThis((stack, t) -> {
            CloudbreakClient client = CloudbreakClient.getTestContextCloudbreakClient().apply(t);
            StackV4Response stackResponse = stack.getResponse();
            String stackName = stackResponse.getName();
            Long workspaceId = stackResponse.getWorkspace().getId();
            Assert.assertNotNull(stackResponse.getName());
            waitAndCheckStackStatus(client.getCloudbreakClient(), workspaceId, stackName, Status.STOPPED);
            waitAndCheckClusterStatus(client.getCloudbreakClient(), workspaceId, stackName, Status.STOPPED);
            waitAndCheckStackStatus(client.getCloudbreakClient(), workspaceId, stackName, Status.STOPPED);
        });
    }

    public static Assertion<Stack> waitAndCheckClusterDeleted() {
        return assertThis((stack, t) -> {
            CloudbreakClient client = CloudbreakClient.getTestContextCloudbreakClient().apply(t);
            StackV4Response stackResponse = stack.getResponse();
            String stackName = stackResponse.getName();
            Long workspaceId = stackResponse.getWorkspace().getId();
            Assert.assertNotNull(stackResponse.getName());
            waitAndCheckStackStatus(client.getCloudbreakClient(), workspaceId, stackName, Status.DELETE_COMPLETED);
        });
    }

    public static Assertion<?> checkClusterHasAmbariRunning(String ambariPort, String ambariUser, String ambariPassword) {
        return assertThis((stack, t) -> {
            CloudbreakClient client = CloudbreakClient.getTestContextCloudbreakClient().apply(t);
            CloudbreakV3Util.checkClusterAvailability(client.getCloudbreakClient().stackV4Endpoint(),
                    ambariPort,
                    stack.getResponse().getWorkspace().getId(),
                    stack.getResponse().getName(),
                    ambariUser,
                    ambariPassword,
                    true);
        });
    }

    public static Assertion<Stack> checkClusterHasAmbariRunningThroughKnox() {
        return assertThis((stack, context) -> {
            CloudbreakClient client = CloudbreakClient.getTestContextCloudbreakClient().apply(context);
            CloudbreakV3Util.checkClusterAvailabilityThroughGateway(client.getCloudbreakClient().stackV4Endpoint(),
                    stack.getResponse().getWorkspace().getId(),
                    stack.getResponse().getName());
        });
    }

    public static Assertion<Stack> checkRecipes(String[] searchOnHost, String[] files, String privateKey, String sshCommand, int require) {
        return checkRecipes(searchOnHost, files, privateKey, Optional.ofNullable(sshCommand), require);
    }

    public static Assertion<Stack> checkRecipes(String[] searchOnHost, String[] files, String privateKey, int require) {
        return checkRecipes(searchOnHost, files, privateKey, Optional.empty(), require);
    }

    public static Assertion<Stack> checkRecipes(String[] searchOnHost, String[] files, String privateKey, Optional<String> sshCommand, int require) {
        return assertThis((stack, t) -> {
            List<String> ips = new ArrayList<>();
            List<InstanceGroupV4Response> instanceGroups = stack.getResponse().getInstanceGroups();
            for (InstanceGroupV4Response instanceGroup : instanceGroups) {
                if (Arrays.asList(searchOnHost).contains(instanceGroup.getName())) {
                    for (InstanceMetaDataV4Response metaData : instanceGroup.getMetadata()) {
                        ips.add(metaData.getPublicIp());
                    }
                }
            }
            int quantity = 0;
            try {
                quantity = new SshService().countFilesOnHostByExtensionAndPath(ips, files, sshCommand, privateKey, "success", "cloudbreak", 120000, require);
            } catch (Exception e) {
                LOGGER.error("Error occurred during ssh execution: " + e);
            }
            assertEquals(quantity, require, "The number of existing files is different than required.");
        });
    }

    public static Assertion<Stack> checkSshCommand(String[] searchOnHost, String privateKey, String sshCommand, String sshChecker) {
        return assertThis((stack, t) -> {
            boolean sshResult = false;
            List<String> ips = new ArrayList<>();
            List<InstanceGroupV4Response> instanceGroups = stack.getResponse().getInstanceGroups();
            for (InstanceGroupV4Response instanceGroup : instanceGroups) {
                if (Arrays.asList(searchOnHost).contains(instanceGroup.getName())) {
                    for (InstanceMetaDataV4Response metaData : instanceGroup.getMetadata()) {
                        ips.add(metaData.getPublicIp());
                    }
                }
            }
            for (String ip : ips) {
                try {
                    sshResult = SshUtil.executeCommand(ip, privateKey, sshCommand, "cloudbreak", SshUtil.getSshCheckMap(sshChecker));
                } catch (Exception e) {
                    LOGGER.warn("Error occurred during ssh execution: " + e);
                }
                Assert.assertTrue(sshResult, "Ssh command executing was not successful");
            }
        });
    }

    public static Assertion<Stack> checkImage(String imageId, String imageCatalogName) {
        return assertThis((stack, t) -> {
            StackImageV4Response image = stack.getResponse().getImage();
            assertEquals(imageId, image.getId());
            if (StringUtils.isNotBlank(imageCatalogName)) {
                assertEquals(imageCatalogName, image.getCatalogName());
            }
        });
    }

    public static Assertion<Stack> checkImagesDifferent() {
        return assertThis((stack, t) -> {
            Set<String> imageIds = stack.getResponse().getHardwareInfoGroups().stream()
                    .map(HardwareInfoGroupV4Response::getHardwareInfos)
                    .map(hwInfoResponses -> hwInfoResponses.stream()
                            .map(HardwareInfoV4Response::getImageId).collect(Collectors.toSet()))
                    .flatMap(Collection::stream).collect(Collectors.toSet());
            assertTrue(imageIds.size() > 1);
            assertTrue(imageIds.contains(stack.getResponse().getImage().getId()));
        });
    }

    public static StackTestDto getByName(TestContext testContext, StackTestDto entity, CloudbreakClient cloudbreakClient) {
        entity.setResponse(
                cloudbreakClient.getCloudbreakClient().stackV4Endpoint().get(cloudbreakClient.getWorkspaceId(), entity.getRequest().getName(), new HashSet<>())
        );
        return entity;
    }

    public static ResourceAction<Stack> repair(String hostgroupName) {
        return new ResourceAction<>(getTestContextStack(), new RepairNodeStrategy(hostgroupName));
    }
}
