package com.sequenceiq.freeipa.service.encryption;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.freeipa.entity.Stack;

@Component
public class CloudInformationDecoratorProvider {

    @Inject
    private List<CloudInformationDecorator> cloudInformationDecorators;

    private final Map<CloudPlatformVariant, CloudInformationDecorator> cloudInformationDecoratorMap = new HashMap<>();

    @PostConstruct
    private void init() {
        for (CloudInformationDecorator cloudInformationDecorator : cloudInformationDecorators) {
            CloudPlatformVariant cloudPlatformVariant = new CloudPlatformVariant(cloudInformationDecorator.platform(), cloudInformationDecorator.variant());
            cloudInformationDecoratorMap.put(cloudPlatformVariant, cloudInformationDecorator);
        }
    }

    public CloudInformationDecorator get(CloudPlatformVariant cloudPlatformVariant) {
        return cloudInformationDecoratorMap.get(cloudPlatformVariant);
    }

    public CloudInformationDecorator getForStack(Stack stack) {
        return get(new CloudPlatformVariant(stack.getCloudPlatform(), stack.getPlatformvariant()));
    }

}
