package com.sequenceiq.cloudbreak.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.core.cluster.ClusterBuilderService;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.sdx.common.PlatformAwareSdxConnector;
import com.sequenceiq.cloudbreak.sdx.common.model.SdxBasicView;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.validation.AllRoleTypes;

@ExtendWith(MockitoExtension.class)
class ClusterServicesRestartServiceTest {
    private static final String DATALAKE_CRN = "crn:cdp:datalake:us-west-1:default:datalake:6b2b1600-8ac6-4c26-aa34-dab36f4bd243";

    private static final String DATAHUB_CRN = "crn:cdp:datahub:us-west-1:default:cluster:0ba0ca99-e961-4c8d-b7e9-da0587cd40d0";

    @Mock
    private ClusterApiConnectors apiConnectors;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private ClusterBuilderService clusterBuilderService;

    @Mock
    private PlatformAwareSdxConnector platformAwareSdxConnector;

    @Mock
    private StackService stackService;

    @Mock
    private RdsConfigService rdsConfigService;

    @Mock
    private ClusterService clusterService;

    @InjectMocks
    private ClusterServicesRestartService underTest;

    @Test
    void testDatahubHmsRdsConfigShouldBeUpdatedWhenDifferentFromDatalake() throws CloudbreakException {
        RDSConfig dhHmsRdsConfig = new RDSConfig();
        dhHmsRdsConfig.setId(333L);
        dhHmsRdsConfig.setType(DatabaseType.HIVE.name());
        RDSConfig hueRdsConfig = new RDSConfig();
        hueRdsConfig.setId(444L);
        hueRdsConfig.setType(DatabaseType.HUE.name());
        Set<RDSConfig> dhRdsConfigs = new HashSet<>();
        dhRdsConfigs.add(dhHmsRdsConfig);
        dhRdsConfigs.add(hueRdsConfig);
        Stack stack = new Stack();
        stack.setId(20L);
        stack.setResourceCrn(DATAHUB_CRN);
        Cluster cluster = new Cluster();
        cluster.setId(20L);
        stack.setCluster(cluster);
        cluster.setRdsConfigs(dhRdsConfigs);

        RDSConfig hmsRdsConfig = new RDSConfig();
        hmsRdsConfig.setId(111L);
        hmsRdsConfig.setType(DatabaseType.HIVE.name());
        RDSConfig cmRdsConfig = new RDSConfig();
        cmRdsConfig.setId(222L);
        cmRdsConfig.setType(DatabaseType.CLOUDERA_MANAGER.name());
        Set<RDSConfig> dlRdsConfigs = new HashSet<>();
        dlRdsConfigs.add(hmsRdsConfig);
        dlRdsConfigs.add(cmRdsConfig);
        Stack dlStack = new Stack();
        dlStack.setId(30L);
        Cluster dlCluster = new Cluster();
        dlCluster.setId(30L);
        dlStack.setCluster(dlCluster);
        dlCluster.setRdsConfigs(dlRdsConfigs);

        SdxBasicView sdxBasicView = mock(SdxBasicView.class);
        Map<String, String> hmsServiceConfig = mock(Map.class);
        CmTemplateProcessor cmTemplateProcessor = mock(CmTemplateProcessor.class);
        ClusterApi clusterApi = mock(ClusterApi.class);
        when(sdxBasicView.crn()).thenReturn(DATALAKE_CRN);
        when(stackService.getByCrn(sdxBasicView.crn())).thenReturn(dlStack);
        when(rdsConfigService.findByClusterId(30L)).thenReturn(dlRdsConfigs);
        when(rdsConfigService.findByClusterId(20L)).thenReturn(dhRdsConfigs);
        when(cmTemplateProcessor.doesCMComponentExistsInBlueprint(eq(AllRoleTypes.HIVEMETASTORE.name()))).thenReturn(true);
        when(platformAwareSdxConnector.getHmsServiceConfig(sdxBasicView.crn())).thenReturn(hmsServiceConfig);
        when(hmsServiceConfig.get(anyString())).thenReturn("value");
        when(apiConnectors.getConnector(stack)).thenReturn(clusterApi);

        underTest.refreshClusterOnRestart(stack, sdxBasicView, cmTemplateProcessor, false);

        ArgumentCaptor<Cluster> clusterArgumentCaptor = ArgumentCaptor.forClass(Cluster.class);
        verify(clusterService, times(1)).save(clusterArgumentCaptor.capture());
        Cluster clusterSaved = clusterArgumentCaptor.getValue();
        Set<RDSConfig> rdsConfigsSaved = clusterSaved.getRdsConfigs();
        RDSConfig hmsRdsConfigSaved =
                rdsConfigsSaved
                        .stream()
                        .filter(config -> DatabaseType.HIVE.name().equals(config.getType()))
                        .findFirst()
                        .get();
        assertEquals(hmsRdsConfig.getId(), hmsRdsConfigSaved.getId());
    }
}
