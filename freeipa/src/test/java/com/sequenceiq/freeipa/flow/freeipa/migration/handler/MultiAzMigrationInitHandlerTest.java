package com.sequenceiq.freeipa.flow.freeipa.migration.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.iterable;
import static org.assertj.core.api.InstanceOfAssertFactories.list;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.network.NetworkConstants;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceGroupAvailabilityZone;
import com.sequenceiq.freeipa.entity.InstanceGroupNetwork;
import com.sequenceiq.freeipa.entity.Network;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.migration.event.MultiAzMigrationInitFailedEvent;
import com.sequenceiq.freeipa.flow.freeipa.migration.event.MultiAzMigrationInitHandlerRequest;
import com.sequenceiq.freeipa.flow.freeipa.migration.event.MultiAzMigrationInitResult;
import com.sequenceiq.freeipa.service.EnvironmentService;
import com.sequenceiq.freeipa.service.client.CachedEnvironmentClientService;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.service.stack.StackUpdater;
import com.sequenceiq.freeipa.service.stack.instance.InstanceGroupService;

@ExtendWith(MockitoExtension.class)
class MultiAzMigrationInitHandlerTest {

    private static final long STACK_ID = 1L;

    private static final String OPERATION_ID = "op-1";

    private static final String ENVIRONMENT_CRN = "crn:env";

    private static final String CLOUD_PLATFORM = "AWS";

    private static final String AZ_A = "az-a";

    private static final String AZ_B = "az-b";

    private static final String SUBNET_A = "subnet-a";

    private static final String SUBNET_B = "subnet-b";

    private static final String LEGACY_VPC_ID = "vpc-legacy";

    private static final String LEGACY_SUBNET_ID = "subnet-legacy";

    @Mock
    private StackService stackService;

    @Mock
    private InstanceGroupService instanceGroupService;

    @Mock
    private CachedEnvironmentClientService cachedEnvironmentClientService;

    @Mock
    private TransactionService transactionService;

    @Mock
    private StackUpdater stackUpdater;

    @Mock
    private EnvironmentService environmentService;

    @InjectMocks
    private MultiAzMigrationInitHandler underTest;

    @Test
    void testSelector() {
        assertEquals(EventSelectorUtil.selector(MultiAzMigrationInitHandlerRequest.class), underTest.selector());
    }

    @Test
    void testDefaultFailureEvent() {
        Exception e = new Exception("test");

        Selectable result = underTest.defaultFailureEvent(STACK_ID, e,
                new Event<>(new MultiAzMigrationInitHandlerRequest(STACK_ID, OPERATION_ID)));

        assertThat(result).isInstanceOf(MultiAzMigrationInitFailedEvent.class);
        MultiAzMigrationInitFailedEvent failure = (MultiAzMigrationInitFailedEvent) result;
        assertEquals(STACK_ID, failure.getResourceId());
        assertEquals(e, failure.getException());
    }

    @Test
    void testDoAcceptUpdatesAvailabilityZonesAndSubnetsForInstanceGroup() throws TransactionExecutionException {
        InstanceGroup instanceGroup = createInstanceGroup("master", new InstanceGroupNetwork(), Set.of());
        Stack stack = createStack(instanceGroup);
        when(stackService.getEnvironmentCrnByStackId(STACK_ID)).thenReturn(ENVIRONMENT_CRN);
        when(cachedEnvironmentClientService.getByCrn(ENVIRONMENT_CRN)).thenReturn(createEnvironment(Set.of(AZ_A, AZ_B)));
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);
        runTransactionInline();

        MultiAzMigrationInitResult result = (MultiAzMigrationInitResult) underTest.doAccept(new HandlerEvent<>(
                new Event<>(new MultiAzMigrationInitHandlerRequest(STACK_ID, OPERATION_ID))));

        assertThat(zonesOf(instanceGroup)).containsExactlyInAnyOrder(AZ_A, AZ_B);
        Map<String, Object> attributesMap = instanceGroup.getInstanceGroupNetwork().getAttributes().getMap();
        assertThat(attributesMap.get(NetworkConstants.AVAILABILITY_ZONES))
                .asInstanceOf(iterable(String.class))
                .containsExactlyInAnyOrder(AZ_A, AZ_B);
        assertThat(attributesMap.get(NetworkConstants.SUBNET_IDS))
                .asInstanceOf(list(String.class))
                .containsExactlyInAnyOrder(SUBNET_A, SUBNET_B);
        verify(instanceGroupService).save(instanceGroup);
        verify(stackUpdater).updateMultiAzEnabled(STACK_ID, true);
        verify(environmentService).setFreeIpaEnableMultiAz(ENVIRONMENT_CRN);

        assertEquals(STACK_ID, result.getResourceId());
        assertEquals(OPERATION_ID, result.getOperationId());
    }

    @Test
    void testDoAcceptCreatesInstanceGroupNetworkFromStackNetworkWhenMissing() throws TransactionExecutionException {
        InstanceGroup instanceGroup = createInstanceGroup("master", null, Set.of());
        Stack stack = createStack(instanceGroup);
        when(stackService.getEnvironmentCrnByStackId(STACK_ID)).thenReturn(ENVIRONMENT_CRN);
        when(cachedEnvironmentClientService.getByCrn(ENVIRONMENT_CRN)).thenReturn(createEnvironment(Set.of(AZ_A)));
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);
        runTransactionInline();

        underTest.doAccept(new HandlerEvent<>(new Event<>(new MultiAzMigrationInitHandlerRequest(STACK_ID, OPERATION_ID))));

        assertThat(instanceGroup.getInstanceGroupNetwork()).isNotNull();
        assertThat(instanceGroup.getInstanceGroupNetwork().cloudPlatform()).isEqualTo(CLOUD_PLATFORM);
        Map<String, Object> attributes = instanceGroup.getInstanceGroupNetwork().getAttributes().getMap();
        assertThat(attributes).containsEntry(NetworkConstants.VPC_ID, LEGACY_VPC_ID);
        assertThat(attributes).containsEntry(NetworkConstants.SUBNET_ID, LEGACY_SUBNET_ID);
        verify(instanceGroupService).save(instanceGroup);
    }

    @Test
    void testDoAcceptCreatesInstanceGroupNetworkWhenStackNetworkMissing() throws TransactionExecutionException {
        InstanceGroup instanceGroup = createInstanceGroup("master", null, Set.of());
        Stack stack = createStack(instanceGroup, null);
        when(stackService.getEnvironmentCrnByStackId(STACK_ID)).thenReturn(ENVIRONMENT_CRN);
        when(cachedEnvironmentClientService.getByCrn(ENVIRONMENT_CRN)).thenReturn(createEnvironment(Set.of(AZ_A)));
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);
        runTransactionInline();

        underTest.doAccept(new HandlerEvent<>(new Event<>(new MultiAzMigrationInitHandlerRequest(STACK_ID, OPERATION_ID))));

        assertThat(instanceGroup.getInstanceGroupNetwork()).isNotNull();
        assertThat(instanceGroup.getInstanceGroupNetwork().cloudPlatform()).isEqualTo(CLOUD_PLATFORM);
        assertThat(instanceGroup.getInstanceGroupNetwork().getAttributes()).isNotNull();
        verify(instanceGroupService).save(instanceGroup);
    }

    @Test
    void testDoAcceptSkipsSaveWhenInstanceGroupAlreadyUpToDate() throws TransactionExecutionException {
        InstanceGroupNetwork network = new InstanceGroupNetwork();
        Map<String, Object> existingAttributes = new HashMap<>();
        existingAttributes.put(NetworkConstants.SUBNET_IDS, List.of(SUBNET_A, SUBNET_B));
        existingAttributes.put(NetworkConstants.AVAILABILITY_ZONES, Set.of(AZ_A, AZ_B));
        network.setAttributes(new Json(existingAttributes));
        InstanceGroup instanceGroup = createInstanceGroup("master", network, Set.of(AZ_A, AZ_B));

        Stack stack = createStack(instanceGroup);
        when(stackService.getEnvironmentCrnByStackId(STACK_ID)).thenReturn(ENVIRONMENT_CRN);
        when(cachedEnvironmentClientService.getByCrn(ENVIRONMENT_CRN)).thenReturn(createEnvironment(Set.of(AZ_A, AZ_B)));
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);
        runTransactionInline();

        MultiAzMigrationInitResult result = (MultiAzMigrationInitResult) underTest.doAccept(new HandlerEvent<>(
                new Event<>(new MultiAzMigrationInitHandlerRequest(STACK_ID, OPERATION_ID))));

        assertThat(zonesOf(instanceGroup)).containsExactlyInAnyOrder(AZ_A, AZ_B);
        verify(instanceGroupService, never()).save(any());
    }

    @Test
    void testDoAcceptReplacesStaleAvailabilityZonesOnRerun() throws TransactionExecutionException {
        InstanceGroup instanceGroup = createInstanceGroup("master", new InstanceGroupNetwork(), Set.of("stale-az"));
        Stack stack = createStack(instanceGroup);
        when(stackService.getEnvironmentCrnByStackId(STACK_ID)).thenReturn(ENVIRONMENT_CRN);
        when(cachedEnvironmentClientService.getByCrn(ENVIRONMENT_CRN)).thenReturn(createEnvironment(Set.of(AZ_A, AZ_B)));
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);
        runTransactionInline();

        underTest.doAccept(new HandlerEvent<>(new Event<>(new MultiAzMigrationInitHandlerRequest(STACK_ID, OPERATION_ID))));

        assertThat(zonesOf(instanceGroup)).containsExactlyInAnyOrder(AZ_A, AZ_B);
        verify(instanceGroupService).save(instanceGroup);
    }

    @Test
    void testDoAcceptReturnsFailedEventWhenTransactionFails() throws TransactionExecutionException {
        when(stackService.getEnvironmentCrnByStackId(STACK_ID)).thenReturn(ENVIRONMENT_CRN);
        when(cachedEnvironmentClientService.getByCrn(ENVIRONMENT_CRN)).thenReturn(createEnvironment(Set.of(AZ_A)));
        TransactionExecutionException txException = new TransactionExecutionException("boom", new RuntimeException("boom"));
        doThrow(txException).when(transactionService).required(any(Runnable.class));

        MultiAzMigrationInitFailedEvent result = (MultiAzMigrationInitFailedEvent) underTest.doAccept(new HandlerEvent<>(
                new Event<>(new MultiAzMigrationInitHandlerRequest(STACK_ID, OPERATION_ID))));

        assertEquals(STACK_ID, result.getResourceId());
        assertEquals(txException, result.getException());
        verify(environmentService, never()).setFreeIpaEnableMultiAz(any());
    }

    private Stack createStack(InstanceGroup instanceGroup) {
        return createStack(instanceGroup, createStackNetwork());
    }

    private Stack createStack(InstanceGroup instanceGroup, Network network) {
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        stack.setName("stack-name");
        stack.setCloudPlatform(CLOUD_PLATFORM);
        stack.setInstanceGroups(new HashSet<>(Set.of(instanceGroup)));
        stack.setNetwork(network);
        return stack;
    }

    private Network createStackNetwork() {
        Network network = new Network();
        network.setCloudPlatform(CLOUD_PLATFORM);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(NetworkConstants.VPC_ID, LEGACY_VPC_ID);
        attributes.put(NetworkConstants.SUBNET_ID, LEGACY_SUBNET_ID);
        network.setAttributes(new Json(attributes));
        return network;
    }

    private InstanceGroup createInstanceGroup(String groupName, InstanceGroupNetwork network, Set<String> currentZones) {
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setGroupName(groupName);
        instanceGroup.setInstanceGroupNetwork(network);
        Set<InstanceGroupAvailabilityZone> zones = currentZones.stream()
                .map(az -> {
                    InstanceGroupAvailabilityZone igaz = new InstanceGroupAvailabilityZone();
                    igaz.setInstanceGroup(instanceGroup);
                    igaz.setAvailabilityZone(az);
                    return igaz;
                })
                .collect(Collectors.toCollection(HashSet::new));
        instanceGroup.setAvailabilityZones(zones);
        return instanceGroup;
    }

    private DetailedEnvironmentResponse createEnvironment(Set<String> availabilityZones) {
        Map<String, CloudSubnet> subnetMetas = new HashMap<>();
        if (availabilityZones.contains(AZ_A)) {
            subnetMetas.put(SUBNET_A, new CloudSubnet.Builder().id(SUBNET_A).name(SUBNET_A).availabilityZone(AZ_A).build());
        }
        if (availabilityZones.contains(AZ_B)) {
            subnetMetas.put(SUBNET_B, new CloudSubnet.Builder().id(SUBNET_B).name(SUBNET_B).availabilityZone(AZ_B).build());
        }
        return DetailedEnvironmentResponse.builder()
                .withNetwork(EnvironmentNetworkResponse.builder()
                        .withSubnetMetas(subnetMetas)
                        .build())
                .build();
    }

    private Set<String> zonesOf(InstanceGroup instanceGroup) {
        return instanceGroup.getAvailabilityZones().stream()
                .map(InstanceGroupAvailabilityZone::getAvailabilityZone)
                .collect(Collectors.toSet());
    }

    private void runTransactionInline() throws TransactionExecutionException {
        doAnswer(invocation -> {
            invocation.getArgument(0, Runnable.class).run();
            return null;
        }).when(transactionService).required(any(Runnable.class));
    }
}