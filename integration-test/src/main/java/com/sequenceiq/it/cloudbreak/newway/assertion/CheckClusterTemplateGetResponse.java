package com.sequenceiq.it.cloudbreak.newway.assertion;

import static com.sequenceiq.cloudbreak.api.model.template.ClusterTemplateType.OTHER;
import static com.sequenceiq.cloudbreak.api.model.template.DatalakeRequired.OPTIONAL;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.model.ResourceStatus;
import com.sequenceiq.cloudbreak.api.model.template.ClusterTemplateResponse;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.ClusterTemplateEntity;

public class CheckClusterTemplateGetResponse implements AssertionV2<ClusterTemplateEntity> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CheckClusterTemplateGetResponse.class);

    @Override
    public ClusterTemplateEntity doAssertion(TestContext testContext, ClusterTemplateEntity entity, CloudbreakClient client) throws Exception {
        Optional<ClusterTemplateResponse> first = entity.getResponses().stream().filter(f -> f.getName().equals(entity.getName())).findFirst();
        if (!first.isPresent()) {
            throw new IllegalArgumentException("No element in the result");
        }

        ClusterTemplateResponse clusterTemplateResponse = first.get();

        if (clusterTemplateResponse.getStackTemplate() == null) {
            throw new IllegalArgumentException("STack template is empty");
        }

        if (!OTHER.equals(clusterTemplateResponse.getType())) {
            throw new IllegalArgumentException(String
                    .format("Mismatch type result, OTHER expected but got %s", clusterTemplateResponse.getType()));
        }

        if (!OPTIONAL.equals(clusterTemplateResponse.getDatalakeRequired())) {
            throw new IllegalArgumentException(String
                    .format("Mismatch datalake required result, OPTIONAL expected but got %s", clusterTemplateResponse.getDatalakeRequired()));
        }

        if (!ResourceStatus.USER_MANAGED.equals(clusterTemplateResponse.getStatus())) {
            throw new IllegalArgumentException(String
                    .format("Mismatch status result, USER_MANAGED expected but got %s", clusterTemplateResponse.getStatus()));
        }

        return entity;
    }
}
