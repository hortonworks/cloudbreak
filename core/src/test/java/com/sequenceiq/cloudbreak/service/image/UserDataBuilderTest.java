package com.sequenceiq.cloudbreak.service.image;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.ui.freemarker.FreeMarkerConfigurationFactoryBean;

import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.common.type.CloudPlatform;
import com.sequenceiq.cloudbreak.common.type.InstanceGroupType;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;

public class UserDataBuilderTest {

    private static UserDataBuilder userDataBuilder = new UserDataBuilder();

    @Before
    public void setup() throws IOException, TemplateException {
        FreeMarkerConfigurationFactoryBean factoryBean = new FreeMarkerConfigurationFactoryBean();
        factoryBean.setPreferFileSystemAccess(false);
        factoryBean.setTemplateLoaderPath("classpath:/");
        factoryBean.afterPropertiesSet();
        Configuration configuration = factoryBean.getObject();
        userDataBuilder.setFreemarkerConfiguration(configuration);
        userDataBuilder.setRelocateDocker(true);
    }

    @Test
    public void testBuildUserDataAzure() throws IOException {
        String expectedGwScript = FileReaderUtils.readFileFromClasspath("azure-gateway-init.sh");
        String expectedCoreScript = FileReaderUtils.readFileFromClasspath("azure-core-init.sh");
        Map<InstanceGroupType, String> userdata = userDataBuilder.buildUserData(CloudPlatform.AZURE, "ssh-rsa test", "cloudbreak", getPlatformParameters());
        Assert.assertEquals(expectedGwScript, userdata.get(InstanceGroupType.GATEWAY));
        Assert.assertEquals(expectedCoreScript, userdata.get(InstanceGroupType.CORE));
    }

    public PlatformParameters getPlatformParameters() {
        return new PlatformParameters() {
            @Override
            public String diskPrefix() {
                return "sd";
            }

            @Override
            public Integer startLabel() {
                return 98;
            }

            @Override
            public Map<String, String> diskTypes() {
                return new HashMap<>();
            }

            @Override
            public String defaultDiskType() {
                return "";
            }

            @Override
            public Map<String, String> regions() {
                return new HashMap<>();
            }

            @Override
            public String defaultRegion() {
                return "";
            }

            @Override
            public Map<String, List<String>> availabiltyZones() {
                return new HashMap<>();
            }

            @Override
            public Map<String, String> virtualMachines() {
                return new HashMap<>();
            }

            @Override
            public String defaultVirtualMachine() {
                return "";
            }
        };
    }
}