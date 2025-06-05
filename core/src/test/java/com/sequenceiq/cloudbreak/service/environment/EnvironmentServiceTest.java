package com.sequenceiq.cloudbreak.service.environment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.tls.TlsSpecificationsHelper;
import com.sequenceiq.environment.api.v1.encryptionprofile.endpoint.EncryptionProfileEndpoint;
import com.sequenceiq.environment.api.v1.encryptionprofile.model.EncryptionProfileResponse;
import com.sequenceiq.environment.api.v1.environment.endpoint.EnvironmentEndpoint;

@ExtendWith(MockitoExtension.class)
public class EnvironmentServiceTest {

    private static final String ENV_CRN = "envCrn";

    @Mock
    private EnvironmentEndpoint environmentEndpoint;

    @Mock
    private EncryptionProfileEndpoint encryptionProfileEndpoint;

    @InjectMocks
    private EnvironmentService underTest;

    @Test()
    public void testGetTlsCipherSuitesDefault() {
        EncryptionProfileResponse encryptionProfileResponse = new EncryptionProfileResponse();
        Set<String> cipherSuites = Set.of(
                "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
                "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
                "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
                "TLS_DHE_RSA_WITH_AES_128_GCM_SHA256",
                "TLS_DHE_RSA_WITH_AES_256_GCM_SHA384",
                "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256",
                "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384",
                "TLS_DHE_RSA_WITH_AES_128_CBC_SHA256:",
                "TLS_DHE_RSA_WITH_AES_256_CBC_SHA256");
        encryptionProfileResponse.setCipherSuites(Map.of("TLSv1.2", cipherSuites));
        String result = underTest.getTlsCipherSuites(ENV_CRN, TlsSpecificationsHelper.CipherSuitesLimitType.DEFAULT);
        assertEquals("DHE-RSA-AES128-GCM-SHA256:DHE-RSA-AES128-SHA256:DHE-RSA-AES256-GCM-SHA384:DHE-RSA-AES256-SHA256:" +
                "ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-ECDSA-AES128-SHA:ECDHE-ECDSA-AES128-SHA256:ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-ECDSA-AES256-SHA:" +
                "ECDHE-ECDSA-AES256-SHA384:ECDHE-RSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-SHA:ECDHE-RSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-SHA", result);
    }

    @Test()
    public void testGetTlsCipherSuitesRedHatVersion8() {
        EncryptionProfileResponse encryptionProfileResponse = new EncryptionProfileResponse();
        Set<String> cipherSuites = Set.of(
                "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
                "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
                "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
                "TLS_DHE_RSA_WITH_AES_128_GCM_SHA256",
                "TLS_DHE_RSA_WITH_AES_256_GCM_SHA384",
                "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256",
                "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384",
                "TLS_DHE_RSA_WITH_AES_128_CBC_SHA256:",
                "TLS_DHE_RSA_WITH_AES_256_CBC_SHA256");
        encryptionProfileResponse.setCipherSuites(Map.of("TLSv1.2", cipherSuites));
        String result = underTest.getTlsCipherSuites(ENV_CRN, TlsSpecificationsHelper.CipherSuitesLimitType.REDHAT_VERSION8);
        assertEquals("DHE-RSA-AES128-GCM-SHA256:DHE-RSA-AES128-SHA256:DHE-RSA-AES256-GCM-SHA384:DHE-RSA-AES256-SHA256:" +
                "ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-ECDSA-AES128-SHA256:ECDHE-ECDSA-AES256-SHA384:ECDHE-RSA-AES128-GCM-SHA256:" +
                "ECDHE-RSA-AES256-GCM-SHA384", result);
    }

    @Test()
    public void testGetTlsCipherSuitesBlackboxExporter() {
        EncryptionProfileResponse encryptionProfileResponse = new EncryptionProfileResponse();
        Set<String> cipherSuites = Set.of(
                "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
                "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
                "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
                "TLS_DHE_RSA_WITH_AES_128_GCM_SHA256",
                "TLS_DHE_RSA_WITH_AES_256_GCM_SHA384",
                "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256",
                "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384",
                "TLS_DHE_RSA_WITH_AES_128_CBC_SHA256:",
                "TLS_DHE_RSA_WITH_AES_256_CBC_SHA256");
        encryptionProfileResponse.setCipherSuites(Map.of("TLSv1.2", cipherSuites));
        String result = underTest.getTlsCipherSuites(ENV_CRN, TlsSpecificationsHelper.CipherSuitesLimitType.BLACKBOX_EXPORTER);
        assertEquals("ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-ECDSA-AES128-SHA:ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-ECDSA-AES256-SHA:" +
                "ECDHE-RSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-SHA:ECDHE-RSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-SHA", result);
    }

    @Test()
    public void testGetTlsCipherSuitesJavaIntermediate() {
        EncryptionProfileResponse encryptionProfileResponse = new EncryptionProfileResponse();
        Set<String> cipherSuites = Set.of(
                "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
                "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
                "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
                "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
                "TLS_DHE_RSA_WITH_AES_128_GCM_SHA256",
                "TLS_DHE_RSA_WITH_AES_256_GCM_SHA384",
                "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256",
                "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA",
                "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA",
                "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384",
                "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA",
                "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA",
                "TLS_DHE_RSA_WITH_AES_128_CBC_SHA256",
                "TLS_DHE_RSA_WITH_AES_128_CBC_SHA",
                "TLS_DHE_RSA_WITH_AES_256_CBC_SHA256",
                "TLS_DHE_RSA_WITH_AES_256_CBC_SHA",
                "TLS_RSA_WITH_AES_128_CBC_SHA",
                "TLS_RSA_WITH_AES_256_CBC_SHA");
        encryptionProfileResponse.setCipherSuites(Map.of("TLSv1.2", cipherSuites));
        String result = underTest.getTlsCipherSuites(ENV_CRN, TlsSpecificationsHelper.CipherSuitesLimitType.JAVA_INTERMEDIATE2018, true);
        assertEquals("TLS_RSA_WITH_AES_128_CBC_SHA:TLS_RSA_WITH_AES_256_CBC_SHA:TLS_DHE_RSA_WITH_AES_128_GCM_SHA256:" +
                "TLS_DHE_RSA_WITH_AES_128_CBC_SHA:TLS_DHE_RSA_WITH_AES_128_CBC_SHA256:TLS_DHE_RSA_WITH_AES_256_GCM_SHA384:TLS_DHE_RSA_WITH_AES_256_CBC_SHA:" +
                "TLS_DHE_RSA_WITH_AES_256_CBC_SHA256:TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256:TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA:" +
                "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256:TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384:TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA:" +
                "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384:TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256:TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA:" +
                "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384:TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA", result);
    }

    @Test()
    public void testGetTlsCipherSuitesMinimal() {
        EncryptionProfileResponse encryptionProfileResponse = new EncryptionProfileResponse();
        Set<String> cipherSuites = Set.of(
                "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
                "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
                "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
                "TLS_DHE_RSA_WITH_AES_128_GCM_SHA256",
                "TLS_DHE_RSA_WITH_AES_256_GCM_SHA384",
                "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256",
                "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384",
                "TLS_DHE_RSA_WITH_AES_128_CBC_SHA256:",
                "TLS_DHE_RSA_WITH_AES_256_CBC_SHA256");
        encryptionProfileResponse.setCipherSuites(Map.of("TLSv1.2", cipherSuites));
        String result = underTest.getTlsCipherSuites(ENV_CRN, TlsSpecificationsHelper.CipherSuitesLimitType.MINIMAL);
        assertEquals("DHE-RSA-AES128-GCM-SHA256:DHE-RSA-AES128-SHA256:DHE-RSA-AES256-GCM-SHA384:DHE-RSA-AES256-SHA256:" +
                "ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-ECDSA-AES128-SHA256:ECDHE-ECDSA-AES256-SHA384:ECDHE-RSA-AES128-GCM-SHA256:" +
                "ECDHE-RSA-AES256-GCM-SHA384", result);
    }
}
