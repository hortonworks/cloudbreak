package com.sequenceiq.it.cloudbreak.newway.assertion;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.ClusterTemplateV4Type.OTHER;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.DatalakeRequired.OPTIONAL;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.responses.ClusterTemplateV4Response;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.ClusterTemplateEntity;

public class CheckClusterTemplateGetResponse implements AssertionV2<ClusterTemplateEntity> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CheckClusterTemplateGetResponse.class);

    @Override
    public ClusterTemplateEntity doAssertion(TestContext testContext, ClusterTemplateEntity entity, CloudbreakClient client) throws Exception {
        Optional<ClusterTemplateV4Response> first = entity.getResponses().stream().filter(f -> f.getName().equals(entity.getName())).findFirst();
        if (!first.isPresent()) {
            throw new IllegalArgumentException("No element in the result");
        }

        ClusterTemplateV4Response clusterTemplateV4Response = first.get();

        if (clusterTemplateV4Response.getStackTemplate() == null) {
            throw new IllegalArgumentException("STack template is empty");
        }

        if (!OTHER.equals(clusterTemplateV4Response.getType())) {
            throw new IllegalArgumentException(String
                    .format("Mismatch type result, OTHER expected but got %s", clusterTemplateV4Response.getType()));
        }

        if (!OPTIONAL.equals(clusterTemplateV4Response.getDatalakeRequired())) {
            throw new IllegalArgumentException(String
                    .format("Mismatch datalake required result, OPTIONAL expected but got %s", clusterTemplateV4Response.getDatalakeRequired()));
        }

        if (!ResourceStatus.USER_MANAGED.equals(clusterTemplateV4Response.getStatus())) {
            throw new IllegalArgumentException(String
                    .format("Mismatch status result, USER_MANAGED expected but got %s", clusterTemplateV4Response.getStatus()));
        }

        return entity;
    }
}
