package com.sequenceiq.cloudbreak.conf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilder;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilderInit;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilderType;

@Configuration
public class ResourceBuilderConfig {

    @Inject
    private List<ResourceBuilder> resourceBuilders;

    @Inject
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
                for (ResourceBuilder instanceResourceBuilderElement : resourceBuilders) {
                    if (ResourceBuilderType.INSTANCE_RESOURCE.equals(instanceResourceBuilderElement.resourceBuilderType())) {
                        if (cloudPlatform.equals(instanceResourceBuilderElement.cloudPlatform())) {
                            mainInstanceResourceBuilders.add(instanceResourceBuilderElement);
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
                for (ResourceBuilder networkResourceBuilderElement : resourceBuilders) {
                    if (ResourceBuilderType.NETWORK_RESOURCE.equals(networkResourceBuilderElement.resourceBuilderType())) {
                        if (cloudPlatform.equals(networkResourceBuilderElement.cloudPlatform())) {
                            mainNetworkResourceBuilders.add(networkResourceBuilderElement);
                        }
                    }
                }
                returnResourceBuilders.put(cloudPlatform, mainNetworkResourceBuilders);
            }
        }
        return returnResourceBuilders;
    }
}
