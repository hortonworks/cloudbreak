package com.sequenceiq.provisioning.conf;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.provisioning.domain.CloudFormationTemplate;
import com.sequenceiq.provisioning.domain.CloudPlatform;
import com.sequenceiq.provisioning.service.CloudInstanceService;
import com.sequenceiq.provisioning.service.CredentialService;
import com.sequenceiq.provisioning.service.InfraService;
import com.sequenceiq.provisioning.service.aws.TemplateReader;

@Configuration
public class AppConfig {

    private static final String DEFAULT_TEMPLATE_NAME = "ambari-cluster.template";

    @Autowired
    private TemplateReader templateReader;

    @Autowired
    private List<CloudInstanceService> cloudInstanceServices;

    @Autowired
    private List<InfraService> infraServices;

    @Autowired
    private List<CredentialService> credentialServices;

    @Bean
    public CloudFormationTemplate defaultTemplate() throws IOException {
        return templateReader.readTemplateFromFile(DEFAULT_TEMPLATE_NAME);
    }

    @Bean
    public Map<CloudPlatform, CloudInstanceService> cloudInstanceServices() {
        Map<CloudPlatform, CloudInstanceService> map = new HashMap<>();
        for (CloudInstanceService cloudInstanceService : cloudInstanceServices) {
            map.put(cloudInstanceService.getCloudPlatform(), cloudInstanceService);
        }
        return map;
    }

    @Bean
    public Map<CloudPlatform, InfraService> infraServices() {
        Map<CloudPlatform, InfraService> map = new HashMap<>();
        for (InfraService infraService : infraServices) {
            map.put(infraService.getCloudPlatform(), infraService);
        }
        return map;
    }

    @Bean
    public Map<CloudPlatform, CredentialService> credentialServices() {
        Map<CloudPlatform, CredentialService> map = new HashMap<>();
        for (CredentialService credentialService : credentialServices) {
            map.put(credentialService.getCloudPlatform(), credentialService);
        }
        return map;
    }

}
