package com.sequenceiq.cloudbreak.core.bootstrap.service.host.decorator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import org.assertj.core.api.MapAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.sdx.common.PlatformAwareSdxConnector;
import com.sequenceiq.cloudbreak.util.CertProcessor;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@ExtendWith(MockitoExtension.class)
class JavaPillarDecoratorTest {

    private static final int JAVA_VERSION = 11;

    @InjectMocks
    private JavaPillarDecorator underTest;

    @Mock
    private StackDto stackDto;

    @Mock
    private Stack stack;

    @Mock
    private PlatformAwareSdxConnector sdxConnector;

    @Spy
    private CertProcessor certProcessor;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(underTest, "cryptoComplyPath", "ccj-path");
        ReflectionTestUtils.setField(underTest, "cryptoComplyHash", "ccj-hash");
        ReflectionTestUtils.setField(underTest, "bouncyCastleTlsPath", "bctls-path");
        ReflectionTestUtils.setField(underTest, "bouncyCastleTlsHash", "bctls-hash");

        when(stackDto.isOnGovPlatformVariant()).thenReturn(true);
        when(stackDto.getStack()).thenReturn(stack);
        when(stack.getJavaVersion()).thenReturn(JAVA_VERSION);
    }

    @Test
    void noJavaVersion() {
        when(stack.getJavaVersion()).thenReturn(null);

        Map<String, SaltPillarProperties> result = underTest.createJavaPillars(stackDto, new DetailedEnvironmentResponse());

        assertThatJavaProperties(result).doesNotContainKey("version");
    }

    @Test
    void javaVersion() {
        Map<String, SaltPillarProperties> result = underTest.createJavaPillars(stackDto, new DetailedEnvironmentResponse());

        assertThatJavaProperties(result).containsEntry("version", JAVA_VERSION);
    }

    @ParameterizedTest
    @ValueSource(strings = {"cryptoComplyPath", "cryptoComplyHash", "bouncyCastleTlsPath", "bouncyCastleTlsHash"})
    void missingSafeLogicPropertyForNonGovStack(String property) {
        when(stackDto.isOnGovPlatformVariant()).thenReturn(false);
        ReflectionTestUtils.setField(underTest, property, null);

        assertThatCode(() -> underTest.createJavaPillars(stackDto, new DetailedEnvironmentResponse())).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {"cryptoComplyPath", "cryptoComplyHash", "bouncyCastleTlsPath", "bouncyCastleTlsHash"})
    void missingSafeLogicPropertyForGovStack(String property) {
        ReflectionTestUtils.setField(underTest, property, null);

        assertThatThrownBy(() -> underTest.createJavaPillars(stackDto, new DetailedEnvironmentResponse()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Required SafeLogic property is blank for application: " + property);
    }

    @Test
    void noSafeLogicPropertiesForNonGovStack() {
        when(stackDto.isOnGovPlatformVariant()).thenReturn(false);

        Map<String, SaltPillarProperties> result = underTest.createJavaPillars(stackDto, new DetailedEnvironmentResponse());

        assertThatJavaProperties(result).doesNotContainKey("safelogic");
    }

    @Test
    void safeLogicPropertiesForGovStack() {
        Map<String, SaltPillarProperties> result = underTest.createJavaPillars(stackDto, new DetailedEnvironmentResponse());

        assertThatSafeLogicProperties(result)
                .containsEntry("cryptoComplyPath", "ccj-path")
                .containsEntry("cryptoComplyHash", "ccj-hash")
                .containsEntry("bouncyCastleTlsPath", "bctls-path")
                .containsEntry("bouncyCastleTlsHash", "bctls-hash");
    }

    @Test
    void testCertificatePillar() {
        DetailedEnvironmentResponse detailedEnvironmentResponse = new DetailedEnvironmentResponse();
        detailedEnvironmentResponse.setEnvironmentType("HYBRID");
        detailedEnvironmentResponse.setRemoteEnvironmentCrn("remoteEnvCrn");
        detailedEnvironmentResponse.setCrn("envCrn");
        when(stackDto.getType()).thenReturn(StackType.WORKLOAD);
        when(sdxConnector.getCACertsForEnvironment("envCrn")).thenReturn(Optional.of(
                """
                        -----BEGIN CERTIFICATE-----
                        MIICGTCCAZ+gAwIBAgIQCeCTZaz32ci5PhwLBCou8zAKBggqhkjOPQQDAzBOMQsw
                        CQYDVQQGEwJVUzEXMBUGA1UEChMORGlnaUNlcnQsIEluYy4xJjAkBgNVBAMTHURp
                        Z2lDZXJ0IFRMUyBFQ0MgUDM4NCBSb290IEc1MB4XDTIxMDExNTAwMDAwMFoXDTQ2
                        MDExNDIzNTk1OVowTjELMAkGA1UEBhMCVVMxFzAVBgNVBAoTDkRpZ2lDZXJ0LCBJ
                        bmMuMSYwJAYDVQQDEx1EaWdpQ2VydCBUTFMgRUNDIFAzODQgUm9vdCBHNTB2MBAG
                        ByqGSM49AgEGBSuBBAAiA2IABMFEoc8Rl1Ca3iOCNQfN0MsYndLxf3c1TzvdlHJS
                        7cI7+Oz6e2tYIOyZrsn8aLN1udsJ7MgT9U7GCh1mMEy7H0cKPGEQQil8pQgO4CLp
                        0zVozptjn4S1mU1YoI71VOeVyaNCMEAwHQYDVR0OBBYEFMFRRVBZqz7nLFr6ICIS
                        B4CIfBFqMA4GA1UdDwEB/wQEAwIBhjAPBgNVHRMBAf8EBTADAQH/MAoGCCqGSM49
                        BAMDA2gAMGUCMQCJao1H5+z8blUD2WdsJk6Dxv3J+ysTvLd6jLRl0mlpYxNjOyZQ
                        LgGheQaRnUi/wr4CMEfDFXuxoJGZSZOoPHzoRgaLLPIxAJSdYsiJvRmEFOml+wG4
                        DXZDjC5Ty3zfDBeWUA==
                        -----END CERTIFICATE-----
                        -----BEGIN CERTIFICATE-----
                        MIIC+TCCAoCgAwIBAgINAKaLeSkAAAAAUNCR+TAKBggqhkjOPQQDAzCBvzELMAkG
                        A1UEBhMCVVMxFjAUBgNVBAoTDUVudHJ1c3QsIEluYy4xKDAmBgNVBAsTH1NlZSB3
                        d3cuZW50cnVzdC5uZXQvbGVnYWwtdGVybXMxOTA3BgNVBAsTMChjKSAyMDEyIEVu
                        dHJ1c3QsIEluYy4gLSBmb3IgYXV0aG9yaXplZCB1c2Ugb25seTEzMDEGA1UEAxMq
                        RW50cnVzdCBSb290IENlcnRpZmljYXRpb24gQXV0aG9yaXR5IC0gRUMxMB4XDTEy
                        MTIxODE1MjUzNloXDTM3MTIxODE1NTUzNlowgb8xCzAJBgNVBAYTAlVTMRYwFAYD
                        VQQKEw1FbnRydXN0LCBJbmMuMSgwJgYDVQQLEx9TZWUgd3d3LmVudHJ1c3QubmV0
                        L2xlZ2FsLXRlcm1zMTkwNwYDVQQLEzAoYykgMjAxMiBFbnRydXN0LCBJbmMuIC0g
                        Zm9yIGF1dGhvcml6ZWQgdXNlIG9ubHkxMzAxBgNVBAMTKkVudHJ1c3QgUm9vdCBD
                        ZXJ0aWZpY2F0aW9uIEF1dGhvcml0eSAtIEVDMTB2MBAGByqGSM49AgEGBSuBBAAi
                        A2IABIQTydC6bUF74mzQ61VfZgIaJPRbiWlH47jCffHyAsWfoPZb1YsGGYZPUxBt
                        ByQnoaD41UcZYUx9ypMn6nQM72+WCf5j7HBdNq1nd67JnXxVRDqiY1Ef9eNi1KlH
                        Bz7MIKNCMEAwDgYDVR0PAQH/BAQDAgEGMA8GA1UdEwEB/wQFMAMBAf8wHQYDVR0O
                        BBYEFLdj5xrdjekIplWDpOBqUEFlEUJJMAoGCCqGSM49BAMDA2cAMGQCMGF52OVC
                        R98crlOZF7ZvHH3hvxGU0QOIdeSNiaSKd0bebWHvAvX7td/M/k7//qnmpwIwW5nX
                        hTcGtXsI/esni0qU+eH6p44mCOh8kmhtc9hvJqwhAriZtyZBWyVgrtBIGu4G
                        -----END CERTIFICATE-----
                        """));

        Map<String, SaltPillarProperties> result = underTest.createJavaPillars(stackDto, detailedEnvironmentResponse);

        Map<String, Object> properties = result.get("java").getProperties();
        Map<String, String> certificates = ((Map<String, Map<String, String>>) properties.get("java")).get("rootCertificates");
        assertEquals("""
                -----BEGIN CERTIFICATE-----
                MIICGTCCAZ+gAwIBAgIQCeCTZaz32ci5PhwLBCou8zAKBggqhkjOPQQDAzBOMQsw
                CQYDVQQGEwJVUzEXMBUGA1UEChMORGlnaUNlcnQsIEluYy4xJjAkBgNVBAMTHURp
                Z2lDZXJ0IFRMUyBFQ0MgUDM4NCBSb290IEc1MB4XDTIxMDExNTAwMDAwMFoXDTQ2
                MDExNDIzNTk1OVowTjELMAkGA1UEBhMCVVMxFzAVBgNVBAoTDkRpZ2lDZXJ0LCBJ
                bmMuMSYwJAYDVQQDEx1EaWdpQ2VydCBUTFMgRUNDIFAzODQgUm9vdCBHNTB2MBAG
                ByqGSM49AgEGBSuBBAAiA2IABMFEoc8Rl1Ca3iOCNQfN0MsYndLxf3c1TzvdlHJS
                7cI7+Oz6e2tYIOyZrsn8aLN1udsJ7MgT9U7GCh1mMEy7H0cKPGEQQil8pQgO4CLp
                0zVozptjn4S1mU1YoI71VOeVyaNCMEAwHQYDVR0OBBYEFMFRRVBZqz7nLFr6ICIS
                B4CIfBFqMA4GA1UdDwEB/wQEAwIBhjAPBgNVHRMBAf8EBTADAQH/MAoGCCqGSM49
                BAMDA2gAMGUCMQCJao1H5+z8blUD2WdsJk6Dxv3J+ysTvLd6jLRl0mlpYxNjOyZQ
                LgGheQaRnUi/wr4CMEfDFXuxoJGZSZOoPHzoRgaLLPIxAJSdYsiJvRmEFOml+wG4
                DXZDjC5Ty3zfDBeWUA==
                -----END CERTIFICATE-----
                """, certificates.get("018E13F0772532CF809BD1B17281867283FC48C6E13BE9C69812854A490C1B05"));
        assertEquals("""
                -----BEGIN CERTIFICATE-----
                MIIC+TCCAoCgAwIBAgINAKaLeSkAAAAAUNCR+TAKBggqhkjOPQQDAzCBvzELMAkG
                A1UEBhMCVVMxFjAUBgNVBAoTDUVudHJ1c3QsIEluYy4xKDAmBgNVBAsTH1NlZSB3
                d3cuZW50cnVzdC5uZXQvbGVnYWwtdGVybXMxOTA3BgNVBAsTMChjKSAyMDEyIEVu
                dHJ1c3QsIEluYy4gLSBmb3IgYXV0aG9yaXplZCB1c2Ugb25seTEzMDEGA1UEAxMq
                RW50cnVzdCBSb290IENlcnRpZmljYXRpb24gQXV0aG9yaXR5IC0gRUMxMB4XDTEy
                MTIxODE1MjUzNloXDTM3MTIxODE1NTUzNlowgb8xCzAJBgNVBAYTAlVTMRYwFAYD
                VQQKEw1FbnRydXN0LCBJbmMuMSgwJgYDVQQLEx9TZWUgd3d3LmVudHJ1c3QubmV0
                L2xlZ2FsLXRlcm1zMTkwNwYDVQQLEzAoYykgMjAxMiBFbnRydXN0LCBJbmMuIC0g
                Zm9yIGF1dGhvcml6ZWQgdXNlIG9ubHkxMzAxBgNVBAMTKkVudHJ1c3QgUm9vdCBD
                ZXJ0aWZpY2F0aW9uIEF1dGhvcml0eSAtIEVDMTB2MBAGByqGSM49AgEGBSuBBAAi
                A2IABIQTydC6bUF74mzQ61VfZgIaJPRbiWlH47jCffHyAsWfoPZb1YsGGYZPUxBt
                ByQnoaD41UcZYUx9ypMn6nQM72+WCf5j7HBdNq1nd67JnXxVRDqiY1Ef9eNi1KlH
                Bz7MIKNCMEAwDgYDVR0PAQH/BAQDAgEGMA8GA1UdEwEB/wQFMAMBAf8wHQYDVR0O
                BBYEFLdj5xrdjekIplWDpOBqUEFlEUJJMAoGCCqGSM49BAMDA2cAMGQCMGF52OVC
                R98crlOZF7ZvHH3hvxGU0QOIdeSNiaSKd0bebWHvAvX7td/M/k7//qnmpwIwW5nX
                hTcGtXsI/esni0qU+eH6p44mCOh8kmhtc9hvJqwhAriZtyZBWyVgrtBIGu4G
                -----END CERTIFICATE-----
                """, certificates.get("02ED0EB28C14DA45165C566791700D6451D7FB56F0B2AB1D3B8EB070E56EDFF5"));
        assertEquals(2, certificates.size());
    }

    private MapAssert<String, Object> assertThatJavaProperties(Map<String, SaltPillarProperties> result) {
        return assertThat((Map<String, Object>) result.get("java").getProperties().get("java"));
    }

    private MapAssert<String, Object> assertThatSafeLogicProperties(Map<String, SaltPillarProperties> result) {
        Map<String, Object> javaProperties = (Map<String, Object>) result.get("java").getProperties().get("java");
        return assertThat((Map<String, Object>) javaProperties.get("safelogic"));
    }

}
