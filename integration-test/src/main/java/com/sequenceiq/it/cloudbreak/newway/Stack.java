package com.sequenceiq.it.cloudbreak.newway;

import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.CloudbreakUtil;
import org.testng.Assert;

import java.util.function.BiConsumer;
import java.util.function.Function;

import static com.sequenceiq.it.cloudbreak.CloudbreakUtil.waitAndCheckClusterStatus;
import static com.sequenceiq.it.cloudbreak.CloudbreakUtil.waitAndCheckStackStatus;

public class Stack extends StackEntity {

    static Function<IntegrationTestContext, Stack> getTestContextStack(String key) {
        return (testContext) -> testContext.getContextParam(key, Stack.class);
    }

    static Function<IntegrationTestContext, Stack> getTestContextStack() {
        return getTestContextStack(StackEntity.STACK);
    }

    static Function<IntegrationTestContext, Stack> getNewStack() {
        return (testContext) -> new Stack();
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
}
