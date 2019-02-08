package com.sequenceiq.it.cloudbreak.newway;

import java.util.List;

import javax.ws.rs.WebApplicationException;

import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprints.requests.BlueprintV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprints.responses.BlueprintV4Response;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

public class BlueprintEntity extends AbstractCloudbreakEntity<BlueprintV4Request, BlueprintV4Response, BlueprintEntity> {
    public static final String BLUEPRINT = "BLUEPRINT";

    BlueprintEntity(String newId) {
        super(newId);
        setRequest(new BlueprintV4Request());
    }

    BlueprintEntity() {
        this(BLUEPRINT);
    }

    public BlueprintEntity(TestContext testContext) {
        super(new BlueprintV4Request(), testContext);
    }

    @Override
    public void cleanUp(TestContext context, CloudbreakClient cloudbreakClient) {
        LOGGER.info("Cleaning up resource with name: {}", getName());
        try {
            cloudbreakClient.getCloudbreakClient().blueprintV4Endpoint().delete(cloudbreakClient.getWorkspaceId(), getName());
        } catch (WebApplicationException ignore) {
            LOGGER.info("Something happend.");
        }
    }

    public BlueprintEntity valid() {
        return withName(getNameCreator().getRandomNameForMock())
                .withAmbariBlueprint("someBlueprint");
    }

    public BlueprintEntity withName(String name) {
        getRequest().setName(name);
        setName(name);
        return this;
    }

    public BlueprintEntity withDescription(String description) {
        getRequest().setDescription(description);
        return this;
    }

    public BlueprintEntity withUrl(String url) {
        getRequest().setUrl(url);
        return this;
    }

    public BlueprintEntity withAmbariBlueprint(String blueprint) {
        getRequest().setAmbariBlueprint(blueprint);
        return this;
    }

    public BlueprintEntity withTag(List<String> keys, List<Object> values) {
        if (keys.size() != values.size()) {
            throw new IllegalStateException("The given keys number does not match with the values number");
        }
        for (int i = 0; i < keys.size(); i++) {
            getRequest().getTags().put(keys.get(i), values.get(i));
        }
        return this;
    }

    @Override
    public int order() {
        return 500;
    }
}