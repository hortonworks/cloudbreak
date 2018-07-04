package com.sequenceiq.it.cloudbreak.newway;

import static com.sequenceiq.it.cloudbreak.CloudbreakUtil.waitAndCheckClusterStatus;
import static com.sequenceiq.it.cloudbreak.CloudbreakUtil.waitAndCheckStackStatus;
import static com.sequenceiq.it.cloudbreak.CloudbreakUtil.waitAndExpectClusterFailure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;

import com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceGroupResponse;
import com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceMetaDataJson;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.CloudbreakUtil;
import com.sequenceiq.it.cloudbreak.SshService;

public class Stack extends StackEntity {
    private static final Logger LOGGER = LoggerFactory.getLogger(Stack.class);

    public static Function<IntegrationTestContext, Stack> getTestContextStack(String key) {
        return testContext -> testContext.getContextParam(key, Stack.class);
    }

    static Function<IntegrationTestContext, Stack> getTestContextStack() {
        return getTestContextStack(StackEntity.STACK);
    }

    static Function<IntegrationTestContext, Stack> getNewStack() {
        return testContext -> new Stack();
    }

    public static Stack request() {
        return new Stack();
    }

    public static Stack isCreated() {
        Stack stack = new Stack();
        stack.setCreationStrategy(StackAction::createInGiven);
        return stack;
    }

    public static Action<Stack> post(String key) {
        return new Action<>(getTestContextStack(key), new StackPostStrategy());
    }

    public static Action<Stack> post() {
        return post(STACK);
    }

    public static Action<Stack> get(String key) {
        return new Action<>(getTestContextStack(key), StackAction::get);
    }

    public static Action<Stack> get() {
        return get(STACK);
    }

    public static Action<Stack> getAll() {
        return new Action<>(getNewStack(), StackAction::getAll);
    }

    public static Action<Stack> delete(String key) {
        return new Action<>(getTestContextStack(key), StackAction::delete);
    }

    public static Action<Stack> delete() {
        return delete(STACK);
    }

    public static Assertion<Stack> assertThis(BiConsumer<Stack, IntegrationTestContext> check) {
        return new Assertion<>(getTestContextStack(GherkinTest.RESULT), check);
    }

    public static Assertion<Stack> waitAndCheckClusterAndStackAvailabilityStatus() {
        return assertThis((stack, t) -> {
            CloudbreakClient client = CloudbreakClient.getTestContextCloudbreakClient().apply(t);
            Assert.assertNotNull(stack.getResponse().getId());
            waitAndCheckStackStatus(client.getCloudbreakClient(), stack.getResponse().getId().toString(), "AVAILABLE");
            waitAndCheckClusterStatus(client.getCloudbreakClient(), stack.getResponse().getId().toString(), "AVAILABLE");
            waitAndCheckStackStatus(client.getCloudbreakClient(), stack.getResponse().getId().toString(), "AVAILABLE");
        });
    }

    public static Assertion<Stack> waitAndCheckClusterFailure(String keyword) {
        return assertThis((stack, t) -> {
            CloudbreakClient client = CloudbreakClient.getTestContextCloudbreakClient().apply(t);
            Assert.assertNotNull(stack.getResponse().getId());
            waitAndCheckStackStatus(client.getCloudbreakClient(), stack.getResponse().getId().toString(), "AVAILABLE");
            waitAndExpectClusterFailure(client.getCloudbreakClient(), stack.getResponse().getId().toString(), "CREATE_FAILED", keyword);
        });
    }

    public static Assertion<Stack> waitAndCheckClusterAndStackStoppedStatus() {
        return assertThis((stack, t) -> {
            CloudbreakClient client = CloudbreakClient.getTestContextCloudbreakClient().apply(t);
            Assert.assertNotNull(stack.getResponse().getId());
            waitAndCheckStackStatus(client.getCloudbreakClient(), stack.getResponse().getId().toString(), "STOPPED");
            waitAndCheckClusterStatus(client.getCloudbreakClient(), stack.getResponse().getId().toString(), "STOPPED");
            waitAndCheckStackStatus(client.getCloudbreakClient(), stack.getResponse().getId().toString(), "STOPPED");
        });
    }

    public static Assertion<Stack> waitAndCheckClusterDeleted() {
        return assertThis((stack, t) -> {
            CloudbreakClient client = CloudbreakClient.getTestContextCloudbreakClient().apply(t);
            Assert.assertNotNull(stack.getResponse().getId());
            waitAndCheckStackStatus(client.getCloudbreakClient(), stack.getResponse().getId().toString(), "DELETE_COMPLETED");
        });
    }

    public static Assertion<?> checkClusterHasAmbariRunning(String ambariPort, String ambariUser, String ambariPassword) {
        return assertThis((stack, t) -> {
            CloudbreakClient client = CloudbreakClient.getTestContextCloudbreakClient().apply(t);
            CloudbreakUtil.checkClusterAvailability(client.getCloudbreakClient().stackV2Endpoint(),
                    ambariPort,
                    stack.getResponse().getId().toString(),
                    ambariUser,
                    ambariPassword,
                    true);
        });
    }

    public static Assertion<Stack> checkClusterHasAmbariRunningThroughKnox(String ambariUser, String ambariPassword) {
        return assertThis((stack, context) -> {
            CloudbreakClient client = CloudbreakClient.getTestContextCloudbreakClient().apply(context);
            CloudbreakUtil.checkClusterAvailabilityThroughGateway(client.getCloudbreakClient().stackV2Endpoint(),
                    stack.getResponse().getId().toString(),
                    ambariUser,
                    ambariPassword);
        });
    }

    public static Assertion<Stack> checkRecipes(String [] searchOnHost, String[] files, String privateKey, String sshCommand, Integer require) {
        return assertThis((stack, t) -> {
            List<String> ips = new ArrayList<>();
            List<String> emptyList = new ArrayList<>();
            Map<String, List<String>> sshCheckMap = new HashMap<>();
            sshCheckMap.put("beginsWith", emptyList);
            List<InstanceGroupResponse> instanceGroups = stack.getResponse().getInstanceGroups();
            for (InstanceGroupResponse instanceGroup : instanceGroups) {
                if (Arrays.asList(searchOnHost).contains(instanceGroup.getGroup())) {
                    for (InstanceMetaDataJson metaData : instanceGroup.getMetadata()) {
                        ips.add(metaData.getPublicIp());
                    }
                }
            }
            try {
                SshService sshService = new SshService();
                sshService.executeCommand(ips, files, privateKey, sshCommand, "cloudbreak", 120000, require, sshCheckMap);
            } catch (Exception e) {
                LOGGER.error("Error occurred during ssh execution: " + e);
            }
        });
    }
}