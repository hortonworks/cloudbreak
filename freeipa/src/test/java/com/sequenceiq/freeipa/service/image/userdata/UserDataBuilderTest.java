package com.sequenceiq.freeipa.service.image.userdata;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.freemarker.FreeMarkerConfigurationFactoryBean;

import com.sequenceiq.cloudbreak.ccm.cloudinit.CcmParameters;
import com.sequenceiq.cloudbreak.ccm.cloudinit.DefaultCcmParameters;
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

import freemarker.template.Configuration;
import freemarker.template.TemplateException;

@ExtendWith(MockitoExtension.class)
public class UserDataBuilderTest {

    @InjectMocks
    private UserDataBuilder underTest;

    @Spy
    private FreeMarkerTemplateUtils freeMarkerTemplateUtils;

    @BeforeEach
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
    @DisplayName("test if CCM parameters are passed the user data contains them")
    public void testBuildUserDataWithCCMParams() throws IOException {
        BaseServiceEndpoint serviceEndpoint = new BaseServiceEndpoint(new HostEndpoint("ccm.cloudera.com"));
        DefaultServerParameters serverParameters = new DefaultServerParameters(serviceEndpoint, "pub-key", "mina-id");
        DefaultInstanceParameters instanceParameters = new DefaultInstanceParameters("tunnel-id", "key-id", "private-key");
        DefaultTunnelParameters nginxTunnel = new DefaultTunnelParameters(KnownServiceIdentifier.GATEWAY, 9443);
        CcmParameters ccmParameters = new DefaultCcmParameters(serverParameters, instanceParameters, List.of(nginxTunnel));
        PlatformParameters platformParameters = mock(PlatformParameters.class);
        ScriptParams scriptParams = mock(ScriptParams.class);
        when(scriptParams.getDiskPrefix()).thenReturn("sd");
        when(scriptParams.getStartLabel()).thenReturn(98);
        when(platformParameters.scriptParams()).thenReturn(scriptParams);

        String userData = underTest.buildUserData(Platform.platform("AZURE"), "priv-key".getBytes(),
                "cloudbreak", platformParameters, "pass", "cert", ccmParameters, null);

        String expectedUserData = FileReaderUtils.readFileFromClasspath("azure-ccm-init.sh");
        Assert.assertEquals(expectedUserData, userData);
    }

    @Test
    @DisplayName("test if NO CCM parameters are passed the user data does not contain them")
    public void testBuildUserDataWithoutCCMParams() throws IOException {
        PlatformParameters platformParameters = mock(PlatformParameters.class);
        ScriptParams scriptParams = mock(ScriptParams.class);
        when(scriptParams.getDiskPrefix()).thenReturn("sd");
        when(scriptParams.getStartLabel()).thenReturn(98);
        when(platformParameters.scriptParams()).thenReturn(scriptParams);

        String userData = underTest.buildUserData(Platform.platform("AZURE"), "priv-key".getBytes(),
                "cloudbreak", platformParameters, "pass", "cert", null, null);

        String expectedUserData = FileReaderUtils.readFileFromClasspath("azure-init.sh");
        Assert.assertEquals(expectedUserData, userData);
    }

}
