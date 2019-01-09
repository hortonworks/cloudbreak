package com.sequenceiq.it.cloudbreak.newway.assertion;

import java.util.Optional;

import com.sequenceiq.cloudbreak.api.endpoint.v4.cluster_template.responses.ClusterTemplateV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.cluster_template.ClusterTemplateV4Type;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.ClusterTemplateEntity;

public class CheckClusterTemplateType implements AssertionV2<ClusterTemplateEntity> {

    private ClusterTemplateV4Type expectedType;

    public CheckClusterTemplateType(ClusterTemplateV4Type expectedType) {
        this.expectedType = expectedType;
    }

    @Override
    public ClusterTemplateEntity doAssertion(TestContext testContext, ClusterTemplateEntity entity, CloudbreakClient cloudbreakClient) throws Exception {
        ClusterTemplateEntity clusterTemplate = testContext.get(ClusterTemplateEntity.class);
        Optional<ClusterTemplateV4Response> first = entity.getResponses().stream().filter(ct -> ct.getName().equals(clusterTemplate.getName())).findFirst();
        if (!first.isPresent()) {
            throw new IllegalArgumentException("No element in the result");
        }

        ClusterTemplateV4Response clusterTemplateV4Response = first.get();

        if (!expectedType.equals(clusterTemplateV4Response.getType())) {
            throw new IllegalArgumentException(String
                    .format("Mismatch type result, %s expected but got %s", expectedType, clusterTemplateV4Response.getType()));
        }
        return entity;
    }
}
