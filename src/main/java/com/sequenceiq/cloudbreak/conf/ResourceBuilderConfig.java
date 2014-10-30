package com.sequenceiq.cloudbreak.conf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.service.stack.resource.InstanceResourceBuilder;
import com.sequenceiq.cloudbreak.service.stack.resource.NetworkResourceBuilder;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilder;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilderInit;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilderType;

@Configuration
public class ResourceBuilderConfig {

    @Autowired
    private List<InstanceResourceBuilder> instanceResourceBuilders;

    @Autowired
    private List<NetworkResourceBuilder> networkResourceBuilders;

    @Autowired
    private List<ResourceBuilderInit> resourceBuilderInits;

    @Bean
    Map<CloudPlatform, ResourceBuilderInit> resourceBuilderInits() {
        Map<CloudPlatform, ResourceBuilderInit> map = new HashMap<>();
        for (ResourceBuilderInit resourceBuilderInit : resourceBuilderInits) {
            map.put(resourceBuilderInit.cloudPlatform(), resourceBuilderInit);
        }
        return map;
    }

    @Bean
    Map<CloudPlatform, Map<ResourceBuilderType, List<ResourceBuilder>>> resourceBuilders() {
        Map<CloudPlatform, Map<ResourceBuilderType, List<ResourceBuilder>>> resourceBuilders = new HashMap<>();
        for (CloudPlatform cloudPlatform : CloudPlatform.values()) {
            if (!cloudPlatform.isWithTemplate()) {
                Map<ResourceBuilderType, List<ResourceBuilder>> mainResourceBuilders = new HashMap<>();
                List<ResourceBuilder> mainInstanceResourceBuilders = new ArrayList<>();
                for (InstanceResourceBuilder instanceResourceBuilder : instanceResourceBuilders) {
                    if (cloudPlatform.equals(instanceResourceBuilder.cloudPlatform())) {
                        mainInstanceResourceBuilders.add(instanceResourceBuilder);
                    }
                }
                List<ResourceBuilder> mainNetworkResourceBuilders = new ArrayList<>();
                for (NetworkResourceBuilder networkResourceBuilder : networkResourceBuilders) {
                    if (cloudPlatform.equals(networkResourceBuilder.cloudPlatform())) {
                        mainNetworkResourceBuilders.add(networkResourceBuilder);
                    }
                }

                mainResourceBuilders.put(ResourceBuilderType.INSTANCE_RESOURCE, mainInstanceResourceBuilders);
                mainResourceBuilders.put(ResourceBuilderType.NETWORK_RESOURCE, mainNetworkResourceBuilders);
                resourceBuilders.put(cloudPlatform, mainResourceBuilders);
            }
        }
        return resourceBuilders;
    }
}
