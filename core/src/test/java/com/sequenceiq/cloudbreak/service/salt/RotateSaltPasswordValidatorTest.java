package com.sequenceiq.cloudbreak.service.salt;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.core.bootstrap.service.SaltBootstrapVersionChecker;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;

@ExtendWith(MockitoExtension.class)
class RotateSaltPasswordValidatorTest {

    private static final String STACK_CRN = "crn:cdp:datalake:us-west-1:cloudera:datalake:33071a14-d605-4b2d-9a55-218c0dbc95e3";

    private static final String ACCOUNT_ID = "0";

    private static final String OLD_PASSWORD = "old-password";

    @Mock
    private SaltBootstrapVersionChecker saltBootstrapVersionChecker;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private StackDto stack;

    @Mock
    private InstanceMetadataView instanceMetadataView;

    @InjectMocks
    private RotateSaltPasswordValidator underTest;

    @BeforeEach
    void setUp() {
        lenient().when(stack.isAvailable()).thenReturn(true);
        lenient().when(stack.getAccountId()).thenReturn(ACCOUNT_ID);
        lenient().when(stack.getStatus()).thenReturn(Status.AVAILABLE);
        lenient().when(stack.getNotTerminatedAndNotZombieGatewayInstanceMetadata()).thenReturn(List.of(instanceMetadataView));
    }

    @Test
    public void testRotateSaltPasswordOnStackWithNotRunningInstanceAndFallbackImplementation() {
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setInstanceStatus(InstanceStatus.STOPPED);
        when(stack.getNotTerminatedInstanceMetaData()).thenReturn(List.of(instanceMetaData));

        Assertions.assertThatThrownBy(() -> underTest.validateRotateSaltPassword(stack))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Rotating SaltStack user password is only supported when all instances are running");
    }

    @Test
    public void testRotateSaltPasswordOnStackWithoutAvailableGateway() {
        when(stack.getNotTerminatedAndNotZombieGatewayInstanceMetadata()).thenReturn(List.of());

        Assertions.assertThatThrownBy(() -> underTest.validateRotateSaltPassword(stack))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Rotating SaltStack user password is not supported when there are no available gateway instances");
    }
}