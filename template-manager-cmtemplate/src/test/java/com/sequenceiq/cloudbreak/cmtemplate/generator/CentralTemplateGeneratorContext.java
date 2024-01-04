package com.sequenceiq.cloudbreak.cmtemplate.generator;

import jakarta.inject.Inject;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;

import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.cmtemplate.generator.configuration.CmTemplateGeneratorConfigurationResolver;
import com.sequenceiq.cloudbreak.cmtemplate.generator.dependencies.ServiceDependencyMatrixService;
import com.sequenceiq.cloudbreak.cmtemplate.generator.support.SupportedVersionService;
import com.sequenceiq.cloudbreak.cmtemplate.generator.template.GeneratedCmTemplateService;

@ContextConfiguration
public class CentralTemplateGeneratorContext {

    @Inject
    private SupportedVersionService supportedVersionService;

    @Inject
    private ServiceDependencyMatrixService serviceDependencyMatrixService;

    @Inject
    private GeneratedCmTemplateService generatedCMTemplateService;

    public SupportedVersionService supportedVersionService() {
        return supportedVersionService;
    }

    public ServiceDependencyMatrixService serviceDependencyMatrixService() {
        return serviceDependencyMatrixService;
    }

    public GeneratedCmTemplateService generatedClusterTemplateService() {
        return generatedCMTemplateService;
    }

    @Configuration
    @ComponentScan({"com.sequenceiq.cloudbreak.cmtemplate.generator"})
    public static class SpringConfig {

        @Bean
        public CmTemplateGeneratorConfigurationResolver resolver() {
            return new CmTemplateGeneratorConfigurationResolver();
        }

        @Bean
        public CmTemplateProcessorFactory cmTemplateProcessorFactory() {
            return new CmTemplateProcessorFactory();
        }
    }

}
