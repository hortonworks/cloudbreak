package com.sequenceiq.cloudbreak.service.stack.connector;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.ui.freemarker.FreeMarkerConfigurationFactoryBean;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;
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
    }

    @Test
    public void testBuildUserDataAzure() throws IOException {
        String expectedCoreScript = FileReaderUtils.readFileFromClasspath("azure-core-init.sh");
        String userdata = userDataBuilder.buildUserData(CloudPlatform.AZURE);
        Assert.assertEquals(expectedCoreScript, userdata);
    }

}
