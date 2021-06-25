package com.sequenceiq.freeipa.service.upgrade;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.StackStatus;

@ExtendWith(MockitoExtension.class)
class UpgradeValidationServiceTest {

    public static final String ACCOUNT_ID = "accId";

    @Mock
    private EntitlementService entitlementService;

    @InjectMocks
    private UpgradeValidationService underTest;

    @Test
    public void testStackValidationOk() {
        Stack stack = mock(Stack.class);
        when(stack.isAvailable()).thenReturn(Boolean.TRUE);
        Set<InstanceMetaData> allInstances = Set.of(createAvailableInstance("im1"), createAvailableInstance("im2"));

        underTest.validateStackForUpgrade(allInstances, stack);
    }

    @Test
    public void testNoInstances() {
        Stack stack = mock(Stack.class);
        Set<InstanceMetaData> allInstances = Set.of();

        assertThrows(BadRequestException.class, () -> underTest.validateStackForUpgrade(allInstances, stack));
    }

    @Test
    public void testMoreThanTwoInstances() {
        Stack stack = mock(Stack.class);
        Set<InstanceMetaData> allInstances = Set.of(createAvailableInstance("im1"), createAvailableInstance("im2"),  createAvailableInstance("im2"));

        assertThrows(BadRequestException.class, () -> underTest.validateStackForUpgrade(allInstances, stack));
    }

    @Test
    public void testNotAvailableInstances() {
        Stack stack = mock(Stack.class);
        InstanceMetaData im2 = createAvailableInstance("im2");
        im2.setInstanceStatus(InstanceStatus.DELETED_ON_PROVIDER_SIDE);
        Set<InstanceMetaData> allInstances = Set.of(createAvailableInstance("im1"), im2);

        assertThrows(BadRequestException.class, () -> underTest.validateStackForUpgrade(allInstances, stack));
    }

    @Test
    public void testStackNotAvailable() {
        Stack stack = mock(Stack.class);
        when(stack.isAvailable()).thenReturn(Boolean.FALSE);
        StackStatus stackStatus = new StackStatus();
        stackStatus.setStatus(Status.DELETED_ON_PROVIDER_SIDE);
        when(stack.getStackStatus()).thenReturn(stackStatus);
        Set<InstanceMetaData> allInstances = Set.of(createAvailableInstance("im1"), createAvailableInstance("im2"));

        assertThrows(BadRequestException.class, () -> underTest.validateStackForUpgrade(allInstances, stack));
    }

    public InstanceMetaData createAvailableInstance(String id) {
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setInstanceId(id);
        instanceMetaData.setInstanceStatus(InstanceStatus.CREATED);
        return instanceMetaData;
    }

    @Test
    public void testEntitlementEnabled() {
        when(entitlementService.isFreeIpaUpgradeEnabled(ACCOUNT_ID)).thenReturn(Boolean.TRUE);

        underTest.validateEntitlement(ACCOUNT_ID);
    }

    @Test
    public void testEntitlementDisabled() {
        when(entitlementService.isFreeIpaUpgradeEnabled(ACCOUNT_ID)).thenReturn(Boolean.FALSE);

        assertThrows(BadRequestException.class, () -> underTest.validateEntitlement(ACCOUNT_ID));
    }
}