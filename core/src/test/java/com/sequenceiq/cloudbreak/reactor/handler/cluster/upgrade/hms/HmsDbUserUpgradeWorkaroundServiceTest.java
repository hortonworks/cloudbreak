package com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.hms;

import static com.sequenceiq.cloudbreak.sdx.RdcConstants.HiveMetastoreDatabase.HIVE_METASTORE_DATABASE_PASSWORD;
import static com.sequenceiq.cloudbreak.sdx.RdcConstants.HiveMetastoreDatabase.HIVE_METASTORE_DATABASE_USER;
import static com.sequenceiq.cloudbreak.sdx.TargetPlatform.PAAS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.Table;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.api.ClusterModificationService;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.ClusterUpgradeRequest;
import com.sequenceiq.cloudbreak.sdx.common.PlatformAwareSdxConnector;
import com.sequenceiq.cloudbreak.sdx.common.model.SdxBasicView;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;

@ExtendWith(MockitoExtension.class)
public class HmsDbUserUpgradeWorkaroundServiceTest {

    @Mock
    private ClusterApiConnectors clusterApiConnectors;

    @Mock
    private RdsConfigService rdsConfigService;

    @Mock
    private PlatformAwareSdxConnector platformAwareSdxConnector;

    @InjectMocks
    private HmsDbUserUpgradeWorkaroundService underTest;

    @Test
    void testBasicConditions() {
        StackDtoDelegate stack = mock(StackDtoDelegate.class);

        ClusterUpgradeRequest request = new ClusterUpgradeRequest(1L, Set.of(), false, false);
        underTest.switchToSdxHmsDbUserBeforeUpgradeIfNeeded(stack, request, Optional.empty());
        underTest.switchBackToOriginalHmsDbUserIfNeeded(stack, request);

        request = new ClusterUpgradeRequest(1L, Set.of(new ClouderaManagerProduct()), true, false);
        underTest.switchToSdxHmsDbUserBeforeUpgradeIfNeeded(stack, request, Optional.empty());
        underTest.switchBackToOriginalHmsDbUserIfNeeded(stack, request);

        request = new ClusterUpgradeRequest(1L, Set.of(new ClouderaManagerProduct()), false, false);
        when(stack.getType()).thenReturn(StackType.DATALAKE);
        underTest.switchToSdxHmsDbUserBeforeUpgradeIfNeeded(stack, request, Optional.empty());
        underTest.switchBackToOriginalHmsDbUserIfNeeded(stack, request);

        verify(clusterApiConnectors, never()).getConnector(any(StackDtoDelegate.class));
    }

    @Test
    void testReplaceBeforeUpgradeWhenNeeded() throws Exception {
        StackDtoDelegate stack = mock(StackDtoDelegate.class);
        when(stack.getType()).thenReturn(StackType.WORKLOAD);
        Cluster cluster = mock(Cluster.class);
        when(cluster.getId()).thenReturn(1L);
        when(stack.getCluster()).thenReturn(cluster);
        ClusterUpgradeRequest request = new ClusterUpgradeRequest(1L, Set.of(new ClouderaManagerProduct()), false, false);
        ClusterApi clusterApi = mock(ClusterApi.class);
        when(clusterApiConnectors.getConnector(any(StackDtoDelegate.class))).thenReturn(clusterApi);
        ClusterModificationService modService = mock(ClusterModificationService.class);
        when(clusterApi.clusterModificationService()).thenReturn(modService);
        when(modService.isServicePresent(any(), any())).thenReturn(Boolean.TRUE);
        doNothing().when(modService).updateConfigWithoutRestart(any(), any());
        RDSConfig rdsConfig = mock(RDSConfig.class);
        when(rdsConfig.getType()).thenReturn("HIVE");
        when(rdsConfig.getClusters()).thenReturn(Set.of(cluster));
        when(rdsConfigService.findByClusterId(any())).thenReturn(Set.of(rdsConfig));

        when(platformAwareSdxConnector.getSdxBasicViewByEnvironmentCrn(any())).thenReturn(Optional.of(SdxBasicView.builder()
                .withCrn("crn")
                .withPlatform(PAAS)
                .build()));
        when(platformAwareSdxConnector.getHmsServiceConfig(any(), any())).thenReturn(Map.of(
                HIVE_METASTORE_DATABASE_PASSWORD, "pass",
                HIVE_METASTORE_DATABASE_USER, "hive"
        ));

        underTest.switchToSdxHmsDbUserBeforeUpgradeIfNeeded(stack, request, Optional.of(""));

        ArgumentCaptor<Table> tableCaptor = ArgumentCaptor.forClass(Table.class);
        verify(modService).updateConfigWithoutRestart(tableCaptor.capture(), any());
        assertEquals(2, tableCaptor.getValue().size());
        assertEquals("hive", tableCaptor.getValue().get("HIVE", HIVE_METASTORE_DATABASE_USER));
        assertEquals("pass", tableCaptor.getValue().get("HIVE", HIVE_METASTORE_DATABASE_PASSWORD));
    }

    @Test
    void testReplaceBeforeUpgradeWhenNotNeeded() throws Exception {
        StackDtoDelegate stack = mock(StackDtoDelegate.class);
        when(stack.getType()).thenReturn(StackType.WORKLOAD);
        Cluster cluster = mock(Cluster.class);
        when(cluster.getId()).thenReturn(1L);
        when(stack.getCluster()).thenReturn(cluster);
        ClusterUpgradeRequest request = new ClusterUpgradeRequest(1L, Set.of(new ClouderaManagerProduct()), false, false);
        ClusterApi clusterApi = mock(ClusterApi.class);
        when(clusterApiConnectors.getConnector(any(StackDtoDelegate.class))).thenReturn(clusterApi);
        ClusterModificationService modService = mock(ClusterModificationService.class);
        when(clusterApi.clusterModificationService()).thenReturn(modService);
        when(modService.isServicePresent(any(), any())).thenReturn(Boolean.TRUE);
        RDSConfig rdsConfig = mock(RDSConfig.class);
        when(rdsConfig.getType()).thenReturn("HIVE");
        when(rdsConfig.getClusters()).thenReturn(Set.of(mock(Cluster.class), cluster));
        when(rdsConfigService.findByClusterId(any())).thenReturn(Set.of(rdsConfig));

        underTest.switchToSdxHmsDbUserBeforeUpgradeIfNeeded(stack, request, Optional.empty());

        verify(modService, never()).updateConfigWithoutRestart(any(), any());
    }

    @Test
    void testReplaceAfterUpgradeWhenNeeded() throws Exception {
        StackDtoDelegate stack = mock(StackDtoDelegate.class);
        when(stack.getType()).thenReturn(StackType.WORKLOAD);
        Cluster cluster = mock(Cluster.class);
        when(cluster.getId()).thenReturn(1L);
        when(stack.getCluster()).thenReturn(cluster);
        ClusterUpgradeRequest request = new ClusterUpgradeRequest(1L, Set.of(new ClouderaManagerProduct()), false, false);
        ClusterApi clusterApi = mock(ClusterApi.class);
        when(clusterApiConnectors.getConnector(any(StackDtoDelegate.class))).thenReturn(clusterApi);
        ClusterModificationService modService = mock(ClusterModificationService.class);
        when(clusterApi.clusterModificationService()).thenReturn(modService);
        when(modService.isServicePresent(any(), any())).thenReturn(Boolean.TRUE);
        doNothing().when(modService).updateConfig(any(), any());
        RDSConfig rdsConfig = mock(RDSConfig.class);
        when(rdsConfig.getType()).thenReturn("HIVE");
        when(rdsConfig.getConnectionUserName()).thenReturn("hive");
        when(rdsConfig.getConnectionPassword()).thenReturn("pass");
        when(rdsConfig.getClusters()).thenReturn(Set.of(cluster));
        when(rdsConfigService.findByClusterId(any())).thenReturn(Set.of(rdsConfig));

        underTest.switchBackToOriginalHmsDbUserIfNeeded(stack, request);

        ArgumentCaptor<Table> tableCaptor = ArgumentCaptor.forClass(Table.class);
        verify(modService).updateConfig(tableCaptor.capture(), any());
        assertEquals(2, tableCaptor.getValue().size());
        assertEquals("hive", tableCaptor.getValue().get("HIVE", HIVE_METASTORE_DATABASE_USER));
        assertEquals("pass", tableCaptor.getValue().get("HIVE", HIVE_METASTORE_DATABASE_PASSWORD));
        verifyNoInteractions(platformAwareSdxConnector);
    }

    @Test
    void testReplaceAfterUpgradeWhenNotNeeded() throws Exception {
        StackDtoDelegate stack = mock(StackDtoDelegate.class);
        when(stack.getType()).thenReturn(StackType.WORKLOAD);
        Cluster cluster = mock(Cluster.class);
        when(cluster.getId()).thenReturn(1L);
        when(stack.getCluster()).thenReturn(cluster);
        ClusterUpgradeRequest request = new ClusterUpgradeRequest(1L, Set.of(new ClouderaManagerProduct()), false, false);
        ClusterApi clusterApi = mock(ClusterApi.class);
        when(clusterApiConnectors.getConnector(any(StackDtoDelegate.class))).thenReturn(clusterApi);
        ClusterModificationService modService = mock(ClusterModificationService.class);
        when(clusterApi.clusterModificationService()).thenReturn(modService);
        when(modService.isServicePresent(any(), any())).thenReturn(Boolean.TRUE);
        RDSConfig rdsConfig = mock(RDSConfig.class);
        when(rdsConfig.getType()).thenReturn("HIVE");
        when(rdsConfig.getClusters()).thenReturn(Set.of(mock(Cluster.class), cluster));
        when(rdsConfigService.findByClusterId(any())).thenReturn(Set.of(rdsConfig));

        underTest.switchBackToOriginalHmsDbUserIfNeeded(stack, request);

        verify(modService, never()).updateConfig(any(), any());
        verifyNoInteractions(platformAwareSdxConnector);
    }
}
