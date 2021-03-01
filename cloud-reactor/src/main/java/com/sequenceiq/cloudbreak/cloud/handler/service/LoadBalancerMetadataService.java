package com.sequenceiq.cloudbreak.cloud.handler.service;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancerMetadata;
import com.sequenceiq.common.api.type.LoadBalancerType;

@Service
public class LoadBalancerMetadataService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoadBalancerMetadataService.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    public List<CloudLoadBalancerMetadata> collectMetadata(CloudContext cloudContext, CloudCredential cloudCredential,
            List<LoadBalancerType> types) {
        LOGGER.debug("Initiating load balancer metadata collection");
        CloudConnector<Object> connector = cloudPlatformConnectors.get(cloudContext.getPlatformVariant());
        AuthenticatedContext ac = connector.authentication().authenticate(cloudContext, cloudCredential);
        List<CloudLoadBalancerMetadata> loadBalancerStatuses = connector.metadata().collectLoadBalancer(ac, types);
        LOGGER.debug("Load balancer metadata collection successfully finished. Collected metadata: {}", loadBalancerStatuses);
        return loadBalancerStatuses;
    }
}
