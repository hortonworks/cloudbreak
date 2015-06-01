package com.sequenceiq.cloudbreak.cloud;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.event.context.StackContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Security;


public interface CloudPlatformConnectorV2 {

    String getCloudPlatform();

    AuthenticatedContext authenticate(StackContext stackContext, CloudCredential cloudCredential);

    List<CloudResourceStatus> launchStack(AuthenticatedContext authenticatedContext, List<Group> groups, Network network, Security security,
            Image image);

    List<CloudResourceStatus> checkResourcesState(AuthenticatedContext authenticatedContext, List<CloudResource> resources);

}
