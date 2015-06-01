package com.sequenceiq.cloudbreak.cloud.event;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.event.context.StackContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Security;

public class LaunchStackRequest extends CloudPlatformRequest {

    private CloudCredential cloudCredential;

    private List<Group> groups;

    private Network network;

    private Security security;

    private Image image;

    public LaunchStackRequest(StackContext stackContext, CloudCredential cloudCredential, List<Group> groups, Network network, Security security,
            Image image) {
        super(stackContext);
        this.cloudCredential = cloudCredential;
        this.groups = groups;
        this.network = network;
        this.security = security;
        this.image = image;
    }

    public CloudCredential getCloudCredential() {
        return cloudCredential;
    }

    public List<Group> getGroups() {
        return groups;
    }

    public Network getNetwork() {
        return network;
    }

    public Security getSecurity() {
        return security;
    }

    public Image getImage() {
        return image;
    }
}
