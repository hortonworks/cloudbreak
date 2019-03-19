package com.sequenceiq.it.cloudbreak.newway.dto;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.requests.ClusterTemplateV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.responses.ClusterTemplateV4Response;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ClusterTemplateV4TestDto extends AbstractCloudbreakTestDto<ClusterTemplateV4Request, ClusterTemplateV4Response, ClusterTemplateV4TestDto> {
    public static final String CLUSTER_TEMPLATE = "CLUSTER_TEMPLATE";

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterTemplateV4TestDto.class);

    ClusterTemplateV4TestDto(String newId) {
        super(newId);
        setRequest(new ClusterTemplateV4Request());
    }

    ClusterTemplateV4TestDto() {
        this(CLUSTER_TEMPLATE);
    }

    public ClusterTemplateV4TestDto(TestContext testContext) {
        super(new ClusterTemplateV4Request(), testContext);
    }

    public ClusterTemplateV4TestDto withName(String name) {
        getRequest().setName(name);
        setName(name);
        return this;
    }

    public ClusterTemplateV4TestDto withDescription(String description) {
        getRequest().setDescription(description);
        return this;
    }

    private static class MapTypeReference extends TypeReference<Map<String, Object>> {
    }
}
