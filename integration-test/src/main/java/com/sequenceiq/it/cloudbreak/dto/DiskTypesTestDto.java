package com.sequenceiq.it.cloudbreak.dto;

import java.util.Collection;

import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.PlatformDisksV4Response;
import com.sequenceiq.it.cloudbreak.context.TestContext;

public class DiskTypesTestDto extends AbstractCloudbreakTestDto<Integer, PlatformDisksV4Response, DiskTypesTestDto> {
    public static final String DISKTYPES = "DISKTYPES";

    private String type;

    private Collection<String> responses;

    private final Integer request = 1;

    protected DiskTypesTestDto(Integer request, TestContext testContext) {
        super(request, testContext);
    }

    public Collection<String> getByFilterResponses() {
        return responses;
    }

    public void setByFilterResponses(Collection<String> responses) {
        this.responses = responses;
    }

    public String getType() {
        return type;
    }

    public DiskTypesTestDto withType(String type) {
        this.type = type;
        return this;
    }

    public void setType(String type) {
        this.type = type;
    }
}
