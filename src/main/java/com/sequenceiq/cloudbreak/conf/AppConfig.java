package com.sequenceiq.cloudbreak.conf;

import com.sequenceiq.cloudbreak.domain.CloudFormationTemplate;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.service.stack.ProvisionService;
import com.sequenceiq.cloudbreak.service.stack.aws.TemplateReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class AppConfig {

    private static final String DEFAULT_TEMPLATE_NAME = "vpc-and-subnet.template";

    @Autowired
    private TemplateReader templateReader;

    @Autowired
    private List<ProvisionService> provisionServices;

    @Bean
    public CloudFormationTemplate defaultTemplate() throws IOException {
        return templateReader.readTemplateFromFile(DEFAULT_TEMPLATE_NAME);
    }

    @Bean
    public Map<CloudPlatform, ProvisionService> provisionServices() {
        Map<CloudPlatform, ProvisionService> map = new HashMap<>();
        for (ProvisionService provisionService : provisionServices) {
            map.put(provisionService.getCloudPlatform(), provisionService);
        }
        return map;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}
