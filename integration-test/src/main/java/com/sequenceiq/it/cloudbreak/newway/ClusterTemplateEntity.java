package com.sequenceiq.it.cloudbreak.newway;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.sequenceiq.cloudbreak.api.model.template.ClusterTemplateRequest;
import com.sequenceiq.cloudbreak.api.model.template.ClusterTemplateResponse;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ClusterTemplateEntity extends AbstractCloudbreakEntity<ClusterTemplateRequest, ClusterTemplateResponse, ClusterTemplateEntity> {
    public static final String CLUSTER_TEMPLATE = "CLUSTER_TEMPLATE";

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterTemplateEntity.class);

    ClusterTemplateEntity(String newId) {
        super(newId);
        setRequest(new ClusterTemplateRequest());
    }

    ClusterTemplateEntity() {
        this(CLUSTER_TEMPLATE);
    }

    public ClusterTemplateEntity(TestContext testContext) {
        super(new ClusterTemplateRequest(), testContext);
    }

    public ClusterTemplateEntity withName(String name) {
        getRequest().setName(name);
        setName(name);
        return this;
    }

    public ClusterTemplateEntity withDescription(String description) {
        getRequest().setDescription(description);
        return this;
    }

    private static class MapTypeReference extends TypeReference<Map<String, Object>> {
    }
}
