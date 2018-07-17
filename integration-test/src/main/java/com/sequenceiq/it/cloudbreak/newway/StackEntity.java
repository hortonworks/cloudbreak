package com.sequenceiq.it.cloudbreak.newway;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.sequenceiq.cloudbreak.api.model.stack.StackAuthenticationRequest;
import com.sequenceiq.cloudbreak.api.model.stack.StackResponse;
import com.sequenceiq.cloudbreak.api.model.v2.ClusterV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.CustomDomainSettings;
import com.sequenceiq.cloudbreak.api.model.v2.GeneralSettings;
import com.sequenceiq.cloudbreak.api.model.v2.ImageSettings;
import com.sequenceiq.cloudbreak.api.model.v2.InstanceGroupV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.NetworkV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.PlacementSettings;
import com.sequenceiq.cloudbreak.api.model.v2.StackV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.Tags;

public class StackEntity extends AbstractCloudbreakEntity<StackV2Request, StackResponse> {
    public static final String STACK = "STACK";

    StackEntity(String newId) {
        super(newId);
        StackV2Request r = new StackV2Request();
        r.setGeneral(new GeneralSettings());
        r.setPlacement(new PlacementSettings());
        setRequest(r);
    }

    StackEntity() {
        this(STACK);
    }

    public StackEntity withName(String name) {
        getRequest().getGeneral().setName(name);
        setName(name);
        return this;
    }

    public StackEntity withCredentialName(String credentialName) {
        getRequest().getGeneral().setCredentialName(credentialName);
        return this;
    }

    public StackEntity withClusterRequest(ClusterV2Request clusterRequest) {
        getRequest().setCluster(clusterRequest);
        return this;
    }

    public StackEntity withAvailabilityZone(String availabilityZone) {
        getRequest().getPlacement().setAvailabilityZone(availabilityZone);
        return this;
    }

    public StackEntity withClusterNameAsSubdomain(boolean b) {
        if (getRequest().getCustomDomain() == null) {
            getRequest().setCustomDomain(new CustomDomainSettings());
        }
        getRequest().getCustomDomain().setClusterNameAsSubdomain(b);
        return this;
    }

    public StackEntity withFlexId(Long flexId) {
        getRequest().setFlexId(flexId);
        return this;
    }

    public StackEntity withImageCatalog(String imageCatalog) {
        if (getRequest().getImageSettings() == null) {
            getRequest().setImageSettings(new ImageSettings());
        }
        getRequest().getImageSettings().setImageCatalog(imageCatalog);
        return this;
    }

    public StackEntity withImageId(String imageId) {
        if (getRequest().getImageSettings() == null) {
            getRequest().setImageSettings(new ImageSettings());
        }
        getRequest().getImageSettings().setImageId(imageId);
        return this;
    }

    public StackEntity withInputs(Map<String, Object> inputs) {
        if (inputs == null) {
            getRequest().setInputs(Collections.emptyMap());
        } else {
            getRequest().setInputs(inputs);
        }
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

    public StackEntity withParameters(Map<String, String> parameters) {
        getRequest().setParameters(parameters);
        return this;
    }

    public StackEntity withRegion(String region) {
        getRequest().getPlacement().setRegion(region);
        return this;
    }

    public StackEntity withStackAuthentication(StackAuthenticationRequest stackAuthentication) {
        getRequest().setStackAuthentication(stackAuthentication);
        return this;
    }

    public StackEntity withUserDefinedTags(Map<String, String> tags) {
        if (getRequest().getTags() == null) {
            getRequest().setTags(new Tags());
        }
        getRequest().getTags().setUserDefinedTags(tags);
        return this;
    }

    public StackEntity withAmbariVersion(String version) {
        getRequest().setAmbariVersion(version);
        return this;
    }

    public boolean hasCluster() {
        return getRequest().getCluster() != null;
    }
}
