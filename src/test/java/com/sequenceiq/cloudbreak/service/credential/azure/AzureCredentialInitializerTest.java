package com.sequenceiq.cloudbreak.service.credential.azure;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.domain.AzureCredential;

public class AzureCredentialInitializerTest {

    private AzureCredentialInitializer azureCredentialInitializer;

    @Before
    public void setUp() {
        azureCredentialInitializer = new AzureCredentialInitializer();
    }

    @Test
    public void validCertificateFileWillNotFail() {
        azureCredentialInitializer.init(
                azureCredential(
                        "-----BEGIN CERTIFICATE-----\n"
                                + "MIICsDCCAhmgAwIBAgIJAPtq+czPZYU/MA0GCSqGSIb3DQEBBQUAMEUxCzAJBgNV\n"
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
                                + "TA0+meQcD7qPGKxxijqwU5Y1QTw=\n"
                                + "-----END CERTIFICATE-----")
        );
    }


    @Test(expected = BadRequestException.class)
    public void inValidCertificateFileWhenKeyIsMissingWillFail() {
        azureCredentialInitializer.init(
                azureCredential(
                        "-----BEGIN CERTIFICATE-----\n"
                                + "-----END CERTIFICATE-----")
        );
    }


    @Test(expected = BadRequestException.class)
    public void inValidCertificateFileWhenKeyIsNotValidTextWillFail() {
        azureCredentialInitializer.init(
                azureCredential(
                        "-----BEGIN CERTIFICATE-----\n"
                                + "key"
                                + "-----END CERTIFICATE-----")
        );
    }

    @Test(expected = BadRequestException.class)
    public void inValidCertificateFileWhenHeaderIsNotIncludedIsMissingWillFail() {
        azureCredentialInitializer.init(
                azureCredential(
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
                                + "TA0+meQcD7qPGKxxijqwU5Y1QTw=\n"
                                + "-----END CERTIFICATE-----"));
    }

    private AzureCredential azureCredential(String publicKey) {
        AzureCredential azureCredential = new AzureCredential();
        azureCredential.setPublicKey(publicKey);
        return azureCredential;
    }

}