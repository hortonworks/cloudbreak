package com.sequenceiq.it.cloudbreak.dto.blueprint;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.requests.BlueprintV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.responses.BlueprintV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.responses.BlueprintV4ViewResponse;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractCloudbreakTestDto;

@Prototype
public class BlueprintTestDto extends AbstractCloudbreakTestDto<BlueprintV4Request, BlueprintV4Response, BlueprintTestDto> {

    private static final String BLUEPRINT_RESOURCE_NAME = "blueprintName";

    private Collection<BlueprintV4ViewResponse> viewResponses;

    private String accountId;

    public BlueprintTestDto(TestContext testContext) {
        super(new BlueprintV4Request(), testContext);
    }

    @Override
    public void deleteForCleanup() {
        getClientForCleanup().getDefaultClient(getTestContext()).blueprintV4Endpoint().deleteByCrn(0L, getCrn());
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

    public BlueprintTestDto withBlueprint(String blueprint) {
        getRequest().setBlueprint(blueprint);
        return this;
    }

    public BlueprintTestDto withAccountId(String accountId) {
        this.accountId = accountId;
        return this;
    }

    public String getAccountId() {
        return accountId;
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
    public String getCrn() {
        return getResponse().getCrn();
    }

    @Override
    public int order() {
        return 500;
    }
}