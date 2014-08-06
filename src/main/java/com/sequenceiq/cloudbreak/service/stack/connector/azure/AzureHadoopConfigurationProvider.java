package com.sequenceiq.cloudbreak.service.stack.connector.azure;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.stack.connector.HadoopConfigurationProvider;

@Service
public class AzureHadoopConfigurationProvider implements HadoopConfigurationProvider {

    @Override
    public Map<String, String> getYarnSiteConfigs(Stack stack) {
        return new HashMap<>();
    }

    @Override
    public Map<String, String> getHdfsSiteConfigs(Stack stack) {
        return new HashMap<>();
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AZURE;
    }

}
