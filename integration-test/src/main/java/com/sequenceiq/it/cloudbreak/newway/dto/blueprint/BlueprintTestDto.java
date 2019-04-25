package com.sequenceiq.it.cloudbreak.newway.dto.blueprint;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.ws.rs.WebApplicationException;

import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.requests.BlueprintV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.responses.BlueprintV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.responses.BlueprintV4ViewResponse;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.Prototype;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.AbstractCloudbreakTestDto;

@Prototype
public class BlueprintTestDto extends AbstractCloudbreakTestDto<BlueprintV4Request, BlueprintV4Response, BlueprintTestDto> {
    public static final String BLUEPRINT = "BLUEPRINT";

    private Collection<BlueprintV4ViewResponse> viewResponses;

    public BlueprintTestDto(TestContext testContext) {
        super(new BlueprintV4Request(), testContext);
    }

    @Override
    public void cleanUp(TestContext context, CloudbreakClient cloudbreakClient) {
        LOGGER.info("Cleaning up resource with name: {}", getName());
        try {
            cloudbreakClient.getCloudbreakClient().clusterTemplateV4EndPoint().delete(cloudbreakClient.getWorkspaceId(), getName());
        } catch (WebApplicationException ignore) {
            LOGGER.info("Something happend.");
        }
    }

    public BlueprintTestDto valid() {
        return withName(resourceProperyProvider().getName())
                .withBlueprint("someBlueprint");
    }

    public BlueprintTestDto withName(String name) {
        getRequest().setName(name);
        setName(name);
        return this;
    }

    public String getDescription() {
        return getRequest().getDescription();
    }

    public BlueprintTestDto withDescription(String description) {
        getRequest().setDescription(description);
        return this;
    }

    public BlueprintTestDto withUrl(String url) {
        getRequest().setUrl(url);
        return this;
    }

    public BlueprintTestDto withBlueprint(String blueprint) {
        getRequest().setBlueprint(blueprint);
        return this;
    }

    public Map<String, Object> getTag() {
        return getRequest().getTags();
    }

    public BlueprintTestDto withTag(List<String> keys, List<Object> values) {
        if (keys.size() != values.size()) {
            throw new IllegalStateException("The given keys number does not match with the values number");
        }
        for (int i = 0; i < keys.size(); i++) {
            getRequest().getTags().put(keys.get(i), values.get(i));
        }
        return this;
    }

    public Collection<BlueprintV4ViewResponse> getViewResponses() {
        return viewResponses;
    }

    public void setViewResponses(Collection<BlueprintV4ViewResponse> viewResponses) {
        this.viewResponses = viewResponses;
    }

    @Override
    public int order() {
        return 500;
    }
}