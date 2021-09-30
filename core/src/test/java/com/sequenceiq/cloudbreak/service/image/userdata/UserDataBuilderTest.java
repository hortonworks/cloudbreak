package com.sequenceiq.cloudbreak.service.image.userdata;

import static com.sequenceiq.cloudbreak.cloud.model.Orchestrator.orchestrator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
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
import com.sequenceiq.cloudbreak.cloud.TagValidator;
import com.sequenceiq.cloudbreak.cloud.model.DiskType;
import com.sequenceiq.cloudbreak.cloud.model.DiskTypes;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.PlatformOrchestrator;
import com.sequenceiq.cloudbreak.cloud.model.ScriptParams;
import com.sequenceiq.cloudbreak.cloud.model.StackParamValidation;
import com.sequenceiq.cloudbreak.cloud.model.TagSpecification;
import com.sequenceiq.cloudbreak.cloud.model.VmRecommendations;
import com.sequenceiq.cloudbreak.common.type.OrchestratorConstants;
import com.sequenceiq.cloudbreak.dto.ProxyAuthentication;
import com.sequenceiq.cloudbreak.dto.ProxyConfig;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.cloudbreak.util.FreeMarkerTemplateUtils;
import com.sequenceiq.common.api.type.InstanceGroupType;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;

@RunWith(MockitoJUnitRunner.class)
public class UserDataBuilderTest {

    @Spy
    private FreeMarkerTemplateUtils freeMarkerTemplateUtils;

    @InjectMocks
    private UserDataBuilder underTest;

    @Before
    public void setup() throws IOException, TemplateException {
        FreeMarkerConfigurationFactoryBean factoryBean = new FreeMarkerConfigurationFactoryBean();
        factoryBean.setPreferFileSystemAccess(false);
        factoryBean.setTemplateLoaderPath("classpath:/");
        factoryBean.afterPropertiesSet();
        Configuration configuration = factoryBean.getObject();
        underTest.setFreemarkerConfiguration(configuration);

        UserDataBuilderParams params = new UserDataBuilderParams();
        params.setCustomData("date >> /tmp/time.txt");

        ReflectionTestUtils.setField(underTest, "userDataBuilderParams", params);
    }

    @Test
    public void testBuildUserDataAzure() throws IOException {
        String expectedGwScript = FileReaderUtils.readFileFromClasspath("azure-gateway-init.sh");
        String expectedCoreScript = FileReaderUtils.readFileFromClasspath("azure-core-init.sh");
        Map<InstanceGroupType, String> userdata = underTest.buildUserData(Platform.platform("AZURE"), "priv-key".getBytes(),
                "cloudbreak", getPlatformParameters(), "pass", "cert", new CcmConnectivityParameters(), null);
        Assert.assertEquals(expectedGwScript, userdata.get(InstanceGroupType.GATEWAY));
        Assert.assertEquals(expectedCoreScript, userdata.get(InstanceGroupType.CORE));
    }

    @Test
    public void testBuildUserDataWithCCM() throws IOException {
        BaseServiceEndpoint serviceEndpoint = new BaseServiceEndpoint(new HostEndpoint("ccm.cloudera.com"));
        DefaultServerParameters serverParameters = new DefaultServerParameters(serviceEndpoint, "pub-key", "mina-id");
        DefaultInstanceParameters instanceParameters = new DefaultInstanceParameters("tunnel-id", "key-id", "private-key");
        DefaultTunnelParameters nginxTunnel = new DefaultTunnelParameters(KnownServiceIdentifier.GATEWAY, 9443);
        DefaultTunnelParameters knoxTunnel = new DefaultTunnelParameters(KnownServiceIdentifier.KNOX, 8443);
        CcmParameters ccmParameters = new DefaultCcmParameters(serverParameters, instanceParameters, List.of(nginxTunnel, knoxTunnel));
        CcmConnectivityParameters ccmConnectivityParameters = new CcmConnectivityParameters(ccmParameters);

        Map<InstanceGroupType, String> userdata = underTest.buildUserData(Platform.platform("AZURE"), "priv-key".getBytes(),
                "cloudbreak", getPlatformParameters(), "pass", "cert", ccmConnectivityParameters, null);

        String expectedGwScript = FileReaderUtils.readFileFromClasspath("azure-gateway-ccm-init.sh");
        String expectedCoreScript = FileReaderUtils.readFileFromClasspath("azure-core-ccm-init.sh");
        Assert.assertEquals(expectedGwScript, userdata.get(InstanceGroupType.GATEWAY));
        Assert.assertEquals(expectedCoreScript, userdata.get(InstanceGroupType.CORE));
    }

    @Test
    public void testBuildUserDataWithCCMV2() throws IOException {
        CcmV2Parameters ccmV2Parameters = new DefaultCcmV2Parameters("invertingProxyHost", "invertingProxyCertificate",
                "agentCrn", "agentKeyId", "agentEncipheredPrivateKey", "agentCertificate");
        CcmConnectivityParameters ccmConnectivityParameters = new CcmConnectivityParameters(ccmV2Parameters);

        Map<InstanceGroupType, String> userdata = underTest.buildUserData(Platform.platform("AZURE"), "priv-key".getBytes(),
                "cloudbreak", getPlatformParameters(), "pass", "cert", ccmConnectivityParameters, null);

        String expectedCoreScript = FileReaderUtils.readFileFromClasspath("azure-core-ccm-init.sh");
        String expectedGwScript = FileReaderUtils.readFileFromClasspath("azure-gateway-ccm-v2-init.sh");

        Assert.assertEquals(expectedCoreScript, userdata.get(InstanceGroupType.CORE));
        Assert.assertEquals(expectedGwScript, userdata.get(InstanceGroupType.GATEWAY));
    }

    @Test
    public void testBuildUserDataWithCCMV2Jumpgate() throws IOException {
        CcmV2JumpgateParameters ccmV2JumpgateParameters = new DefaultCcmV2JumpgateParameters();
        CcmConnectivityParameters ccmConnectivityParameters = new CcmConnectivityParameters(ccmV2JumpgateParameters);

        Map<InstanceGroupType, String> userdata = underTest.buildUserData(Platform.platform("AZURE"), "priv-key".getBytes(),
                "cloudbreak", getPlatformParameters(), "pass", "cert", ccmConnectivityParameters, null);

        String expectedCoreScript = FileReaderUtils.readFileFromClasspath("azure-core-ccm-init.sh");
        String expectedGwScript = FileReaderUtils.readFileFromClasspath("azure-gateway-ccm-v2-jumpgate-init.sh");

        Assert.assertEquals(expectedCoreScript, userdata.get(InstanceGroupType.CORE));
        Assert.assertEquals(expectedGwScript, userdata.get(InstanceGroupType.GATEWAY));
    }

    @Test
    public void testBuildUserDataAzureWithNoAuthProxy() throws IOException {
        String expectedGwScript = FileReaderUtils.readFileFromClasspath("azure-gateway-init-noauthproxy.sh");
        String expectedCoreScript = FileReaderUtils.readFileFromClasspath("azure-core-init.sh");
        ProxyConfig proxyConfig = ProxyConfig.builder()
                .withServerHost("proxy.host")
                .withServerPort(1234)
                .withNoProxyHosts("noproxy.com")
                .build();
        Map<InstanceGroupType, String> userdata = underTest.buildUserData(Platform.platform("AZURE"), "priv-key".getBytes(),
                "cloudbreak", getPlatformParameters(), "pass", "cert", new CcmConnectivityParameters(), proxyConfig);
        Assert.assertEquals(expectedGwScript, userdata.get(InstanceGroupType.GATEWAY));
        Assert.assertEquals(expectedCoreScript, userdata.get(InstanceGroupType.CORE));
    }

    @Test
    public void testBuildUserDataAzureWithAuthProxy() throws IOException {
        String expectedGwScript = FileReaderUtils.readFileFromClasspath("azure-gateway-init-authproxy.sh");
        String expectedCoreScript = FileReaderUtils.readFileFromClasspath("azure-core-init.sh");
        ProxyAuthentication proxyAuthentication = ProxyAuthentication.builder()
                .withUserName("user")
                .withPassword("pwd")
                .build();
        ProxyConfig proxyConfig = ProxyConfig.builder()
                .withServerHost("proxy.host")
                .withServerPort(1234)
                .withProxyAuthentication(proxyAuthentication)
                .withNoProxyHosts("noproxy.com")
                .build();
        Map<InstanceGroupType, String> userdata = underTest.buildUserData(Platform.platform("AZURE"), "priv-key".getBytes(),
                "cloudbreak", getPlatformParameters(), "pass", "cert", new CcmConnectivityParameters(), proxyConfig);
        Assert.assertEquals(expectedGwScript, userdata.get(InstanceGroupType.GATEWAY));
        Assert.assertEquals(expectedCoreScript, userdata.get(InstanceGroupType.CORE));
    }

    private PlatformParameters getPlatformParameters() {
        return new TestPlatformParameters();
    }

    private static class TestPlatformParameters implements PlatformParameters {
        @Override
        public ScriptParams scriptParams() {
            return new ScriptParams("sd", 98);
        }

        @Override
        public DiskTypes diskTypes() {
            return new DiskTypes(new ArrayList<>(), DiskType.diskType(""), new HashMap<>(), new HashMap<>());
        }

        @Override
        public String resourceDefinition(String resource) {
            return "";
        }

        @Override
        public String resourceDefinitionInSubDir(String subDir, String resource) {
            return "";
        }

        @Override
        public List<StackParamValidation> additionalStackParameters() {
            return new ArrayList<>();
        }

        @Override
        public PlatformOrchestrator orchestratorParams() {
            return new PlatformOrchestrator(Collections.singleton(orchestrator(OrchestratorConstants.SALT)),
                    orchestrator(OrchestratorConstants.SALT));
        }

        @Override
        public TagSpecification tagSpecification() {
            return null;
        }

        @Override
        public VmRecommendations recommendedVms() {
            return null;
        }

        @Override
        public TagValidator tagValidator() {
            return null;
        }

        @Override
        public String platforName() {
            return "TEST";
        }

        @Override
        public boolean isAutoTlsSupported() {
            return false;
        }

    }
}
