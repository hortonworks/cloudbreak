package com.sequenceiq.cloudbreak.service.credential.azure;

import static org.mockito.BDDMockito.given;

import java.io.IOException;
import java.security.GeneralSecurityException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.domain.AzureCredential;
import com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil;

public class AzureCredentialHandlerTest {

    private static final String AZURE_CERTIFICATE_HEADER = "-----BEGIN CERTIFICATE-----\n";
    private static final String AZURE_CERTIFICATE_FOOTER = "-----END CERTIFICATE-----";
    private static final String AZURE_CERTIFICATE_CONTENT =
                    "MIICsDCCAhmgAwIBAgIJAPtq+czPZYU/MA0GCSqGSIb3DQEBBQUAMEUxCzAJBgNV\n"
                    + "BAYTAkFVMRMwEQYDVQQIEwpTb21lLVN0YXRlMSEwHwYDVQQKExhJbnRlcm5ldCBX\n"
                    + "aWRnaXRzIFB0eSBMdGQwHhcNMTQwNTEzMDIxNDUwWhcNMTUwNTEzMDIxNDUwWjBF\n"
                    + "MQswCQYDVQQGEwJBVTETMBEGA1UECBMKU29tZS1TdGF0ZTEhMB8GA1UEChMYSW50\n"
                    + "ZXJuZXQgV2lkZ2l0cyBQdHkgTHRkMIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKB\n"
                    + "gQCvv6nBCp3wiqDVT0g1dEAJvfLiTU6oPVau9FCaNWrxJgkR697kuxMNhY4CpLXS\n"
                    + "DgmSh/guI4iN5pmQtJ5RJsVBZRHWEu7k+GdvSFkNJ/7+i1t2DOjNtnOxGQ6TpjZg\n"
                    + "lyDGNW2m2IY9iaaTzzwhowCcfMMwC+S0OzZ5AT3YE152XQIDAQABo4GnMIGkMB0G\n"
                    + "A1UdDgQWBBR/lhZljxO+cPl9EQmfSb2sndrKFDB1BgNVHSMEbjBsgBR/lhZljxO+\n"
                    + "cPl9EQmfSb2sndrKFKFJpEcwRTELMAkGA1UEBhMCQVUxEzARBgNVBAgTClNvbWUt\n"
                    + "U3RhdGUxITAfBgNVBAoTGEludGVybmV0IFdpZGdpdHMgUHR5IEx0ZIIJAPtq+czP\n"
                    + "ZYU/MAwGA1UdEwQFMAMBAf8wDQYJKoZIhvcNAQEFBQADgYEABYXu5HwJ8F9LyPrD\n"
                    + "HkUQUM6HRoybllBZWf0uwrM5Mey/pYwhouR1PNd2/y6OXt5mjzxLG/53YvidfrEG\n"
                    + "I5QW2HYwS3jZ2zlOLx5fj+wmeenxNrMxgP7XkbkVcBa76wdfZ1xBAr0ybXb13Gi2\n"
                    + "TA0+meQcD7qPGKxxijqwU5Y1QTw=\n";

    @InjectMocks
    private AzureCredentialHandler azureCredentialHandler;

    @Mock
    private AzureStackUtil azureStackUtil;

    @Before
    public void setUp() {
        azureCredentialHandler = new AzureCredentialHandler();
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void validCertificateFileWillNotFail() throws IOException, GeneralSecurityException {
        AzureCredential azureCredential = azureCredential(AZURE_CERTIFICATE_HEADER
                + AZURE_CERTIFICATE_CONTENT
                + AZURE_CERTIFICATE_FOOTER);

        given(azureStackUtil.generateAzureServiceFiles(azureCredential)).willReturn(azureCredential);
        given(azureStackUtil.generateAzureSshCerFile(azureCredential)).willReturn(azureCredential);
        azureCredentialHandler.init(azureCredential);
    }

    @Test
    public void validCertificateFileWithoutEndLinesWillNotFail() throws IOException, GeneralSecurityException {
        AzureCredential azureCredential = azureCredential((AZURE_CERTIFICATE_HEADER
                + AZURE_CERTIFICATE_CONTENT
                + AZURE_CERTIFICATE_FOOTER).replaceAll("\n", ""));

        given(azureStackUtil.generateAzureServiceFiles(azureCredential)).willReturn(azureCredential);
        given(azureStackUtil.generateAzureSshCerFile(azureCredential)).willReturn(azureCredential);
        azureCredentialHandler.init(azureCredential);

    }


    @Test(expected = BadRequestException.class)
    public void inValidCertificateFileWhenKeyIsMissingWillFail() throws IOException, GeneralSecurityException {
        AzureCredential azureCredential = azureCredential(AZURE_CERTIFICATE_HEADER + AZURE_CERTIFICATE_FOOTER);
        given(azureStackUtil.generateAzureServiceFiles(azureCredential)).willReturn(azureCredential);
        given(azureStackUtil.generateAzureSshCerFile(azureCredential)).willReturn(azureCredential);
        azureCredentialHandler.init(azureCredential);
    }


    @Test(expected = BadRequestException.class)
    public void inValidCertificateFileWhenKeyIsNotValidTextWillFail() throws IOException, GeneralSecurityException {
        AzureCredential azureCredential = azureCredential(
                AZURE_CERTIFICATE_HEADER + "key" + AZURE_CERTIFICATE_FOOTER);
        given(azureStackUtil.generateAzureServiceFiles(azureCredential)).willReturn(azureCredential);
        given(azureStackUtil.generateAzureSshCerFile(azureCredential)).willReturn(azureCredential);
        azureCredentialHandler.init(azureCredential);
    }

    @Test(expected = BadRequestException.class)
    public void inValidCertificateFileWhenHeaderIsNotIncludedIsMissingWillFail() throws IOException, GeneralSecurityException {
        AzureCredential azureCredential = azureCredential(AZURE_CERTIFICATE_CONTENT + AZURE_CERTIFICATE_FOOTER);
        given(azureStackUtil.generateAzureServiceFiles(azureCredential)).willReturn(azureCredential);
        given(azureStackUtil.generateAzureSshCerFile(azureCredential)).willReturn(azureCredential);
        azureCredentialHandler.init(azureCredential);
    }

    private AzureCredential azureCredential(String publicKey) {
        AzureCredential azureCredential = new AzureCredential();
        azureCredential.setPublicKey(publicKey);
        return azureCredential;
    }

}