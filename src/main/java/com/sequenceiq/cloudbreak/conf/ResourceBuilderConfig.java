package com.sequenceiq.cloudbreak.conf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilder;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilderInit;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilderType;

@Configuration
public class ResourceBuilderConfig {

    @Autowired
    private List<ResourceBuilder> resourceBuilders;

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
    Map<CloudPlatform, List<ResourceBuilder>> instanceResourceBuilders() {
        Map<CloudPlatform, List<ResourceBuilder>> returnResourceBuilders = new HashMap<>();
        for (CloudPlatform cloudPlatform : CloudPlatform.values()) {
            if (!cloudPlatform.isWithTemplate()) {
                List<ResourceBuilder> mainInstanceResourceBuilders = new ArrayList<>();
                for (ResourceBuilder networkResourceBuilder : resourceBuilders) {
                    if (ResourceBuilderType.INSTANCE_RESOURCE.equals(networkResourceBuilder.resourceBuilderType())) {
                        if (cloudPlatform.equals(networkResourceBuilder.cloudPlatform())) {
                            mainInstanceResourceBuilders.add(networkResourceBuilder);
                        }
                    }
                }
                returnResourceBuilders.put(cloudPlatform, mainInstanceResourceBuilders);
            }
        }
        return returnResourceBuilders;
    }

    @Bean
    Map<CloudPlatform, List<ResourceBuilder>> networkResourceBuilders() {
        Map<CloudPlatform, List<ResourceBuilder>> returnResourceBuilders = new HashMap<>();
        for (CloudPlatform cloudPlatform : CloudPlatform.values()) {
            if (!cloudPlatform.isWithTemplate()) {
                List<ResourceBuilder> mainNetworkResourceBuilders = new ArrayList<>();
                for (ResourceBuilder networkResourceBuilder : resourceBuilders) {
                    if (ResourceBuilderType.NETWORK_RESOURCE.equals(networkResourceBuilder.resourceBuilderType())) {
                        if (cloudPlatform.equals(networkResourceBuilder.cloudPlatform())) {
                            mainNetworkResourceBuilders.add(networkResourceBuilder);
                        }
                    }
                }
                returnResourceBuilders.put(cloudPlatform, mainNetworkResourceBuilders);
            }
        }
        return returnResourceBuilders;
    }
}
