package com.sequenceiq.cloudbreak.service.rdsconfig;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.grpc.ManagedChannelWrapper;
import com.sequenceiq.cloudbreak.sdx.common.PlatformAwareSdxConnector;
import com.sequenceiq.cloudbreak.sdx.common.model.SdxBasicView;
import com.sequenceiq.cloudbreak.sdx.common.model.ServiceDiscoveryChannelConfig;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.cluster.EmbeddedDatabaseService;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.cloudbreak.view.StackView;

@ExtendWith(MockitoExtension.class)
public class HiveRdsConfigProviderTest {

    private static final Crn CDL_CRN =
        Crn.safeFromString("crn:cdp:sdxsvc:us-west-1:9d74eee4-1cad-45d7-b645-7ccf9edbb73d:instance:ab79a335-70cc-4c06-90af-ea74efe02636");

    private static final String ACTOR_CRN = "crn:cdp:iam:us-west-1:1" + ":user:" + UUID.randomUUID();

    @InjectMocks
    private HiveRdsConfigProvider underTest;

    @Mock
    private RdsConfigService rdsConfigService;

    @Mock
    private ClusterService clusterService;

    @Mock
    private PlatformAwareSdxConnector platformAwareSdxConnector;

    @Mock
    private RdsConfigWithoutClusterService rdsConfigWithoutClusterService;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private ManagedChannelWrapper managedChannelWrapper;

    @Mock
    private ServiceDiscoveryChannelConfig serviceDiscoveryChannelConfig;

    @Mock
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    @Mock
    private EmbeddedDatabaseService embeddedDatabaseService;

    @Test
    void testCreateRdsConfigIfNeededCdlCrn() {
        lenient().when(platformAwareSdxConnector.getSdxBasicViewByEnvironmentCrn(any())).thenReturn(
                Optional.of(SdxBasicView.builder().withCrn(CDL_CRN.toString()).build()));
        StackDto stackDto = mock(StackDto.class);
        ClusterView clusterView = mock(ClusterView.class);
        when(clusterView.getId()).thenReturn(1L);
        when(stackDto.getCluster()).thenReturn(clusterView);
        Blueprint blueprint = mock(Blueprint.class);
        when(stackDto.getBlueprint()).thenReturn(blueprint);
        CmTemplateProcessor cmTemplateProcessor = mock(CmTemplateProcessor.class);
        when(cmTemplateProcessorFactory.get(any())).thenReturn(cmTemplateProcessor);
        ThreadBasedUserCrnProvider.doAs(ACTOR_CRN, () -> underTest.createPostgresRdsConfigIfNeeded(stackDto));
    }

    @Test
    void testCreateRdsConfigIfNeededPaasCrn() {
        when(rdsConfigService.createIfNotExists(any(), any(), any())).thenAnswer(i -> {
            RDSConfig rdsConfig = i.getArgument(1, RDSConfig.class);
            rdsConfig.setId(1L);
            return rdsConfig;
        });
        StackDto stackDto = mock(StackDto.class);
        ClusterView clusterView = mock(ClusterView.class);
        when(clusterView.getId()).thenReturn(1L);
        when(stackDto.getCluster()).thenReturn(clusterView);
        Blueprint blueprint = mock(Blueprint.class);
        when(stackDto.getBlueprint()).thenReturn(blueprint);
        StackView stackView = mock(StackView.class);
        when(stackDto.getStack()).thenReturn(stackView);
        CmTemplateProcessor cmTemplateProcessor = mock(CmTemplateProcessor.class);
        when(cmTemplateProcessorFactory.get(any())).thenReturn(cmTemplateProcessor);
        when(cmTemplateProcessor.doesCMComponentExistsInBlueprint(any())).thenReturn(true);
        InstanceMetadataView instanceMetadataView = mock(InstanceMetadataView.class);
        when(stackDto.getPrimaryGatewayInstance()).thenReturn(instanceMetadataView);
        when(instanceMetadataView.getDiscoveryFQDN()).thenReturn("fqdn");
        ThreadBasedUserCrnProvider.doAs(ACTOR_CRN, () -> underTest.createPostgresRdsConfigIfNeeded(stackDto));
        verify(embeddedDatabaseService, times(1)).isSslEnforcementForEmbeddedDatabaseEnabled(any(), any(), any());
    }
}
