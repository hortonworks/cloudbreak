package com.sequenceiq.it.cloudbreak.config;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;

import freemarker.template.Configuration;
import freemarker.template.Template;

@org.springframework.context.annotation.Configuration
public class FreeMarkerConfig {
    @Autowired
    @Qualifier("freemarkerConfiguration")
    private Configuration freeConfig;

    @Bean
    public Template gccCredentialCreationTemplate() throws IOException {
        return freeConfig.getTemplate("requests/gcc.credential.json.fm", "UTF-8");
    }

    @Bean
    public Template awsCredentialCreationTemplate() throws IOException {
        return freeConfig.getTemplate("requests/aws.credential.json.fm", "UTF-8");
    }

    @Bean
    public Template blueprintCreationTemplate() throws IOException {
        return freeConfig.getTemplate("requests/blueprint.json.fm", "UTF-8");
    }

    @Bean
    public Template gccTemplateCreationTemplate() throws IOException {
        return freeConfig.getTemplate("requests/gcc.template.json.fm", "UTF-8");
    }

    @Bean
    public Template awsTemplateCreationTemplate() throws IOException {
        return freeConfig.getTemplate("requests/aws.template.json.fm", "UTF-8");
    }

    @Bean
    public Template azureTemplateCreationTemplate() throws IOException {
        return freeConfig.getTemplate("requests/azure.template.json.fm", "UTF-8");
    }

    @Bean
    public Template stackCreationTemplate() throws IOException {
        return freeConfig.getTemplate("requests/stack.json.fm", "UTF-8");
    }

    @Bean
    public Template clusterCreationTemplate() throws IOException {
        return freeConfig.getTemplate("requests/cluster.json.fm", "UTF-8");
    }

    @Bean
    public Template hostgroupAdjustmentTemplate() throws IOException {
        return freeConfig.getTemplate("requests/hostgroup.adjustment.json.fm", "UTF-8");
    }

    @Bean
    public Template instancegroupAdjustmentTemplate() throws IOException {
        return freeConfig.getTemplate("requests/instancegroup.adjustment.json.fm", "UTF-8");
    }

    @Bean
    public Template statusUpdateTemplate() throws IOException {
        return freeConfig.getTemplate("requests/status.update.json.fm", "UTF-8");
    }
}
