package com.sequenceiq.cloudbreak.cmtemplate.generator.support;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTestContextBootstrapper;
import org.springframework.test.context.BootstrapWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.sequenceiq.cloudbreak.cmtemplate.generator.CentralTemplateGeneratorContext;
import com.sequenceiq.cloudbreak.cmtemplate.generator.support.domain.SupportedVersions;

@ExtendWith(SpringExtension.class)
@BootstrapWith(SpringBootTestContextBootstrapper.class)
class SupportedVersionServiceTest extends CentralTemplateGeneratorContext {

    @Test
    void testServicesAndDependencies() {
        SupportedVersions supportedVersions = supportedVersionService().collectSupportedVersions();
        assertTrue(!supportedVersions.getSupportedVersions().isEmpty());
    }
}