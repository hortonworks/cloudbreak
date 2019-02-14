package com.sequenceiq.it.cloudbreak.newway;

import java.util.List;

import javax.ws.rs.WebApplicationException;

import com.sequenceiq.cloudbreak.api.model.BlueprintRequest;
import com.sequenceiq.cloudbreak.api.model.BlueprintResponse;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

public class ClusterDefinitionEntity extends AbstractCloudbreakEntity<BlueprintRequest, BlueprintResponse, ClusterDefinitionEntity> {
    public static final String CLUSTER_DEFINITION = "CLUSTER_DEFINITION";

    ClusterDefinitionEntity(String newId) {
        super(newId);
        setRequest(new BlueprintRequest());
    }

    ClusterDefinitionEntity() {
        this(CLUSTER_DEFINITION);
    }

    public ClusterDefinitionEntity(TestContext testContext) {
        super(new BlueprintRequest(), testContext);
    }

    @Override
    public void cleanUp(TestContext context, CloudbreakClient cloudbreakClient) {
        LOGGER.info("Cleaning up resource with name: {}", getName());
        try {
            cloudbreakClient.getCloudbreakClient().blueprintV3Endpoint().deleteInWorkspace(cloudbreakClient.getWorkspaceId(), getName());
        } catch (WebApplicationException ignore) {
            LOGGER.info("Something happend.");
        }
    }

    public ClusterDefinitionEntity valid() {
        return withName(getNameCreator().getRandomNameForMock())
                .withAmbariBlueprint("someBlueprint");
    }

    public ClusterDefinitionEntity withName(String name) {
        getRequest().setName(name);
        setName(name);
        return this;
    }

    public ClusterDefinitionEntity withDescription(String description) {
        getRequest().setDescription(description);
        return this;
    }

    public ClusterDefinitionEntity withUrl(String url) {
        getRequest().setUrl(url);
        return this;
    }

    public ClusterDefinitionEntity withAmbariBlueprint(String blueprint) {
        getRequest().setAmbariBlueprint(blueprint);
        return this;
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

}