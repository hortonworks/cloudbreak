package com.sequenceiq.cloudbreak.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.security.KeyPair;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.clusterproxy.ReadConfigResponse;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.core.flow2.cluster.provision.service.ClusterProxyService;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;

@ExtendWith(MockitoExtension.class)
class ClusterProxyRotationServiceTest {

    private static final String SECRET_FIELD = "secretField";

    private static final String SECRET_PATH = "cluster-proxy/secretPath";

    private static final String ALGORITHM = "RSA";

    // CHECKSTYLE:OFF
    private static final String JWK = "{\"kty\":\"RSA\",\"use\":\"sig\",\"kid\":\"ebba970f-7ffc-47f6-bc4a-85e7d8133c31\"," +
            "\"n\":\"goweyu8gcx5PGRL5hwkNK55Xuw44mEj0zspDEUK5RHPPmt-w8YeFgiokR31USjQ_P6dKwW6imC-MCnrGPMBe6h03glySs_nnq_lw1GBwIXihgOC7NdwQ6yWTGifjhuJB8os00KDecNC0PnqvFdMQrrAyH0k7lbqxLfuymG5Nl1-YpHxizuApWEFAXWVhaSB7vAtYLlurdHcPMYc4OgGWUW56ZV1ujPzMxkSZ2lHKQKCzuIylx9-OgBANZAhk4yVe_3LU8jd-pVad0h5Zy6ccRX2biTL9CR8RlWxzRTucrShYDIwWZ90CDtuv5plwdw_1Y8CcyYmSGuAfMxouUupy6w\"," +
            "\"e\":\"AQAB\"," +
            "\"d\":\"LOOPhwo4pzzpx95tbNBg9fmpgwl9zmdvvldwPlQqLm6AGWoHhtWpw7tH5EklHarfgLy-iMrJH_lhdw0Gc0dWm7vWJX49d6Nb1Redg3lxtSCssAsWdIuqSaEO9vR2WSAduw_A5mgadMJe6Aj5gVClMNwnL_jXg0HBwOUGYUG0bnMQfAFeEr_utGt849MX6wGmauVqSoU1eC0dpK4Tv66bs9bxduwYkzMsmPkoj3T9w17TA2sfMqk6yMn8MlTQaXC6Ih2l0ClsDXLdeE-20pnabGv8G3I1GtDauvShq4NvcVtPEyeUjuI2NWSzfAohYw3EMxxHlJWFYUWbF06JDYWiSQ\"," +
            "\"p\":\"5sinf_1vOSJby3oScwthDO4nmfnKD11oNMabEJjAUrWICEe6gIKTk-_7zuyIHaNzx2qAesyIFend8hve-Z0IAyw9Uj__QkssPjm7UxmTqe95BoiJN5nCCdEJgVD-L4nVCH6DynjIJUQYJmz243sOv1yh_pcJwDHraH9syBa7GQ8\"," +
            "\"q\":\"kM-2R-7gCPXPz-PnTApemC7TypDdLWGnN4zV6CcBOQn7z4gNMq-55Ha_pb2ARiz_xg15o2PZjPGc9buuvoGprBYCSeLt29OslXln3TGNKJVELmeSH-Uw3g4TTb34taP7QPDBcmfDNHlNMFXX-PLhfaHIMZuIn9wVx2EmgV-tcGU\"," +
            "\"dp\":\"v2AhVkeJFe3iBSfjaLdE8X4EJPE4l-kzYqBXMjoZMf0LIyKoC608R-839u26KinC9tjgVfSJ3PIkdIKcMo3_ePIUn8ImIIlJ1Qf2yQBqrP7Cc6KBdzPv7kgEA9JwFmfL2tFe5GWoQw7mn-a4DQecwR9FzmLdWR2MQwl34azH1g0\"," +
            "\"dq\":\"fugpnNyxJekWV-AoobEsuT3AifNuggjknIAmLi-QmMYxEezvdA4gYSHYTo3GTrm85XqFsTUeQLC1l30FZRsI8TDQjKP9Q_s7cjtVVLnKNViqyegmXSFkNBYtwKHNxpGQ7ZaQUYyxu9jaVpPEk_12s70GTsM5dq9Pvs1bC2IoAyk\"," +
            "\"qi\":\"KKcPlXMsBHLUpKLnaPXZKjQ2GqoLKdz8Xo2haNSFw47Am0fQlXczhPl_Zx8yijUDCa_dNOLZY3CQZmjqBWUXmg9KK4ZASAQGwg82QGzWUNn89eTNYkFrEEAfzV5pnoUiICyJRekZ6IK-ch7GI2Y2j_f63Zw_kjE0H4KyCd1mzaM\"}";
    // CHECKSTYLE:ON

    @Mock
    private ReadConfigResponse readConfigResponse;

    @Mock
    private SecretService secretService;

    @Mock
    private ClusterProxyService clusterProxyService;

    @InjectMocks
    private ClusterProxyRotationService underTest;

    @Test
    void readClusterProxyTokenKeysShouldConvertJwkToPem() {
        when(readConfigResponse.getKnoxSecretRef()).thenReturn(SECRET_PATH + ":" + SECRET_FIELD);
        ArgumentCaptor<String> secretPathArgumentCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> secretFieldArgumentCaptor = ArgumentCaptor.forClass(String.class);
        when(secretService.getKvV2SecretByPathAndField(secretPathArgumentCaptor.capture(), secretFieldArgumentCaptor.capture())).thenReturn(JWK);

        KeyPair keyPair = underTest.readClusterProxyTokenKeys(readConfigResponse);
        String secretField = secretFieldArgumentCaptor.getValue();
        assertEquals(SECRET_FIELD, secretField);
        String secretPath = secretPathArgumentCaptor.getValue();
        assertEquals(SECRET_PATH, secretPath);

        assertEquals(ALGORITHM, keyPair.getPublic().getAlgorithm());
    }

    @Test
    void readClusterProxyTokenKeysShouldThrowExceptionWrongPath() {
        when(readConfigResponse.getKnoxSecretRef()).thenReturn("illegalPath" + ":" + SECRET_FIELD);

        CloudbreakServiceException e = assertThrows(CloudbreakServiceException.class,
                () -> underTest.readClusterProxyTokenKeys(readConfigResponse));
        assertEquals("Cannot read JWK format token keys from cluster-proxy.", e.getMessage());
        assertEquals("Cannot read jwk from cluster-proxy, not a cluster-proxy vault path. Path: 'illegalPath'", e.getCause().getMessage());
    }

    @Test
    void readClusterProxyTokenKeysShouldThrowExceptionNoField() {
        when(readConfigResponse.getKnoxSecretRef()).thenReturn(SECRET_PATH);

        CloudbreakServiceException e = assertThrows(CloudbreakServiceException.class,
                () -> underTest.readClusterProxyTokenKeys(readConfigResponse));
        assertEquals("Cannot read JWK format token keys from cluster-proxy.", e.getMessage());
        assertEquals("Cannot read jwk from cluster-proxy, secret path invalid.", e.getCause().getMessage());
    }

    @Test
    void readClusterProxyTokenKeysShouldThrowExceptionBadJwk() {
        when(readConfigResponse.getKnoxSecretRef()).thenReturn(SECRET_PATH + ":" + SECRET_FIELD);
        when(secretService.getKvV2SecretByPathAndField(any(), any())).thenReturn("badjwk");
        CloudbreakServiceException e = assertThrows(CloudbreakServiceException.class,
                () -> underTest.readClusterProxyTokenKeys(readConfigResponse));
        assertEquals("Cannot read JWK format token keys from cluster-proxy.", e.getMessage());
        assertEquals("JWK key from cluster-proxy cannot converted to PEM, key elements missing.", e.getCause().getMessage());
    }

    @Test
    void generateTokenCert() {
        TokenCertInfo keyAndCert = underTest.generateTokenCert();
        assertNotNull(keyAndCert.base64DerCert());
        assertNotNull(keyAndCert.privateKey());
        assertNotNull(keyAndCert.publicKey());
        assertFalse(keyAndCert.toString().contains("privateKey"));
        assertTrue(keyAndCert.toString().contains(("publicKey")));
    }

    @Test
    void generateClusterProxySecretFormat() {
        String secretJson = "{\"enginePath\":\"secret\"," +
                "\"engineClass\":\"com.sequenceiq.cloudbreak.service.secret.vault.VaultKvV2Engine\"," +
                "\"path\":\"cb/secretPath\"}";
        String clusterProxySecretFormat = underTest.generateClusterProxySecretFormat(secretJson);

        assertEquals("cb/secretPath:secret", clusterProxySecretFormat);
    }

}