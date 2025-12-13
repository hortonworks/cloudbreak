package com.sequenceiq.cloudbreak.service.stack;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceMetadataType.CORE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceMetadataType.GATEWAY_PRIMARY;
import static org.junit.jupiter.api.Assertions.assertThrows;

import jakarta.ws.rs.ForbiddenException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;

@ExtendWith(MockitoExtension.class)
class StackDownscaleValidatorServiceTest {

    private static final Long STACK_ID = 1L;

    private static final String INSTANCE_PUBLIC_IP = "2.2.2.2";

    private static final String CLUSTER_MANAGER_HOST_EXCEPTION_MESSAGE_WITHOUT_IP = "Downscale for the given node is prohibited because it " +
            "serves as a host the Cluster Manager server";

    private static final String CLUSTER_MANAGER_SERVER_HOST_EXCEPTION_MESSAGE = String.format("Downscale for the given node [public IP: %s] is prohibited " +
            "because it serves as a host the Cluster Manager server", INSTANCE_PUBLIC_IP);

    private static final String ACCESS_DENIED_EXCEPTION_MESSAGE = String.format("Private stack (%s) is only modifiable by the owner.", STACK_ID);

    private StackDownscaleValidatorService underTest = new StackDownscaleValidatorService();

    @Test
    void testCheckInstanceIsTheAmbariServerOrNotMethodWhenInstanceIsCoreTypeThenNoExceptionWouldInvoke() {
        underTest.checkInstanceIsTheClusterManagerServerOrNot(INSTANCE_PUBLIC_IP, CORE);
    }

    @Test
    void testCheckInstanceIsTheAmbariServerOrNotMethodWhenTypeIsGatewayThenException() {
        assertThrows(BadRequestException.class, () -> underTest.checkInstanceIsTheClusterManagerServerOrNot(INSTANCE_PUBLIC_IP, GATEWAY_PRIMARY),
                CLUSTER_MANAGER_SERVER_HOST_EXCEPTION_MESSAGE);
    }

    @Test
    void testCheckInstanceIsTheAmbariServerOrNotMethodWhenTypeIsGatewayPrimaryThenException() {
        assertThrows(BadRequestException.class, () -> underTest.checkInstanceIsTheClusterManagerServerOrNot(INSTANCE_PUBLIC_IP, GATEWAY_PRIMARY),
                CLUSTER_MANAGER_SERVER_HOST_EXCEPTION_MESSAGE);
    }

    @Test
    void testCheckUserHasRightToTerminateInstanceMethodWhenOwnerAndUserIdIsSameThenNoException() {
        underTest.checkUserHasRightToTerminateInstance("same", "same", STACK_ID);
    }

    @Test
    void testCheckUserHasRightToTerminateInstanceMethodWhenUserIdAndOwnerAreNotTheSameThenExceptionWouldInvoke() {
        assertThrows(ForbiddenException.class, () -> underTest.checkUserHasRightToTerminateInstance("something here", "something else here", STACK_ID),
                ACCESS_DENIED_EXCEPTION_MESSAGE);
    }

    @Test
    void testCheckClusterInValidStatusWhenValidStatus() {
        Stack stack = new Stack();
        stack.setStackStatus(new StackStatus(stack, DetailedStackStatus.AVAILABLE));

        underTest.checkClusterInValidStatus(stack);
    }

    @Test
    void testCheckClusterInValidStatusWhenInStoppedStatus() {
        Stack stack = new Stack();
        stack.setStackStatus(new StackStatus(stack, DetailedStackStatus.STOPPED));

        assertThrows(BadRequestException.class, () -> underTest.checkClusterInValidStatus(stack),
                "Cluster is in Stopped status. Please start the cluster for downscale.");
    }

    @Test
    void testCheckInstanceIsTheAmbariServerOrNotMethodWhenTypeIsGatewayPrimaryAndNullPublicIpProvidedThenExceptionWithoutIp() {
        assertThrows(BadRequestException.class, () -> underTest.checkInstanceIsTheClusterManagerServerOrNot(null, GATEWAY_PRIMARY),
                CLUSTER_MANAGER_HOST_EXCEPTION_MESSAGE_WITHOUT_IP);
    }

    @Test
    void testCheckInstanceIsTheAmbariServerOrNotMethodWhenTypeIsGatewayPrimaryAndEmptyPublicIpProvidedThenExceptionWithoutIp() {
        assertThrows(BadRequestException.class, () -> underTest.checkInstanceIsTheClusterManagerServerOrNot("", GATEWAY_PRIMARY),
                CLUSTER_MANAGER_HOST_EXCEPTION_MESSAGE_WITHOUT_IP);
    }

    @Test
    void testCheckInstanceIsTheAmbariServerOrNotMethodWhenTypeIsGatewayAndNullPublicIpProvidedThenExceptionWithoutIp() {
        assertThrows(BadRequestException.class, () -> underTest.checkInstanceIsTheClusterManagerServerOrNot(null, GATEWAY_PRIMARY),
                CLUSTER_MANAGER_HOST_EXCEPTION_MESSAGE_WITHOUT_IP);
    }

    @Test
    void testCheckInstanceIsTheAmbariServerOrNotMethodWhenTypeIsGatewayAndEmptyPublicIpProvidedThenExceptionWithoutIp() {
        assertThrows(BadRequestException.class, () -> underTest.checkInstanceIsTheClusterManagerServerOrNot("", GATEWAY_PRIMARY),
                CLUSTER_MANAGER_HOST_EXCEPTION_MESSAGE_WITHOUT_IP);
    }

}
