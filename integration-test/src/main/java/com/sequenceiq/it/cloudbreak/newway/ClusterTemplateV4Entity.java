package com.sequenceiq.it.cloudbreak.newway;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.sequenceiq.cloudbreak.api.endpoint.v4.cluster_template.requests.ClusterTemplateV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.cluster_template.responses.ClusterTemplateV4Response;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ClusterTemplateV4Entity extends AbstractCloudbreakEntity<ClusterTemplateV4Request, ClusterTemplateV4Response, ClusterTemplateV4Entity> {
    public static final String CLUSTER_TEMPLATE = "CLUSTER_TEMPLATE";

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterTemplateV4Entity.class);

    ClusterTemplateV4Entity(String newId) {
        super(newId);
        setRequest(new ClusterTemplateV4Request());
    }

    ClusterTemplateV4Entity() {
        this(CLUSTER_TEMPLATE);
    }

    public ClusterTemplateV4Entity(TestContext testContext) {
        super(new ClusterTemplateV4Request(), testContext);
    }

    public ClusterTemplateV4Entity withName(String name) {
        getRequest().setName(name);
        setName(name);
        return this;
    }

    public ClusterTemplateV4Entity withDescription(String description) {
        getRequest().setDescription(description);
        return this;
    }

    private static class MapTypeReference extends TypeReference<Map<String, Object>> {
    }
}
