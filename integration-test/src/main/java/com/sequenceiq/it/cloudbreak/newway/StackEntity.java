package com.sequenceiq.it.cloudbreak.newway;

import com.sequenceiq.cloudbreak.api.model.OrchestratorRequest;
import com.sequenceiq.cloudbreak.api.model.StackAuthenticationRequest;
import com.sequenceiq.cloudbreak.api.model.StackResponse;
import com.sequenceiq.cloudbreak.api.model.v2.ClusterV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.InstanceGroupV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.NetworkV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.StackV2Request;

import java.util.List;
import java.util.Map;

public class StackEntity extends AbstractCloudbreakEntity<StackV2Request, StackResponse> {
    public static final String STACK = "STACK";

    StackEntity(String newId) {
        super(newId);
        setRequest(new StackV2Request());
    }

    StackEntity() {
        this(STACK);
    }

    public StackEntity withName(String name) {
        getRequest().setName(name);
        setName(name);
        return this;
    }

    public StackEntity withCredentialName(String credentialName) {
        getRequest().setCredentialName(credentialName);
        return this;
    }

    public StackEntity withClusterRequest(ClusterV2Request clusterRequest) {
        getRequest().setClusterRequest(clusterRequest);
        return this;
    }

    public StackEntity withAvailabilityZone(String availabilityZone) {
        getRequest().setAvailabilityZone(availabilityZone);
        return this;
    }

    public StackEntity withClusterNameAsSubdomain(boolean b) {
        getRequest().setClusterNameAsSubdomain(b);
        return this;
    }

    public StackEntity withFlexId(Long flexId) {
        getRequest().setFlexId(flexId);
        return this;
    }

    public StackEntity withImageCatalog(String imageCatalog) {
        getRequest().setImageCatalog(imageCatalog);
        return this;
    }

    public StackEntity withImageId(String imageId) {
        getRequest().setImageId(imageId);
        return this;
    }

    public StackEntity withInstanceGroups(List<InstanceGroupV2Request> instanceGroups) {
        getRequest().setInstanceGroups(instanceGroups);
        return this;
    }

    public StackEntity withNetwork(NetworkV2Request network) {
        getRequest().setNetwork(network);
        return this;
    }

    public StackEntity withOrchestrator(OrchestratorRequest orchestrator) {
        getRequest().setOrchestrator(orchestrator);
        return this;
    }

    public StackEntity withParameters(Map<String, String> parameters) {
        getRequest().setParameters(parameters);
        return this;
    }

    public StackEntity withRegion(String region) {
        getRequest().setRegion(region);
        return this;
    }

    public StackEntity withStackAuthentication(StackAuthenticationRequest stackAuthentication) {
        getRequest().setStackAuthentication(stackAuthentication);
        return this;
    }

    public StackEntity withUserDefinedTags(Map<String, String> tags) {
        getRequest().setUserDefinedTags(tags);
        return this;
    }

    public StackEntity withAmbariVersion(String version) {
        getRequest().setAmbariVersion(version);
        return this;
    }
}
