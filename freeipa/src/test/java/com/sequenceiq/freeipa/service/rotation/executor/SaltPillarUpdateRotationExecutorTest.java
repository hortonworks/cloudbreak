package com.sequenceiq.freeipa.service.rotation.executor;

import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateParams;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.GatewayConfigService;
import com.sequenceiq.freeipa.service.freeipa.config.FreeIpaBackupConfigView;
import com.sequenceiq.freeipa.service.freeipa.config.FreeIpaConfigView;
import com.sequenceiq.freeipa.service.freeipa.config.FreeIpaEncryptionConfigView;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaNodeUtilService;
import com.sequenceiq.freeipa.service.orchestrator.FreeIpaSaltPingService;
import com.sequenceiq.freeipa.service.rotation.FreeIpaDefaultPillarGenerator;
import com.sequenceiq.freeipa.service.rotation.context.SaltPillarUpdateRotationContext;
import com.sequenceiq.freeipa.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
class SaltPillarUpdateRotationExecutorTest {

    private static final String ENVIRONMENT_CRN = "crn:cdp:environments:us-west-1:accountId:environment:ac5ba74b-c35e-45e9-9f47-123456789876";

    @Mock
    private StackService stackService;

    @Mock
    private FreeIpaSaltPingService freeIpaSaltPingService;

    @Mock
    private FreeIpaNodeUtilService freeIpaNodeUtilService;

    @Mock
    private HostOrchestrator hostOrchestrator;

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private FreeIpaDefaultPillarGenerator freeIpaDefaultPillarGenerator;

    @InjectMocks
    private SaltPillarUpdateRotationExecutor underTest;

    @Test
    public void testRotate() throws Exception {
        Stack stack = mock(Stack.class);
        InstanceMetaData primaryGWInstanceMetadata = mock(InstanceMetaData.class);
        InstanceMetaData otherInstanceMetadata = mock(InstanceMetaData.class);
        when(stack.getPrimaryGateway()).thenReturn(Optional.of(primaryGWInstanceMetadata));
        Set<InstanceMetaData> instanceMetaDataSet = Set.of(primaryGWInstanceMetadata, otherInstanceMetadata);
        when(stack.getNotDeletedInstanceMetaDataSet()).thenReturn(instanceMetaDataSet);
        Node node1 = mock(Node.class);
        when(node1.getHostname()).thenReturn("host1");
        Node node2 = mock(Node.class);
        when(node2.getHostname()).thenReturn("host2");
        Set<Node> allNodes = Set.of(node1, node2);
        when(stackService.getByEnvironmentCrnAndAccountIdWithLists(eq(ENVIRONMENT_CRN), anyString())).thenReturn(stack);
        when(freeIpaNodeUtilService.mapInstancesToNodes(instanceMetaDataSet)).thenReturn(allNodes);
        FreeIpaConfigView freeIpaConfigView = new FreeIpaConfigView.Builder()
                .withAdminUser("adminuser")
                .withPassword("adminpassword")
                .withBackupConfig(mock(FreeIpaBackupConfigView.class))
                .withEncryptionConfig(mock(FreeIpaEncryptionConfigView.class))
                .build();
        Map<String, SaltPillarProperties> freeIpaPillar = new HashMap<>();
        freeIpaPillar.put("freeipa", new SaltPillarProperties("/freeipa/init.sls", singletonMap("freeipa", freeIpaConfigView.toMap())));
        when(freeIpaDefaultPillarGenerator.apply(stack)).thenReturn(freeIpaPillar);
        GatewayConfig gatewayConfig = mock(GatewayConfig.class);
        when(gatewayConfigService.getGatewayConfig(stack, primaryGWInstanceMetadata)).thenReturn(gatewayConfig);
        SaltPillarUpdateRotationContext rotationContext = SaltPillarUpdateRotationContext.builder()
                .withEnvironmentCrn(ENVIRONMENT_CRN)
                .withServicePillarGenerator(freeIpaDefaultPillarGenerator)
                .build();
        underTest.rotate(rotationContext);
        ArgumentCaptor<SaltConfig> saltConfigArgumentCaptor = ArgumentCaptor.forClass(SaltConfig.class);
        ArgumentCaptor<OrchestratorStateParams> orchestratorStateParamsArgumentCaptor = ArgumentCaptor.forClass(OrchestratorStateParams.class);
        verify(hostOrchestrator, times(1)).saveCustomPillars(saltConfigArgumentCaptor.capture(), eq(null), orchestratorStateParamsArgumentCaptor.capture());
        SaltConfig saltConfig = saltConfigArgumentCaptor.getValue();
        Map<String, SaltPillarProperties> servicePillarConfig = saltConfig.getServicePillarConfig();
        SaltPillarProperties freeipaPillarProperties = servicePillarConfig.get("freeipa");
        assertEquals("adminuser", ((Map<String, String>) freeipaPillarProperties.getProperties().get("freeipa")).get("admin_user"));
        assertEquals("adminpassword", ((Map<String, String>) freeipaPillarProperties.getProperties().get("freeipa")).get("password"));
    }
}