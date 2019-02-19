package com.sequenceiq.it.cloudbreak.newway.entity.clusterdefinition;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.ws.rs.WebApplicationException;

import com.sequenceiq.cloudbreak.api.endpoint.v4.clusterdefinition.requests.ClusterDefinitionV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.clusterdefinition.responses.ClusterDefinitionV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.clusterdefinition.responses.ClusterDefinitionV4ViewResponse;
import com.sequenceiq.it.cloudbreak.newway.AbstractCloudbreakEntity;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

public class ClusterDefinitionEntity extends AbstractCloudbreakEntity<ClusterDefinitionV4Request, ClusterDefinitionV4Response, ClusterDefinitionEntity> {
    public static final String CLUSTER_DEFINITION = "CLUSTER_DEFINITION";

    private Collection<ClusterDefinitionV4ViewResponse> viewResponses;

    ClusterDefinitionEntity(String newId) {
        super(newId);
        setRequest(new ClusterDefinitionV4Request());
    }

    ClusterDefinitionEntity() {
        this(CLUSTER_DEFINITION);
    }

    public ClusterDefinitionEntity(TestContext testContext) {
        super(new ClusterDefinitionV4Request(), testContext);
    }

    @Override
    public void cleanUp(TestContext context, CloudbreakClient cloudbreakClient) {
        LOGGER.info("Cleaning up resource with name: {}", getName());
        try {
            cloudbreakClient.getCloudbreakClient().clusterDefinitionV4Endpoint().delete(cloudbreakClient.getWorkspaceId(), getName());
        } catch (WebApplicationException ignore) {
            LOGGER.info("Something happend.");
        }
    }

    public ClusterDefinitionEntity valid() {
        return withName(getNameCreator().getRandomNameForMock())
                .withClusterDefinition("someBlueprint");
    }

    public ClusterDefinitionEntity withName(String name) {
        getRequest().setName(name);
        setName(name);
        return this;
    }

    public String getDescription() {
        return getRequest().getDescription();
    }

    public ClusterDefinitionEntity withDescription(String description) {
        getRequest().setDescription(description);
        return this;
    }

    public ClusterDefinitionEntity withUrl(String url) {
        getRequest().setUrl(url);
        return this;
    }

    public ClusterDefinitionEntity withClusterDefinition(String clusterDefinition) {
        getRequest().setClusterDefinition(clusterDefinition);
        return this;
    }

    public Map<String, Object> getTag() {
        return getRequest().getTags();
    }

    public ClusterDefinitionEntity withTag(List<String> keys, List<Object> values) {
        if (keys.size() != values.size()) {
            throw new IllegalStateException("The given keys number does not match with the values number");
        }
        for (int i = 0; i < keys.size(); i++) {
            getRequest().getTags().put(keys.get(i), values.get(i));
        }
        return this;
    }

    public Collection<ClusterDefinitionV4ViewResponse> getViewResponses() {
        return viewResponses;
    }

    public void setViewResponses(Collection<ClusterDefinitionV4ViewResponse> viewResponses) {
        this.viewResponses = viewResponses;
    }

    @Override
    public int order() {
        return 500;
    }
}