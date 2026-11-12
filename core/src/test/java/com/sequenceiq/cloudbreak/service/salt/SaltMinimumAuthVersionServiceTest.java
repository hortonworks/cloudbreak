package com.sequenceiq.cloudbreak.service.salt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.domain.Orchestrator;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterComponent;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.model.BootstrapParams;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.salt.utils.SaltMinimumAuthVersionCalculator;
import com.sequenceiq.cloudbreak.service.TlsSecurityService;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;

@ExtendWith(MockitoExtension.class)
class SaltMinimumAuthVersionServiceTest {

    private static final long STACK_ID = 1L;

    private static final long CLUSTER_ID = 2L;

    private static final String OLD_SALT = "3006.10";

    private static final String NEW_SALT = "3006.25";

    @InjectMocks
    private SaltMinimumAuthVersionService underTest;

    @Spy
    private SaltMinimumAuthVersionCalculator saltMinimumAuthVersionCalculator;

    @Mock
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @Mock
    private TlsSecurityService tlsSecurityService;

    @Mock
    private StackDto stack;

    private final List<InstanceMetadataView> instances = new ArrayList<>();

    private ClusterComponent saltStateComponent;

    @BeforeEach
    void setUp() {
        lenient().when(stack.getId()).thenReturn(STACK_ID);
        lenient().when(stack.getAllAvailableAndProviderDeletedInstances()).thenReturn(instances);
        ClusterView cluster = mock(ClusterView.class);
        lenient().when(cluster.getId()).thenReturn(CLUSTER_ID);
        lenient().when(stack.getCluster()).thenReturn(cluster);
        lenient().when(clusterComponentConfigProvider.getComponent(CLUSTER_ID, ComponentType.SALT_STATE)).thenAnswer(invocation -> saltStateComponent);
    }

    @Test
    void firstBootstrapWritesAndSavesTheValueWithoutStoppingNewMasters() {
        saltStateComponent(Map.of(ComponentType.SALT_STATE.name(), "zip"));
        when(stack.getOrchestrator()).thenReturn(new Orchestrator());
        instances(NEW_SALT, NEW_SALT);
        List<GatewayConfig> gatewayConfigs = gateways(NEW_SALT);
        BootstrapParams params = new BootstrapParams();

        underTest.configureBootstrapParams(stack, gatewayConfigs, params);

        assertThat(params.getSaltMasterMinimumAuthVersion()).isEqualTo(3);
        assertThat(params.isSaltMasterRestartNeeded()).isFalse();
        assertThat(storedAttributes()).doesNotContainValue(3);

        underTest.saveAppliedMinimumAuthVersion(stack, gatewayConfigs, params);

        assertThat(storedAttributes()).containsEntry(SaltMinimumAuthVersionService.APPLIED_MINIMUM_AUTH_VERSION, 3)
                .containsEntry(ComponentType.SALT_STATE.name(), "zip");
    }

    @Test
    void failedFirstBootstrapRestartsMastersOnRetryEvenWithoutAnOrchestratorEndpoint() {
        saltStateComponent(Map.of());
        when(stack.getOrchestrator()).thenReturn(new Orchestrator());
        instances(NEW_SALT, NEW_SALT);
        List<GatewayConfig> gatewayConfigs = gateways(NEW_SALT);
        underTest.configureBootstrapParams(stack, gatewayConfigs, new BootstrapParams());
        BootstrapParams retryParams = new BootstrapParams();

        underTest.configureBootstrapParams(stack, gatewayConfigs, retryParams);

        assertThat(retryParams.isSaltMasterRestartNeeded()).isTrue();
    }

    @Test
    void firstGatewayReplacementRelaxesAndRestartsEveryMaster() {
        saltStateComponent(Map.of(ComponentType.SALT_STATE.name(), "zip"));
        instances(NEW_SALT, OLD_SALT, OLD_SALT);
        BootstrapParams params = new BootstrapParams();

        List<GatewayConfig> gatewayConfigs = gateways(NEW_SALT, OLD_SALT);
        underTest.configureBootstrapParams(stack, gatewayConfigs, params);
        underTest.saveAppliedMinimumAuthVersion(stack, gatewayConfigs, params);

        assertThat(params.getSaltMasterMinimumAuthVersion()).isEqualTo(2);
        assertThat(params.isSaltMasterRestartNeeded()).isTrue();
        assertThat(storedAttributes()).containsEntry(SaltMinimumAuthVersionService.APPLIED_MINIMUM_AUTH_VERSION, 2)
                .containsEntry(ComponentType.SALT_STATE.name(), "zip");
    }

    @Test
    void unchangedValueNeedsNoRestartAndNoWrite() {
        saltStateComponent(Map.of(SaltMinimumAuthVersionService.APPLIED_MINIMUM_AUTH_VERSION, 2));
        instances(NEW_SALT, NEW_SALT, OLD_SALT);
        BootstrapParams params = new BootstrapParams();

        List<GatewayConfig> gatewayConfigs = gateways(NEW_SALT, NEW_SALT);
        underTest.configureBootstrapParams(stack, gatewayConfigs, params);
        underTest.saveAppliedMinimumAuthVersion(stack, gatewayConfigs, params);

        assertThat(params.getSaltMasterMinimumAuthVersion()).isEqualTo(2);
        assertThat(params.isSaltMasterRestartNeeded()).isFalse();
        verify(clusterComponentConfigProvider, never()).store(any(ClusterComponent.class));
    }

    @Test
    void lastOldMinionGoneRestoresEnforcementAndForgetsTheOldValueUntilTheRestartSucceeds() {
        saltStateComponent(Map.of(SaltMinimumAuthVersionService.APPLIED_MINIMUM_AUTH_VERSION, 2));
        instances(NEW_SALT, NEW_SALT, NEW_SALT);
        BootstrapParams params = new BootstrapParams();

        List<GatewayConfig> gatewayConfigs = gateways(NEW_SALT, NEW_SALT);
        underTest.configureBootstrapParams(stack, gatewayConfigs, params);

        assertThat(params.getSaltMasterMinimumAuthVersion()).isEqualTo(3);
        assertThat(params.isSaltMasterRestartNeeded()).isTrue();
        assertThat(storedAttributes()).containsEntry(SaltMinimumAuthVersionService.APPLIED_MINIMUM_AUTH_VERSION, "unknown");

        underTest.saveAppliedMinimumAuthVersion(stack, gatewayConfigs, params);

        assertThat(storedAttributes()).containsEntry(SaltMinimumAuthVersionService.APPLIED_MINIMUM_AUTH_VERSION, 3);
        verify(clusterComponentConfigProvider, times(2)).store(any(ClusterComponent.class));
    }

    @Test
    void failedRestartKeepsTheValueUnknownSoTheNextBootstrapRestartsAgain() {
        saltStateComponent(Map.of(SaltMinimumAuthVersionService.APPLIED_MINIMUM_AUTH_VERSION, 2));
        instances(NEW_SALT, NEW_SALT, NEW_SALT);
        underTest.configureBootstrapParams(stack, gateways(NEW_SALT, NEW_SALT), new BootstrapParams());
        BootstrapParams retryParams = new BootstrapParams();

        underTest.configureBootstrapParams(stack, gateways(NEW_SALT, NEW_SALT), retryParams);

        assertThat(retryParams.getSaltMasterMinimumAuthVersion()).isEqualTo(3);
        assertThat(retryParams.isSaltMasterRestartNeeded()).isTrue();
    }

    @Test
    void unknownAppliedValueOfAManagedClusterRestartsTheMasters() {
        saltStateComponent(Map.of(ComponentType.SALT_STATE.name(), "zip"));
        instances(NEW_SALT, NEW_SALT);
        BootstrapParams params = new BootstrapParams();

        List<GatewayConfig> gatewayConfigs = gateways(NEW_SALT);
        underTest.configureBootstrapParams(stack, gatewayConfigs, params);

        assertThat(params.getSaltMasterMinimumAuthVersion()).isEqualTo(3);
        assertThat(params.isSaltMasterRestartNeeded()).isTrue();
        verify(clusterComponentConfigProvider, never()).store(any(ClusterComponent.class));
    }

    @Test
    void oldGatewaysDoNotManageTheCluster() {
        saltStateComponent(Map.of());
        instances(OLD_SALT, OLD_SALT);
        BootstrapParams params = new BootstrapParams();

        List<GatewayConfig> gatewayConfigs = gateways(OLD_SALT);
        underTest.configureBootstrapParams(stack, gatewayConfigs, params);

        assertThat(params.getSaltMasterMinimumAuthVersion()).isNull();
        assertThat(params.isSaltMasterRestartNeeded()).isFalse();
    }

    @Test
    void clusterWithoutNewSaltIsUnmanaged() {
        instances(OLD_SALT, OLD_SALT);
        BootstrapParams params = new BootstrapParams();

        List<GatewayConfig> gatewayConfigs = gateways(OLD_SALT);
        underTest.configureBootstrapParams(stack, gatewayConfigs, params);
        underTest.saveAppliedMinimumAuthVersion(stack, gatewayConfigs, params);

        assertThat(params.getSaltMasterMinimumAuthVersion()).isNull();
        assertThat(params.isSaltMasterRestartNeeded()).isFalse();
        verify(clusterComponentConfigProvider, never()).store(any(ClusterComponent.class));
    }

    @Test
    void missingSaltStateComponentRestartsWithoutFailing() {
        instances(NEW_SALT, OLD_SALT);
        BootstrapParams params = new BootstrapParams();

        List<GatewayConfig> gatewayConfigs = gateways(NEW_SALT);
        underTest.configureBootstrapParams(stack, gatewayConfigs, params);
        underTest.saveAppliedMinimumAuthVersion(stack, gatewayConfigs, params);

        assertThat(params.getSaltMasterMinimumAuthVersion()).isEqualTo(2);
        assertThat(params.isSaltMasterRestartNeeded()).isTrue();
        verify(clusterComponentConfigProvider, never()).store(any(ClusterComponent.class));
    }

    @Test
    void appliedValueIsSavedWhenEveryGatewayWasReconciled() {
        saltStateComponent(Map.of());
        instances(NEW_SALT, NEW_SALT);
        gatewayInstances("10.0.0.1", "10.0.0.2");
        List<GatewayConfig> gatewayConfigs = List.of(gateway("10.0.0.1"), gateway("10.0.0.2"));
        BootstrapParams params = new BootstrapParams();

        underTest.configureBootstrapParams(stack, gatewayConfigs, params);
        underTest.saveAppliedMinimumAuthVersion(stack, gatewayConfigs, params);

        assertThat(storedAttributes()).containsEntry(SaltMinimumAuthVersionService.APPLIED_MINIMUM_AUTH_VERSION, 3);
    }

    @Test
    void unreachableGatewayKeepsTheAppliedValueUnknownUntilABootstrapReachesEveryGateway() {
        saltStateComponent(Map.of(SaltMinimumAuthVersionService.APPLIED_MINIMUM_AUTH_VERSION, 2));
        instances(NEW_SALT, NEW_SALT, NEW_SALT);
        gatewayInstances("10.0.0.1", "10.0.0.2");
        List<GatewayConfig> reachableGatewayConfigs = List.of(gateway("10.0.0.1"));
        BootstrapParams params = new BootstrapParams();

        underTest.configureBootstrapParams(stack, reachableGatewayConfigs, params);
        underTest.saveAppliedMinimumAuthVersion(stack, reachableGatewayConfigs, params);

        assertThat(params.getSaltMasterMinimumAuthVersion()).isEqualTo(3);
        assertThat(params.isSaltMasterRestartNeeded()).isTrue();
        assertThat(storedAttributes()).containsEntry(SaltMinimumAuthVersionService.APPLIED_MINIMUM_AUTH_VERSION, "unknown");
        verify(clusterComponentConfigProvider, times(1)).store(any(ClusterComponent.class));

        List<GatewayConfig> everyGatewayConfig = List.of(gateway("10.0.0.1"), gateway("10.0.0.2"));
        BootstrapParams nextParams = new BootstrapParams();

        underTest.configureBootstrapParams(stack, everyGatewayConfig, nextParams);
        underTest.saveAppliedMinimumAuthVersion(stack, everyGatewayConfig, nextParams);

        assertThat(nextParams.getSaltMasterMinimumAuthVersion()).isEqualTo(3);
        assertThat(nextParams.isSaltMasterRestartNeeded()).isTrue();
        assertThat(storedAttributes()).containsEntry(SaltMinimumAuthVersionService.APPLIED_MINIMUM_AUTH_VERSION, 3);
        verify(clusterComponentConfigProvider, times(2)).store(any(ClusterComponent.class));
    }

    private void saltStateComponent(Map<String, Object> attributes) {
        saltStateComponent = new ClusterComponent(ComponentType.SALT_STATE, new Json(attributes), null);
    }

    private void instances(String... saltVersions) {
        for (String saltVersion : saltVersions) {
            InstanceMetadataView instance = mock(InstanceMetadataView.class);
            when(tlsSecurityService.getInstanceSaltVersion(instance)).thenReturn(saltVersion);
            instances.add(instance);
        }
    }

    private List<GatewayConfig> gateways(String... saltVersions) {
        return Arrays.stream(saltVersions)
                .map(saltVersion -> GatewayConfig.builder().withSaltVersion(saltVersion).build())
                .toList();
    }

    private GatewayConfig gateway(String privateAddress) {
        return GatewayConfig.builder().withPrivateAddress(privateAddress).withSaltVersion(NEW_SALT).build();
    }

    private void gatewayInstances(String... privateIps) {
        List<InstanceMetadataView> gatewayInstances = Arrays.stream(privateIps)
                .map(privateIp -> {
                    InstanceMetadataView gatewayInstance = mock(InstanceMetadataView.class);
                    lenient().when(gatewayInstance.getPrivateIp()).thenReturn(privateIp);
                    lenient().when(gatewayInstance.getInstanceId()).thenReturn("i-" + privateIp);
                    return gatewayInstance;
                })
                .toList();
        when(stack.getNotTerminatedAndNotZombieGatewayInstanceMetadata()).thenReturn(gatewayInstances);
    }

    private Map<String, Object> storedAttributes() {
        ArgumentCaptor<ClusterComponent> componentCaptor = ArgumentCaptor.forClass(ClusterComponent.class);
        verify(clusterComponentConfigProvider, atLeastOnce()).store(componentCaptor.capture());
        return new HashMap<>(componentCaptor.getValue().getAttributes().getMap());
    }
}
