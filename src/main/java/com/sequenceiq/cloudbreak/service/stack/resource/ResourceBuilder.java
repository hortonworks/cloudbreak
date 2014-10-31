package com.sequenceiq.cloudbreak.service.stack.resource;

import java.util.List;

import com.google.common.base.Optional;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.ResourceType;

public interface ResourceBuilder<P extends ProvisionContextObject, D extends DeleteContextObject, DCO extends DescribeContextObject> {

    List<Resource> create(P po) throws Exception;

    List<Resource> create(P po, int index, List<Resource> resources) throws Exception;

    Boolean delete(Resource resource, D d) throws Exception;

    Optional<String> describe(Resource resource, DCO dco) throws Exception;

    ResourceBuilderType resourceBuilderType();

    ResourceType resourceType();

    CloudPlatform cloudPlatform();
}
