package com.sequenceiq.cloudbreak.rotation;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.api.ClusterSecurityService;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.freeipa.FreeipaClientService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@ExtendWith(MockitoExtension.class)
public class CMCAValidationServiceTest {

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private FreeipaClientService freeipaClientService;

    @Mock
    private ClusterApiConnectors clusterApiConnectors;

    @InjectMocks
    private CMCAValidationService underTest;

    @Test
    void testCertCheck() throws Exception {
        String rootCert = FileReaderUtils.readFileFromClasspathQuietly("rotation/root.pem");
        String cmcaCert = FileReaderUtils.readFileFromClasspathQuietly("rotation/cmca.pem");

        mockCmcaRotation(rootCert, cmcaCert);

        underTest.checkCMCAWithRootCert(1L);
    }

    @Test
    void testCertCheckIfNotIssuedByRoot() throws Exception {
        String rootCert = FileReaderUtils.readFileFromClasspathQuietly("rotation/root.pem");
        String otherCert = FileReaderUtils.readFileFromClasspathQuietly("rotation/other.pem");

        mockCmcaRotation(rootCert, otherCert);

        assertThrows(CloudbreakServiceException.class, () -> underTest.checkCMCAWithRootCert(1L));
    }

    private void mockCmcaRotation(String rootCert, String cmcaCert) throws Exception {
        ClusterApi clusterApi = mock(ClusterApi.class);
        ClusterSecurityService securityService = mock(ClusterSecurityService.class);
        when(clusterApiConnectors.getConnector(any(StackDto.class))).thenReturn(clusterApi);
        when(clusterApi.clusterSecurityService()).thenReturn(securityService);
        when(securityService.getTrustStore()).thenReturn(cmcaCert);
        when(freeipaClientService.getRootCertificateByEnvironmentCrn(any())).thenReturn(rootCert);
        StackDto stackDto = mock(StackDto.class);
        when(stackDto.getResourceName()).thenReturn("examplestack");
        when(stackDtoService.getById(any())).thenReturn(stackDto);
    }
}
