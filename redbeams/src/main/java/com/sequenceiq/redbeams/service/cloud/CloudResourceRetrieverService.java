package com.sequenceiq.redbeams.service.cloud;

import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.service.ResourceRetriever;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.redbeams.converter.spi.DBResourceToCloudResourceConverter;
import com.sequenceiq.redbeams.domain.stack.DBResource;
import com.sequenceiq.redbeams.service.stack.DBResourceService;

@Component
public class CloudResourceRetrieverService implements ResourceRetriever {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudResourceRetrieverService.class);

    @Inject
    private DBResourceService dbResourceService;

    @Inject
    private DBResourceToCloudResourceConverter cloudResourceConverter;

    @Override
    public Optional<CloudResource> findByResourceReferenceAndStatusAndType(String resourceReference, CommonStatus status, ResourceType resourceType) {
        return Optional.empty();
    }

    @Override
    public Optional<CloudResource> findByStatusAndTypeAndStack(CommonStatus status, ResourceType resourceType, Long stackId) {
        Optional<DBResource> optionalResource = dbResourceService.findByStatusAndTypeAndStack(status, resourceType, stackId);
        LOGGER.debug("Resource retrieved by optional Resource status: {}, type: {}, stackId: {}. Is present: {}", status, resourceType, stackId,
                optionalResource.isPresent());
        return optionalResource
                .map(resource -> cloudResourceConverter.convert(resource));
    }
}
