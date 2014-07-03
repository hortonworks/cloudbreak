package com.sequenceiq.cloudbreak.conf;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.sequenceiq.cloudbreak.domain.CloudFormationTemplate;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.service.stack.MetadataSetup;
import com.sequenceiq.cloudbreak.service.stack.ProvisionService;
import com.sequenceiq.cloudbreak.service.stack.ProvisionSetup;
import com.sequenceiq.cloudbreak.service.stack.Provisioner;
import com.sequenceiq.cloudbreak.service.stack.aws.TemplateReader;

@Configuration
public class AppConfig {

    private static final String DEFAULT_TEMPLATE_NAME = "vpc-and-subnet.template";

    @Autowired
    private TemplateReader templateReader;

    @Autowired
    private List<ProvisionService> provisionServices;

    @Autowired
    private List<ProvisionSetup> provisionSetups;

    @Autowired
    private List<Provisioner> provisioners;

    @Autowired
    private List<MetadataSetup> metadataSetups;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

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
    public Map<CloudPlatform, ProvisionSetup> provisionSetups() {
        Map<CloudPlatform, ProvisionSetup> map = new HashMap<>();
        for (ProvisionSetup provisionSetup : provisionSetups) {
            map.put(provisionSetup.getCloudPlatform(), provisionSetup);
        }
        return map;
    }

    @Bean
    public Map<CloudPlatform, Provisioner> provisioners() {
        Map<CloudPlatform, Provisioner> map = new HashMap<>();
        for (Provisioner provisioner : provisioners) {
            map.put(provisioner.getCloudPlatform(), provisioner);
        }
        return map;
    }

    @Bean
    public Map<CloudPlatform, MetadataSetup> metadataSetups() {
        Map<CloudPlatform, MetadataSetup> map = new HashMap<>();
        for (MetadataSetup metadataSetup : metadataSetups) {
            map.put(metadataSetup.getCloudPlatform(), metadataSetup);
        }
        return map;
    }
}
