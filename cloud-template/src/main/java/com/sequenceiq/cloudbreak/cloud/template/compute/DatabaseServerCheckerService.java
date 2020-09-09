package com.sequenceiq.cloudbreak.cloud.template.compute;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;

public interface DatabaseServerCheckerService {

    List<CloudResourceStatus> check(
        AuthenticatedContext ac,
        List<CloudResource> resources,
        ResourceStatus waitedStatus);
}
