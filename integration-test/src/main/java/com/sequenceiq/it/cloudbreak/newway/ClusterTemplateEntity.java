package com.sequenceiq.it.cloudbreak.newway;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.api.model.template.ClusterTemplateRequest;
import com.sequenceiq.cloudbreak.api.model.template.ClusterTemplateResponse;
import com.sequenceiq.cloudbreak.api.model.v2.StackV2Request;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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

    public ClusterTemplateEntity withCloudPlatform(String cloudPlatform) {
        getRequest().setCloudPlatform(cloudPlatform);
        return this;
    }

    public ClusterTemplateEntity withClusterTemplate(StackV2Request clusterTemplate) {
        getRequest().setTemplate(clusterTemplate);
        return this;
    }

    public ClusterTemplateEntity withClusterTemplateJSON(String clusterTemplate) {
        StackV2Request stackRequest = new StackV2Request();
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> map = new HashMap<>();
        TypeReference<Map<String, Object>> typeRef = new MapTypeReference();

        try {
            map = mapper.readValue(clusterTemplate, typeRef);
        } catch (IOException e) {
            LOGGER.info("Cluster Template JSON string to Map exception ::: " + e);
        }
        stackRequest.setParameters(map);

        getRequest().setTemplate(stackRequest);
        return this;
    }

    private static class MapTypeReference extends TypeReference<Map<String, Object>> {
    }
}
