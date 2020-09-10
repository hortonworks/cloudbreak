package com.sequenceiq.it.cloudbreak.dto.blueprint;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.requests.BlueprintV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.responses.BlueprintV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.responses.BlueprintV4ViewResponse;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.client.BlueprintTestClient;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractCloudbreakTestDto;

@Prototype
public class BlueprintTestDto extends AbstractCloudbreakTestDto<BlueprintV4Request, BlueprintV4Response, BlueprintTestDto> {

    public static final String BLUEPRINT = "BLUEPRINT";

    private static final String BLUEPRINT_RESOURCE_NAME = "blueprintName";

    private Collection<BlueprintV4ViewResponse> viewResponses;

    @Inject
    private BlueprintTestClient blueprintTestClient;

    public BlueprintTestDto(TestContext testContext) {
        super(new BlueprintV4Request(), testContext);
    }

    @Override
    public void cleanUp(TestContext context, CloudbreakClient cloudbreakClient) {
        LOGGER.info("Cleaning up blueprint with name: {}", getName());
        if (getResponse() != null) {
            when(blueprintTestClient.deleteV4(), key("delete-blueprint-" + getName()).withSkipOnFail(false));
        } else {
            LOGGER.info("Blueprint: {} response is null!", getName());
        }
    }

    public BlueprintTestDto valid() {
        return withName(getResourcePropertyProvider().getName(getCloudPlatform()))
                .withBlueprint("someBlueprint");
    }

    public BlueprintTestDto withName(String name) {
        getRequest().setName(name);
        setName(name);
        return this;
    }

    @Override
    public String getResourceNameType() {
        return BLUEPRINT_RESOURCE_NAME;
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