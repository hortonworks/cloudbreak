package com.sequenceiq.it.cloudbreak.dto.util;

import java.util.List;

import javax.ws.rs.core.Response;

import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractSdxTestDto;

@Prototype
public class SdxEventTestDto extends AbstractSdxTestDto<Object, Response, SdxEventTestDto> {
    private String environmentCrn;

    private List<StructuredEventType> types;

    private Integer page;

    private Integer size;

    protected SdxEventTestDto(TestContext testContext) {
        super(null, testContext);
    }

    public SdxEventTestDto withEnvironmentCrn(String environmentCrn) {
        this.environmentCrn = environmentCrn;
        return this;
    }

    public SdxEventTestDto withTypes(List<StructuredEventType> types) {
        this.types = types;
        return this;
    }

    public SdxEventTestDto withPage(Integer page) {
        this.page = page;
        return this;
    }

    public SdxEventTestDto withSize(Integer size) {
        this.size = size;
        return this;
    }

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public List<StructuredEventType> getTypes() {
        return types;
    }

    public Integer getPage() {
        return page;
    }

    public Integer getSize() {
        return size;
    }

    public String argsToString() {
        return String.format(
                "{EnvironmentCrn: \"%s\", Types: \"%s\", Page: \"%s\", Size: \"%s\"}",
                environmentCrn, types, page, size
        );
    }
}
