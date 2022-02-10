package com.sequenceiq.freeipa.service.image.userdata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.freemarker.FreeMarkerConfigurationFactoryBean;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.ccm.cloudinit.CcmConnectivityParameters;
import com.sequenceiq.cloudbreak.ccm.cloudinit.CcmParameters;
import com.sequenceiq.cloudbreak.ccm.cloudinit.CcmV2JumpgateParameters;
import com.sequenceiq.cloudbreak.ccm.cloudinit.CcmV2Parameters;
import com.sequenceiq.cloudbreak.ccm.cloudinit.DefaultCcmParameters;
import com.sequenceiq.cloudbreak.ccm.cloudinit.DefaultCcmV2JumpgateParameters;
import com.sequenceiq.cloudbreak.ccm.cloudinit.DefaultCcmV2Parameters;
import com.sequenceiq.cloudbreak.ccm.cloudinit.DefaultInstanceParameters;
import com.sequenceiq.cloudbreak.ccm.cloudinit.DefaultServerParameters;
import com.sequenceiq.cloudbreak.ccm.cloudinit.DefaultTunnelParameters;
import com.sequenceiq.cloudbreak.ccm.endpoint.BaseServiceEndpoint;
import com.sequenceiq.cloudbreak.ccm.endpoint.HostEndpoint;
import com.sequenceiq.cloudbreak.ccm.endpoint.KnownServiceIdentifier;
import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.ScriptParams;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.cloudbreak.util.FreeMarkerTemplateUtils;
import com.sequenceiq.common.api.type.CcmV2TlsType;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;

@ExtendWith(MockitoExtension.class)
class UserDataBuilderTest {

    private static final String ACCOUNT_ID = "accountId";

    @Spy
    private FreeMarkerTemplateUtils freeMarkerTemplateUtils;

    @Mock
    private EntitlementService entitlementService;

    @InjectMocks
    private UserDataBuilder underTest;

    private DetailedEnvironmentResponse environment;

    @BeforeEach
    void setup() throws IOException, TemplateException {
        FreeMarkerConfigurationFactoryBean factoryBean = new FreeMarkerConfigurationFactoryBean();
        factoryBean.setPreferFileSystemAccess(false);
        factoryBean.setTemplateLoaderPath("classpath:/");
        factoryBean.afterPropertiesSet();
        Configuration configuration = factoryBean.getObject();
        underTest.setFreemarkerConfiguration(configuration);

        UserDataBuilderParams params = new UserDataBuilderParams();
        params.setCustomData("date >> /tmp/time.txt");

        ReflectionTestUtils.setField(underTest, "userDataBuilderParams", params);
        environment = new DetailedEnvironmentResponse();
        environment.setCrn("environmentCrn");
    }

    @Test
    @DisplayName("test if CCM parameters are passed the user data contains them")
    void testBuildUserDataWithCCMParams() throws IOException {
        BaseServiceEndpoint serviceEndpoint = new BaseServiceEndpoint(new HostEndpoint("ccm.cloudera.com"));
        DefaultServerParameters serverParameters = new DefaultServerParameters(serviceEndpoint, "pub-key", "mina-id");
        DefaultInstanceParameters instanceParameters = new DefaultInstanceParameters("tunnel-id", "key-id", "private-key");
        DefaultTunnelParameters nginxTunnel = new DefaultTunnelParameters(KnownServiceIdentifier.GATEWAY, 9443);
        CcmParameters ccmParameters = new DefaultCcmParameters(serverParameters, instanceParameters, List.of(nginxTunnel));
        CcmConnectivityParameters ccmConnectivityParameters = new CcmConnectivityParameters(ccmParameters);

        PlatformParameters platformParameters = mock(PlatformParameters.class);
        ScriptParams scriptParams = mock(ScriptParams.class);
        when(scriptParams.getDiskPrefix()).thenReturn("sd");
        when(scriptParams.getStartLabel()).thenReturn(98);
        when(platformParameters.scriptParams()).thenReturn(scriptParams);

        String userData = underTest.buildUserData(ACCOUNT_ID, environment, Platform.platform("AZURE"), "priv-key".getBytes(),
                "cloudbreak", platformParameters, "pass", "cert", ccmConnectivityParameters, null);

        String expectedUserData = FileReaderUtils.readFileFromClasspath("azure-ccm-init.sh");
        assertEquals(expectedUserData, userData);
    }

    @Test
    @DisplayName("test if CCM V2 parameters are passed the user data contains them")
    void testBuildUserDataWithCCMV2Params() throws IOException {
        CcmV2Parameters ccmV2Parameters = new DefaultCcmV2Parameters("invertingProxyHost", "invertingProxyCertificate",
                "agentCrn", "agentKeyId", "agentEncipheredPrivateKey", "agentCertificate");
        CcmConnectivityParameters ccmConnectivityParameters = new CcmConnectivityParameters(ccmV2Parameters);

        PlatformParameters platformParameters = mock(PlatformParameters.class);
        ScriptParams scriptParams = mock(ScriptParams.class);
        when(scriptParams.getDiskPrefix()).thenReturn("sd");
        when(scriptParams.getStartLabel()).thenReturn(98);
        when(platformParameters.scriptParams()).thenReturn(scriptParams);

        String userData = underTest.buildUserData(ACCOUNT_ID, environment, Platform.platform("AZURE"), "priv-key".getBytes(),
                "cloudbreak", platformParameters, "pass", "cert", ccmConnectivityParameters, null);

        String expectedUserData = FileReaderUtils.readFileFromClasspath("azure-ccm-v2-init.sh");
        assertEquals(expectedUserData, userData);
    }

    static Stream<Arguments> tlsCases() {
        return Stream.of(
                Arguments.of(CcmV2TlsType.ONE_WAY_TLS, false, "azure-ccm-v2-jumpgate-onewaytls-init.sh"),
                Arguments.of(CcmV2TlsType.ONE_WAY_TLS, true, "azure-ccm-v2-jumpgate-onewaytls-init.sh"),
                Arguments.of(CcmV2TlsType.TWO_WAY_TLS, false, "azure-ccm-v2-jumpgate-twowaytls-init.sh"),
                Arguments.of(CcmV2TlsType.TWO_WAY_TLS, true, "azure-ccm-v2-jumpgate-twowaytls-init.sh"),
                Arguments.of(null, false, "azure-ccm-v2-jumpgate-twowaytls-init.sh"),
                Arguments.of(null, true, "azure-ccm-v2-jumpgate-onewaytls-init.sh")
        );
    }

    @ParameterizedTest(name = "Forced TLS mode = {0}, entitlement = {1}, filename = {2}")
    @MethodSource("tlsCases")
    @DisplayName("test if CCM V2 Jumpgate parameters are passed the user data contains them")
    void testBuildUserDataWithCCMV2JumpgateParams(CcmV2TlsType ccmV2TlsType,
            boolean oneWayTlsEntitlementEnabled, String expectedContentFileName) throws IOException {
        CcmV2JumpgateParameters ccmV2JumpgateParameters = new DefaultCcmV2JumpgateParameters("invertingProxyHost", "invertingProxyCertificate",
                "agentCrn", "agentKeyId", "agentEncipheredPrivateKey", "agentCertificate", "environmentCrn",
                "agentMachineUserAccessKeyId", "agentMachineUserEncipheredAccessKey");
        CcmConnectivityParameters ccmConnectivityParameters = new CcmConnectivityParameters(ccmV2JumpgateParameters);

        PlatformParameters platformParameters = mock(PlatformParameters.class);
        ScriptParams scriptParams = mock(ScriptParams.class);
        when(scriptParams.getDiskPrefix()).thenReturn("sd");
        when(scriptParams.getStartLabel()).thenReturn(98);
        when(platformParameters.scriptParams()).thenReturn(scriptParams);
        lenient().when(entitlementService.ccmV2UseOneWayTls(ACCOUNT_ID)).thenReturn(oneWayTlsEntitlementEnabled);
        environment.setCcmV2TlsType(ccmV2TlsType);
        String userData = underTest.buildUserData(ACCOUNT_ID, environment, Platform.platform("AZURE"), "priv-key".getBytes(),
                "cloudbreak", platformParameters, "pass", "cert", ccmConnectivityParameters, null);

        String expectedUserData = FileReaderUtils.readFileFromClasspath(expectedContentFileName);
        assertEquals(expectedUserData, userData);
    }

    @Test
    @DisplayName("test if NO CCM parameters are passed the user data does not contain them")
    void testBuildUserDataWithoutCCMParams() throws IOException {
        PlatformParameters platformParameters = mock(PlatformParameters.class);
        ScriptParams scriptParams = mock(ScriptParams.class);
        when(scriptParams.getDiskPrefix()).thenReturn("sd");
        when(scriptParams.getStartLabel()).thenReturn(98);
        when(platformParameters.scriptParams()).thenReturn(scriptParams);

        String userData = underTest.buildUserData(ACCOUNT_ID, environment, Platform.platform("AZURE"), "priv-key".getBytes(),
                "cloudbreak", platformParameters, "pass", "cert", new CcmConnectivityParameters(), null);

        String expectedUserData = FileReaderUtils.readFileFromClasspath("azure-init.sh");
        assertEquals(expectedUserData, userData);
    }
}
