package com.sequenceiq.it.cloudbreak.newway.assertion;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.responses.ClusterTemplateV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.ClusterTemplateEntity;

public class CheckStackTemplateAfterClusterTemplateCreation implements AssertionV2<ClusterTemplateEntity> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CheckStackTemplateAfterClusterTemplateCreation.class);

    @Override
    public ClusterTemplateEntity doAssertion(TestContext testContext, ClusterTemplateEntity entity, CloudbreakClient client) {
        Optional<ClusterTemplateV4Response> first = entity.getResponses().stream().filter(f -> f.getName().equals(entity.getName())).findFirst();
        if (!first.isPresent()) {
            throw new IllegalArgumentException("No element in the result");
        }

        ClusterTemplateV4Response clusterTemplateV4Response = first.get();

        StackV4Request stackTemplate = clusterTemplateV4Response.getStackTemplate();
        if (stackTemplate == null) {
            throw new IllegalArgumentException("Stack template is empty");
        }

        if (!StringUtils.isEmpty(stackTemplate.getName())) {
            throw new IllegalArgumentException("Stack template name should be empty!");
        }

        if (!StringUtils.isEmpty(stackTemplate.getCluster().getPassword())) {
            throw new IllegalArgumentException("Ambari password should be empty!");
        }

        if (!StringUtils.isEmpty(stackTemplate.getCluster().getUserName())) {
            throw new IllegalArgumentException("Ambari username should be empty!");
        }

        return entity;
    }
}
