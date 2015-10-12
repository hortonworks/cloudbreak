package com.sequenceiq.cloudbreak.service.stack.resource;

import com.sequenceiq.cloudbreak.common.type.CloudPlatform;
import com.sequenceiq.cloudbreak.common.type.ResourceType;
import com.sequenceiq.cloudbreak.domain.Resource;

// TODO Have to be removed when the termination of the old version of azure clusters won't be supported anymore
public interface ResourceBuilder<D extends DeleteContextObject> {

    Boolean delete(Resource resource, D d, String region) throws Exception;

    ResourceBuilderType resourceBuilderType();

    ResourceType resourceType();

    CloudPlatform cloudPlatform();
}
