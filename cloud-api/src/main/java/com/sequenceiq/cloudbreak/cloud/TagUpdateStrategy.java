package com.sequenceiq.cloudbreak.cloud;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.common.api.type.ResourceType;

public interface TagUpdateStrategy {

    Set<ResourceType> supportedTypes();

    void updateTags(AuthenticatedContext authenticatedContext, CloudResource cloudResource, Map<String, String> tags) throws IOException;
}
