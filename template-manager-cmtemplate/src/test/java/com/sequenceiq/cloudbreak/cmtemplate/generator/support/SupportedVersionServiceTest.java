package com.sequenceiq.cloudbreak.cmtemplate.generator.support;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTestContextBootstrapper;
import org.springframework.test.context.BootstrapWith;
import org.springframework.test.context.junit4.SpringRunner;

import com.sequenceiq.cloudbreak.cmtemplate.generator.CentralTemplateGeneratorContext;
import com.sequenceiq.cloudbreak.cmtemplate.generator.support.domain.SupportedVersions;

@RunWith(SpringRunner.class)
@BootstrapWith(SpringBootTestContextBootstrapper.class)
public class SupportedVersionServiceTest extends CentralTemplateGeneratorContext {

    @Test
    public void testServicesAndDependencies() {
        SupportedVersions supportedVersions = supportedVersionService().collectSupportedVersions();
        Assert.assertTrue(!supportedVersions.getSupportedVersions().isEmpty());
    }
}