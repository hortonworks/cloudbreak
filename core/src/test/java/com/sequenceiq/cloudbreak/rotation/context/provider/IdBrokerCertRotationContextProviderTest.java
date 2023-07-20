package com.sequenceiq.cloudbreak.rotation.context.provider;


import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretRotationStep.CM_SERVICE_ROLE_RESTART;
import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretRotationStep.SALT_PILLAR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.converter.IdBrokerConverterUtil;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.ClusterHostServiceRunner;
import com.sequenceiq.cloudbreak.domain.stack.cluster.IdBroker;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType;
import com.sequenceiq.cloudbreak.rotation.ExitCriteriaProvider;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.context.CMServiceRoleRestartRotationContext;
import com.sequenceiq.cloudbreak.rotation.context.SaltPillarRotationContext;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.idbroker.IdBrokerService;
import com.sequenceiq.cloudbreak.service.secret.domain.Secret;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.ClusterView;

@ExtendWith(MockitoExtension.class)
class IdBrokerCertRotationContextProviderTest {

    private static final String RESOURCE_CRN = "resourceCrn";

    @Mock
    private StackDtoService stackService;

    @Mock
    private IdBrokerService idBrokerService;

    @Mock
    private IdBrokerConverterUtil idBrokerConverterUtil;

    @Spy
    private ClusterHostServiceRunner clusterHostServiceRunner;

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private ExitCriteriaProvider exitCriteriaProvider;

    @Mock
    private ClusterApiConnectors clusterApiConnectors;

    @Mock
    private StackDto stackDto;

    @Mock
    private ClusterView clusterView;

    @Mock
    private Secret secret;

    @InjectMocks
    private IdBrokerCertRotationContextProvider underTest;

    @Test
    void testGetContexts() {
        when(stackService.getByCrn(any())).thenReturn(stackDto);
        when(stackDto.getCluster()).thenReturn(clusterView);
        when(stackDto.getResourceCrn()).thenReturn(RESOURCE_CRN);
        IdBroker oldIdBroker = getIdBroker("Old");
        IdBroker newIdBroker = getIdBroker("New");
        when(idBrokerService.getByCluster(any())).thenReturn(oldIdBroker);
        when(idBrokerConverterUtil.generateIdBrokerSignKeys(any(), any())).thenReturn(newIdBroker);

        Map<SecretRotationStep, RotationContext> contexts = underTest.getContexts(RESOURCE_CRN);

        assertEquals(5, contexts.size());
        assertTrue(CloudbreakSecretType.IDBROKER_CERT.getSteps().stream().allMatch(contexts::containsKey));

        CMServiceRoleRestartRotationContext roleRestartContext = (CMServiceRoleRestartRotationContext) contexts.get(CM_SERVICE_ROLE_RESTART);
        assertEquals("KNOX", roleRestartContext.getServiceType());
        assertEquals("IDBROKER", roleRestartContext.getRoleType());

        SaltPillarRotationContext saltPillarRotationContext = (SaltPillarRotationContext) contexts.get(SALT_PILLAR);
        Map<String, SaltPillarProperties> pillar = saltPillarRotationContext.getServicePillarGenerator().apply(stackDto);
        Map<String, String> idBrokerProperties = (Map<String, String>) pillar.get("idbroker").getProperties().get("idbroker");
        assertEquals("signCertNew", idBrokerProperties.get("signcert"));
        assertEquals("signKeyNew", idBrokerProperties.get("signkey"));
        assertEquals("signPubNew", idBrokerProperties.get("signpub"));
    }

    private IdBroker getIdBroker(String suffix) {
        IdBroker result = mock(IdBroker.class);
        lenient().when(result.getMasterSecret()).thenReturn(String.format("masterSecret%s", suffix));
        lenient().when(result.getSignCert()).thenReturn(String.format("signCert%s", suffix));
        lenient().when(result.getSignKey()).thenReturn(String.format("signKey%s", suffix));
        lenient().when(result.getSignPub()).thenReturn(String.format("signPub%s", suffix));
        lenient().when(result.getSignKeySecret()).thenReturn(secret);
        lenient().when(secret.getSecret()).thenReturn("secret");
        return result;
    }
}