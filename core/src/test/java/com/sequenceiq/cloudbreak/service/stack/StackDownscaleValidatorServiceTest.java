package com.sequenceiq.cloudbreak.service.stack;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceMetadataType.CORE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceMetadataType.GATEWAY;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceMetadataType.GATEWAY_PRIMARY;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.access.AccessDeniedException;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;

@RunWith(MockitoJUnitRunner.class)
public class StackDownscaleValidatorServiceTest {

    private static final Long STACK_ID = 1L;

    private static final String INSTANCE_PUBLIC_IP = "2.2.2.2";

    private static final String CLUSTER_MANAGER_HOST_EXCEPTION_MESSAGE_WITHOUT_IP = "Downscale for the given node is prohibited because it " +
            "serves as a host the Cluster Manager server";

    private static final String CLUSTER_MANAGER_SERVER_HOST_EXCEPTION_MESSAGE = String.format("Downscale for the given node [public IP: %s] is prohibited " +
            "because it serves as a host the Cluster Manager server", INSTANCE_PUBLIC_IP);

    private static final String ACCESS_DENIED_EXCEPTION_MESSAGE = String.format("Private stack (%s) is only modifiable by the owner.", STACK_ID);

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private StackDownscaleValidatorService underTest;

    @Test
    public void testCheckInstanceIsTheAmbariServerOrNotMethodWhenInstanceIsCoreTypeThenNoExceptionWouldInvoke() {
        underTest.checkInstanceIsTheClusterManagerServerOrNot(INSTANCE_PUBLIC_IP, CORE);
    }

    @Test
    public void testCheckInstanceIsTheAmbariServerOrNotMethodWhenTypeIsGatewayThenException() {
        expectedException.expect(BadRequestException.class);
        expectedException.expectMessage(CLUSTER_MANAGER_SERVER_HOST_EXCEPTION_MESSAGE);

        underTest.checkInstanceIsTheClusterManagerServerOrNot(INSTANCE_PUBLIC_IP, GATEWAY);
    }

    @Test
    public void testCheckInstanceIsTheAmbariServerOrNotMethodWhenTypeIsGatewayPrimaryThenException() {
        expectedException.expect(BadRequestException.class);
        expectedException.expectMessage(CLUSTER_MANAGER_SERVER_HOST_EXCEPTION_MESSAGE);

        underTest.checkInstanceIsTheClusterManagerServerOrNot(INSTANCE_PUBLIC_IP, GATEWAY_PRIMARY);
    }

    @Test
    public void testCheckUserHasRightToTerminateInstanceMethodWhenOwnerAndUserIdIsSameThenNoException() {
        underTest.checkUserHasRightToTerminateInstance("same", "same", STACK_ID);
    }

    @Test
    public void testCheckUserHasRightToTerminateInstanceMethodWhenUserIdAndOwnerAreNotTheSameThenExceptionWouldInvoke() {
        expectedException.expect(AccessDeniedException.class);
        expectedException.expectMessage(ACCESS_DENIED_EXCEPTION_MESSAGE);

        underTest.checkUserHasRightToTerminateInstance("something here", "something else here", STACK_ID);
    }

    @Test
    public void testCheckClusterInValidStatusWhenValidStatus() {
        Stack stack = new Stack();
        stack.setStackStatus(new StackStatus<>(stack, DetailedStackStatus.AVAILABLE));

        underTest.checkClusterInValidStatus(stack);
    }

    @Test
    public void testCheckClusterInValidStatusWhenInStoppedStatus() {
        Stack stack = new Stack();
        stack.setStackStatus(new StackStatus<>(stack, DetailedStackStatus.STOPPED));

        expectedException.expect(BadRequestException.class);
        expectedException.expectMessage("Cluster is in Stopped status. Please start the cluster for downscale.");

        underTest.checkClusterInValidStatus(stack);
    }

    @Test
    public void testCheckInstanceIsTheAmbariServerOrNotMethodWhenTypeIsGatewayPrimaryAndNullPublicIpProvidedThenExceptionWithoutIp() {
        expectedException.expect(BadRequestException.class);
        expectedException.expectMessage(CLUSTER_MANAGER_HOST_EXCEPTION_MESSAGE_WITHOUT_IP);

        underTest.checkInstanceIsTheClusterManagerServerOrNot(null, GATEWAY_PRIMARY);
    }

    @Test
    public void testCheckInstanceIsTheAmbariServerOrNotMethodWhenTypeIsGatewayPrimaryAndEmptyPublicIpProvidedThenExceptionWithoutIp() {
        expectedException.expect(BadRequestException.class);
        expectedException.expectMessage(CLUSTER_MANAGER_HOST_EXCEPTION_MESSAGE_WITHOUT_IP);

        underTest.checkInstanceIsTheClusterManagerServerOrNot("", GATEWAY_PRIMARY);
    }

    @Test
    public void testCheckInstanceIsTheAmbariServerOrNotMethodWhenTypeIsGatewayAndNullPublicIpProvidedThenExceptionWithoutIp() {
        expectedException.expect(BadRequestException.class);
        expectedException.expectMessage(CLUSTER_MANAGER_HOST_EXCEPTION_MESSAGE_WITHOUT_IP);

        underTest.checkInstanceIsTheClusterManagerServerOrNot(null, GATEWAY);
    }

    @Test
    public void testCheckInstanceIsTheAmbariServerOrNotMethodWhenTypeIsGatewayAndEmptyPublicIpProvidedThenExceptionWithoutIp() {
        expectedException.expect(BadRequestException.class);
        expectedException.expectMessage(CLUSTER_MANAGER_HOST_EXCEPTION_MESSAGE_WITHOUT_IP);

        underTest.checkInstanceIsTheClusterManagerServerOrNot("", GATEWAY);
    }

}
