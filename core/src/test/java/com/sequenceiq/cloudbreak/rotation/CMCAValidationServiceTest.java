package com.sequenceiq.cloudbreak.rotation;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

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

        mockCmcaRotation(rootCert, Optional.of(cmcaCert));

        underTest.checkCMCAWithRootCert(1L);

        verify(freeipaClientService).getRootCertificateByEnvironmentCrn(any());
    }

    @Test
    void testCertCheckIfNotIssuedByRoot() throws Exception {
        String rootCert = FileReaderUtils.readFileFromClasspathQuietly("rotation/root.pem");
        String otherCert = FileReaderUtils.readFileFromClasspathQuietly("rotation/other.pem");

        mockCmcaRotation(rootCert, Optional.of(otherCert));

        assertThrows(CloudbreakServiceException.class, () -> underTest.checkCMCAWithRootCert(1L));

        verify(freeipaClientService).getRootCertificateByEnvironmentCrn(any());
    }

    @Test
    void testCertCheckSkipping() throws Exception {
        String rootCert = FileReaderUtils.readFileFromClasspathQuietly("rotation/root.pem");

        mockCmcaRotation(rootCert, Optional.empty());

        underTest.checkCMCAWithRootCert(1L);

        verifyNoInteractions(freeipaClientService);
    }

    private void mockCmcaRotation(String rootCert, Optional<String> cmcaCert) throws Exception {
        ClusterApi clusterApi = mock(ClusterApi.class);
        ClusterSecurityService securityService = mock(ClusterSecurityService.class);
        when(clusterApiConnectors.getConnector(any(StackDto.class))).thenReturn(clusterApi);
        when(clusterApi.clusterSecurityService()).thenReturn(securityService);
        when(securityService.getTrustStoreForValidation()).thenReturn(cmcaCert);
        lenient().when(freeipaClientService.getRootCertificateByEnvironmentCrn(any())).thenReturn(rootCert);
        StackDto stackDto = mock(StackDto.class);
        lenient().when(stackDto.getResourceName()).thenReturn("examplestack");
        when(stackDtoService.getById(any())).thenReturn(stackDto);
    }
}
