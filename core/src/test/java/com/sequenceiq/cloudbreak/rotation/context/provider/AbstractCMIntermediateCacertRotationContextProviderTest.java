package com.sequenceiq.cloudbreak.rotation.context.provider;

import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.CUSTOM_JOB;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.api.ClusterSecurityService;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.rotation.ExitCriteriaProvider;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.rotation.secret.custom.CustomJobRotationContext;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.freeipa.FreeipaClientService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.cloudbreak.view.ClusterView;

@ExtendWith(MockitoExtension.class)
public class AbstractCMIntermediateCacertRotationContextProviderTest {

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private ExitCriteriaProvider exitCriteriaProvider;

    @Mock
    private StackDtoService stackService;

    @Mock
    private ClusterApiConnectors clusterApiConnectors;

    @Mock
    private FreeipaClientService freeipaClientService;

    @InjectMocks
    private DatahubCMIntermediateCacertRotationContextProvider underTest;

    @Test
    void testGetContexts() {
        mockStack();

        Map<SecretRotationStep, RotationContext> contexts = underTest.getContexts("crn");

        assertEquals(2, contexts.size());
    }

    @Test
    void testCertCheck() throws CloudbreakException {
        String rootCert = FileReaderUtils.readFileFromClasspathQuietly("rotation/root.pem");
        String cmcaCert = FileReaderUtils.readFileFromClasspathQuietly("rotation/cmca.pem");

        ClusterApi clusterApi = mock(ClusterApi.class);
        ClusterSecurityService securityService = mock(ClusterSecurityService.class);
        when(clusterApiConnectors.getConnector(any(StackDto.class))).thenReturn(clusterApi);
        when(clusterApi.clusterSecurityService()).thenReturn(securityService);
        when(securityService.getTrustStore()).thenReturn(cmcaCert);
        when(freeipaClientService.getRootCertificateByEnvironmentCrn(any())).thenReturn(rootCert);
        mockStack();

        ((CustomJobRotationContext) underTest.getContexts("crn").get(CUSTOM_JOB)).getPostValidateJob().ifPresent(Runnable::run);
    }

    @Test
    void testCertCheckIfNotIssuedByRoot() throws CloudbreakException {
        String rootCert = FileReaderUtils.readFileFromClasspathQuietly("rotation/root.pem");
        String otherCert = FileReaderUtils.readFileFromClasspathQuietly("rotation/other.pem");

        ClusterApi clusterApi = mock(ClusterApi.class);
        ClusterSecurityService securityService = mock(ClusterSecurityService.class);
        when(clusterApiConnectors.getConnector(any(StackDto.class))).thenReturn(clusterApi);
        when(clusterApi.clusterSecurityService()).thenReturn(securityService);
        when(securityService.getTrustStore()).thenReturn(otherCert);
        when(freeipaClientService.getRootCertificateByEnvironmentCrn(any())).thenReturn(rootCert);
        mockStack();

        assertThrows(SecretRotationException.class, () ->
                ((CustomJobRotationContext) underTest.getContexts("crn").get(CUSTOM_JOB)).getPostValidateJob().ifPresent(Runnable::run));
    }

    private void mockStack() {
        StackDto stackDto = mock(StackDto.class);
        ClusterView clusterView = mock(ClusterView.class);
        when(stackDto.getResourceCrn()).thenReturn("crn");
        when(stackDto.getCluster()).thenReturn(clusterView);
        lenient().when(stackDto.getResourceName()).thenReturn("examplestack");
        when(clusterView.getAutoTlsEnabled()).thenReturn(Boolean.TRUE);
        when(stackService.getByCrn(any())).thenReturn(stackDto);
        GatewayConfig gatewayConfig = mock(GatewayConfig.class);
        when(gatewayConfig.getHostname()).thenReturn("host");
        when(gatewayConfigService.getPrimaryGatewayConfig(any())).thenReturn(gatewayConfig);
    }
}
