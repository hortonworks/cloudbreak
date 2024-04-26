package com.sequenceiq.freeipa.service.image.userdata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.Map;
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
import com.sequenceiq.cloudbreak.cloud.model.encryption.EncryptionKeySource;
import com.sequenceiq.cloudbreak.cloud.model.encryption.EncryptionKeyType;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.dto.ProxyAuthentication;
import com.sequenceiq.cloudbreak.dto.ProxyConfig;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.cloudbreak.util.FreeMarkerTemplateUtils;
import com.sequenceiq.common.api.type.CcmV2TlsType;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.freeipa.encryption.EncryptionUtil;
import com.sequenceiq.freeipa.entity.StackEncryption;
import com.sequenceiq.freeipa.service.StackEncryptionService;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;

@ExtendWith(MockitoExtension.class)
class UserDataBuilderTest {

    private static final String ACCOUNT_ID = "accountId";

    private static final Long STACK_ID = 1L;

    @Spy
    private FreeMarkerTemplateUtils freeMarkerTemplateUtils;

    @Mock
    private CcmV2TlsTypeDecider ccmV2TlsTypeDecider;

    @Mock
    private EncryptionUtil encryptionUtil;

    @Mock
    private StackEncryptionService stackEncryptionService;

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
        params.setCustomUserData("date >> /tmp/time.txt");
        params.setUserDataSecrets(Map.of("saltBootPassword", "SALT_BOOT_PASSWORD"));

        ReflectionTestUtils.setField(underTest, "userDataBuilderParams", params);
        environment = DetailedEnvironmentResponse.builder()
                .withCrn("environmentCrn")
                .build();
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
        ProxyAuthentication proxyAuthentication = ProxyAuthentication.builder()
                .withUserName("user")
                .withPassword("pwd")
                .build();
        ProxyConfig proxyConfig = ProxyConfig.builder()
                .withServerHost("proxy.host")
                .withServerPort(1234)
                .withProxyAuthentication(proxyAuthentication)
                .withNoProxyHosts("noproxy.com")
                .withProtocol("https")
                .build();
        PlatformParameters platformParameters = mock(PlatformParameters.class);
        ScriptParams scriptParams = mock(ScriptParams.class);
        when(scriptParams.getDiskPrefix()).thenReturn("sd");
        when(scriptParams.getStartLabel()).thenReturn(98);
        when(platformParameters.scriptParams()).thenReturn(scriptParams);

        String userData = underTest.buildUserData(ACCOUNT_ID, environment, Platform.platform("AZURE"), "priv-key".getBytes(),
                "cloudbreak", platformParameters, "pass", "cert", ccmConnectivityParameters, proxyConfig, STACK_ID);

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
                "cloudbreak", platformParameters, "pass", "cert", ccmConnectivityParameters, null, STACK_ID);

        String expectedUserData = FileReaderUtils.readFileFromClasspath("azure-ccm-v2-init.sh");
        assertEquals(expectedUserData, userData);
    }

    static Stream<Arguments> tlsCases() {
        return Stream.of(
                Arguments.of(CcmV2TlsType.ONE_WAY_TLS, "azure-ccm-v2-jumpgate-onewaytls-init.sh"),
                Arguments.of(CcmV2TlsType.TWO_WAY_TLS, "azure-ccm-v2-jumpgate-twowaytls-init.sh")
        );
    }

    @ParameterizedTest(name = "TLS mode = {0}, filename = {1}")
    @MethodSource("tlsCases")
    @DisplayName("test if CCM V2 Jumpgate parameters are passed the user data contains them")
    void testBuildUserDataWithCCMV2JumpgateParams(CcmV2TlsType ccmV2TlsType, String expectedContentFileName) throws IOException {
        CcmV2JumpgateParameters ccmV2JumpgateParameters = new DefaultCcmV2JumpgateParameters("invertingProxyHost", "invertingProxyCertificate",
                "agentCrn", "agentKeyId", "agentEncipheredPrivateKey", "agentCertificate", "environmentCrn",
                "agentMachineUserAccessKeyId", "agentMachineUserEncipheredAccessKey", "hmacKey", "initialisationVector", "hmacForPrivateKey");
        CcmConnectivityParameters ccmConnectivityParameters = new CcmConnectivityParameters(ccmV2JumpgateParameters);

        PlatformParameters platformParameters = mock(PlatformParameters.class);
        ScriptParams scriptParams = mock(ScriptParams.class);
        when(scriptParams.getDiskPrefix()).thenReturn("sd");
        when(scriptParams.getStartLabel()).thenReturn(98);
        when(platformParameters.scriptParams()).thenReturn(scriptParams);
        lenient().when(ccmV2TlsTypeDecider.decide(environment)).thenReturn(ccmV2TlsType);
        environment.setCcmV2TlsType(ccmV2TlsType);
        environment.setAccountId(ACCOUNT_ID);
        String userData = underTest.buildUserData(ACCOUNT_ID, environment, Platform.platform("AZURE"), "priv-key".getBytes(),
                "cloudbreak", platformParameters, "pass", "cert", ccmConnectivityParameters, null, STACK_ID);

        String expectedUserData = FileReaderUtils.readFileFromClasspath(expectedContentFileName);
        assertEquals(expectedUserData, userData);
    }

    @Test
    @DisplayName("test if NO CCM parameters are passed the user data does not contain them. Should also pass the API Endpoint URL.")
    void testBuildUserDataWithoutCCMParams() throws IOException {
        ReflectionTestUtils.setField(underTest, "cdpApiEndpointUrl", "endpointUrl");
        PlatformParameters platformParameters = mock(PlatformParameters.class);
        ScriptParams scriptParams = mock(ScriptParams.class);
        when(scriptParams.getDiskPrefix()).thenReturn("sd");
        when(scriptParams.getStartLabel()).thenReturn(98);
        when(platformParameters.scriptParams()).thenReturn(scriptParams);

        String userData = underTest.buildUserData(ACCOUNT_ID, environment, Platform.platform("AZURE"), "priv-key".getBytes(),
                "cloudbreak", platformParameters, "pass", "cert", new CcmConnectivityParameters(), null,
                STACK_ID);

        String expectedUserData = FileReaderUtils.readFileFromClasspath("azure-init.sh");
        assertEquals(expectedUserData, userData);
    }

    @Test
    @DisplayName("test if secret encryption is enabled, then it is reflected in the user data (boolean and key arn are passed, secrets are encrypted)")
    void testBuildUserDataWithSecretEncryptionEnabled() {
        environment.setEnableSecretEncryption(true);
        environment.setCloudPlatform(CloudPlatform.AWS.name());
        PlatformParameters platformParameters = mock(PlatformParameters.class);
        ScriptParams scriptParams = mock(ScriptParams.class);
        when(platformParameters.scriptParams()).thenReturn(scriptParams);
        when(scriptParams.getDiskPrefix()).thenReturn("sd");
        when(scriptParams.getStartLabel()).thenReturn(98);
        StackEncryption stackEncryption = mock(StackEncryption.class);
        when(stackEncryption.getEncryptionKeyLuks()).thenReturn("keyArn");
        when(stackEncryptionService.getStackEncryption(STACK_ID)).thenReturn(stackEncryption);
        EncryptionKeySource encryptionKeySource = EncryptionKeySource.builder()
                .withKeyType(EncryptionKeyType.AWS_KMS_KEY_ARN)
                .withKeyValue("keyArn")
                .build();
        when(encryptionUtil.getEncryptionKeySource(CloudPlatform.AWS, "keyArn")).thenReturn(encryptionKeySource);
        when(encryptionUtil.encrypt(CloudPlatform.AWS, "pass", environment, "SALT_BOOT_PASSWORD", encryptionKeySource))
                .thenReturn("encrypted-pass");

        String userData = underTest.buildUserData(ACCOUNT_ID, environment, Platform.platform("AZURE"), "priv-key".getBytes(),
                "cloudbreak", platformParameters, "pass", "cert", new CcmConnectivityParameters(), null, STACK_ID);

        verify(encryptionUtil, times(1))
                .encrypt(eq(CloudPlatform.AWS), eq("pass"), eq(environment), eq("SALT_BOOT_PASSWORD"), eq(encryptionKeySource));
        assertTrue(userData.contains("ENVIRONMENT_CRN=\"environmentCrn\""));
        assertTrue(userData.contains("SALT_BOOT_PASSWORD=encrypted-pass"));
        assertTrue(userData.contains("SECRET_ENCRYPTION_ENABLED=true"));
        assertTrue(userData.contains("SECRET_ENCRYPTION_KEY_SOURCE=\"keyArn\""));
    }

    @Test
    @DisplayName("test if secret encryption is disabled, then user data does not contain secret encryption related variables, and secrets are not encrypted")
    void testBuildUserDataWithSecretEncryptionDisabled() {
        environment.setEnableSecretEncryption(false);
        PlatformParameters platformParameters = mock(PlatformParameters.class);
        ScriptParams scriptParams = mock(ScriptParams.class);
        when(platformParameters.scriptParams()).thenReturn(scriptParams);
        when(scriptParams.getDiskPrefix()).thenReturn("sd");
        when(scriptParams.getStartLabel()).thenReturn(98);

        String userData = underTest.buildUserData(ACCOUNT_ID, environment, Platform.platform("AZURE"), "priv-key".getBytes(),
                "cloudbreak", platformParameters, "pass", "cert", new CcmConnectivityParameters(), null,
                STACK_ID);

        verify(encryptionUtil, never()).encrypt(any(), any(), any(), any(), any());
        assertTrue(userData.contains("SALT_BOOT_PASSWORD=pass"));
        assertFalse(userData.contains("SECRET_ENCRYPTION_ENABLED"));
        assertFalse(userData.contains("SECRET_ENCRYPTION_KEY_SOURCE"));
    }

}
