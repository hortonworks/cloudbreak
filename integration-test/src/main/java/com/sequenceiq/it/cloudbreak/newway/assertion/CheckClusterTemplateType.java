package com.sequenceiq.it.cloudbreak.newway.assertion;

import java.util.Optional;

import com.sequenceiq.cloudbreak.api.model.template.ClusterTemplateResponse;
import com.sequenceiq.cloudbreak.api.model.template.ClusterTemplateType;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.ClusterTemplateEntity;

public class CheckClusterTemplateType implements AssertionV2<ClusterTemplateEntity> {

    private ClusterTemplateType expectedType;

    public CheckClusterTemplateType(ClusterTemplateType expectedType) {
        this.expectedType = expectedType;
    }

    @Override
    public ClusterTemplateEntity doAssertion(TestContext testContext, ClusterTemplateEntity entity, CloudbreakClient cloudbreakClient) throws Exception {
        ClusterTemplateEntity clusterTemplate = testContext.get(ClusterTemplateEntity.class);
        Optional<ClusterTemplateResponse> first = entity.getResponses().stream().filter(ct -> ct.getName().equals(clusterTemplate.getName())).findFirst();
        if (!first.isPresent()) {
            throw new IllegalArgumentException("No element in the result");
        }

        ClusterTemplateResponse clusterTemplateResponse = first.get();

        if (!expectedType.equals(clusterTemplateResponse.getType())) {
            throw new IllegalArgumentException(String
                    .format("Mismatch type result, %s expected but got %s", expectedType, clusterTemplateResponse.getType()));
        }
        return entity;
    }
}
