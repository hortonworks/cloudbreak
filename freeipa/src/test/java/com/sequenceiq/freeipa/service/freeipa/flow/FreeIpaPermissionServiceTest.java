package com.sequenceiq.freeipa.service.freeipa.flow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.polling.Poller;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;

@ExtendWith(MockitoExtension.class)
class FreeIpaPermissionServiceTest {

    private static final String HOST_ENROLLMENT_PRIVILEGE = "Host Enrollment";

    private static final String ADD_HOSTS_PERMISSION = "System: Add Hosts";

    private static final String REMOVE_SERVICES_PERMISSION = "System: Remove Services";

    private static final String REMOVE_HOSTS_PERMISSION = "System: Remove Hosts";

    private static final String DNS_ADMINISTRATORS_PRIVILEGE = "DNS Administrators";

    private static final String ENROLLMENT_ADMINISTRATOR_ROLE = "Enrollment Administrator";

    private static final long POLLING_INTERVAL = 20L;

    private static final long POLLING_DELAY = 5L;

    @Mock
    private FreeIpaClientFactory freeIpaClientFactory;

    @Mock
    private Poller<Void> poller;

    @InjectMocks
    private FreeIpaPermissionService underTest;

    @BeforeEach
    public void init() {
        ReflectionTestUtils.setField(underTest, "pollingInterval", POLLING_INTERVAL);
        ReflectionTestUtils.setField(underTest, "pollingDelay", POLLING_DELAY);
    }

    @Test
    public void testSingleInstanceNoPolling() throws FreeIpaClientException {
        FreeIpaClient freeIpaClient = mock(FreeIpaClient.class);
        Stack stack = new Stack();
        InstanceGroup instanceGroup = new InstanceGroup();
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setInstanceStatus(InstanceStatus.CREATED);
        instanceMetaData.setDiscoveryFQDN("name1");
        instanceGroup.setInstanceMetaData(Set.of(instanceMetaData));
        stack.setInstanceGroups(Set.of(instanceGroup));

        underTest.setPermissions(stack, freeIpaClient);

        verify(freeIpaClient).addPermissionsToPrivilege(HOST_ENROLLMENT_PRIVILEGE, List.of(ADD_HOSTS_PERMISSION));
        verify(freeIpaClient).addPermissionsToPrivilege(HOST_ENROLLMENT_PRIVILEGE, List.of(REMOVE_HOSTS_PERMISSION));
        verify(freeIpaClient).addPermissionsToPrivilege(HOST_ENROLLMENT_PRIVILEGE, List.of(REMOVE_SERVICES_PERMISSION));
        verify(freeIpaClient).addRolePrivileges(ENROLLMENT_ADMINISTRATOR_ROLE, Set.of(DNS_ADMINISTRATORS_PRIVILEGE));
        verifyNoInteractions(poller);
        verify(freeIpaClientFactory, times(1)).createClientForAllInstances(stack);
    }

    @Test
    public void testMultipleInstanceWithPolling() throws FreeIpaClientException {
        FreeIpaClient freeIpaClient = mock(FreeIpaClient.class);
        Stack stack = new Stack();
        InstanceGroup instanceGroup = new InstanceGroup();
        InstanceMetaData instanceMetaData1 = new InstanceMetaData();
        instanceMetaData1.setInstanceStatus(InstanceStatus.CREATED);
        instanceMetaData1.setDiscoveryFQDN("name1");
        InstanceMetaData instanceMetaData2 = new InstanceMetaData();
        instanceMetaData2.setInstanceStatus(InstanceStatus.CREATED);
        instanceMetaData2.setDiscoveryFQDN("name2");
        InstanceMetaData instanceMetaData3 = new InstanceMetaData();
        instanceMetaData3.setInstanceStatus(InstanceStatus.CREATED);
        instanceMetaData3.setDiscoveryFQDN("name3");
        instanceGroup.setInstanceMetaData(Set.of(instanceMetaData1, instanceMetaData2, instanceMetaData3));
        stack.setInstanceGroups(Set.of(instanceGroup));
        FreeIpaClient freeIpaClient1 = mock(FreeIpaClient.class);
        FreeIpaClient freeIpaClient2 = mock(FreeIpaClient.class);
        FreeIpaClient freeIpaClient3 = mock(FreeIpaClient.class);
        when(freeIpaClientFactory.createClientForAllInstances(stack)).thenReturn(List.of(freeIpaClient1, freeIpaClient2, freeIpaClient3));

        underTest.setPermissions(stack, freeIpaClient);

        ArgumentCaptor<FreeIpaPermissionReplicatedPoller> replicatedPollerArgumentCaptor = ArgumentCaptor.forClass(FreeIpaPermissionReplicatedPoller.class);
        verify(poller, times(3)).runPollerDontStopOnException(eq(POLLING_INTERVAL), eq(POLLING_DELAY), replicatedPollerArgumentCaptor.capture());
        verify(freeIpaClient).addPermissionsToPrivilege(HOST_ENROLLMENT_PRIVILEGE, List.of(ADD_HOSTS_PERMISSION));
        verify(freeIpaClient).addPermissionsToPrivilege(HOST_ENROLLMENT_PRIVILEGE, List.of(REMOVE_HOSTS_PERMISSION));
        verify(freeIpaClient).addPermissionsToPrivilege(HOST_ENROLLMENT_PRIVILEGE, List.of(REMOVE_SERVICES_PERMISSION));
        verify(freeIpaClient).addRolePrivileges(ENROLLMENT_ADMINISTRATOR_ROLE, Set.of(DNS_ADMINISTRATORS_PRIVILEGE));
        List<FreeIpaPermissionReplicatedPoller> pollerArgumentCaptorAllValues = replicatedPollerArgumentCaptor.getAllValues();
        FreeIpaPermissionReplicatedPoller addHostPermissionPoller = pollerArgumentCaptorAllValues.get(0);
        assertEquals(ADD_HOSTS_PERMISSION, ReflectionTestUtils.getField(addHostPermissionPoller, "permission"));
        assertEquals(HOST_ENROLLMENT_PRIVILEGE, ReflectionTestUtils.getField(addHostPermissionPoller, "privilegeName"));
        assertTrue(((List<FreeIpaClient>) ReflectionTestUtils.getField(addHostPermissionPoller, "clientForAllInstances"))
                .containsAll(List.of(freeIpaClient1, freeIpaClient2, freeIpaClient3)));
        FreeIpaPermissionReplicatedPoller removeHostPermissionPoller = pollerArgumentCaptorAllValues.get(1);
        assertEquals(REMOVE_HOSTS_PERMISSION, ReflectionTestUtils.getField(removeHostPermissionPoller, "permission"));
        assertEquals(HOST_ENROLLMENT_PRIVILEGE, ReflectionTestUtils.getField(removeHostPermissionPoller, "privilegeName"));
        assertTrue(((List<FreeIpaClient>) ReflectionTestUtils.getField(removeHostPermissionPoller, "clientForAllInstances"))
                .containsAll(List.of(freeIpaClient1, freeIpaClient2, freeIpaClient3)));
        FreeIpaPermissionReplicatedPoller removeServicesPermissionPoller = pollerArgumentCaptorAllValues.get(2);
        assertEquals(REMOVE_SERVICES_PERMISSION, ReflectionTestUtils.getField(removeServicesPermissionPoller, "permission"));
        assertEquals(HOST_ENROLLMENT_PRIVILEGE, ReflectionTestUtils.getField(removeServicesPermissionPoller, "privilegeName"));
        assertTrue(((List<FreeIpaClient>) ReflectionTestUtils.getField(removeServicesPermissionPoller, "clientForAllInstances"))
                .containsAll(List.of(freeIpaClient1, freeIpaClient2, freeIpaClient3)));
    }
}