package com.sequenceiq.provisioning.conf;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.provisioning.domain.CloudFormationTemplate;
import com.sequenceiq.provisioning.service.TemplateReader;

@Configuration
public class AppConfig {

    private static final String DEFAULT_TEMPLATE_NAME = "sample-vpc-template";

    @Autowired
    private TemplateReader templateReader;

    @Bean
    public CloudFormationTemplate defaultTemplate() throws IOException {
        return templateReader.readTemplateFromFile(DEFAULT_TEMPLATE_NAME);
    }

}
