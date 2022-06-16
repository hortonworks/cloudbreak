package com.sequenceiq.consumption.service.cloud;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.notification.model.ResourceNotification;
import com.sequenceiq.cloudbreak.cloud.service.Persister;

/**
 * This class is only required by the cloud-reactor module. The cloud-consumption service does not
 * create or interact with cloud resources, it only uses the cloud-reactor module to query metadata
 * from cloud providers (e.g. Amazon CloudWatch's GetMetricStatistics API), hence the functions of
 * this class does not need implementation.
 */
@Component
public class CloudResourcePersisterService implements Persister<ResourceNotification> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudResourcePersisterService.class);

    @Override
    public ResourceNotification persist(ResourceNotification notification) {
        throw new UnsupportedOperationException("Cloud resource persist operation is not supported.");
    }

    @Override
    public ResourceNotification update(ResourceNotification notification) {
        throw new UnsupportedOperationException("Cloud resource update operation is not supported.");
    }

    @Override
    public ResourceNotification delete(ResourceNotification notification) {
        throw new UnsupportedOperationException("Cloud resource delete operation is not supported.");
    }

}
