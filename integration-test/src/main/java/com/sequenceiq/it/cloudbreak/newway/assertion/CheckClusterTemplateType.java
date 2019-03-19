package com.sequenceiq.it.cloudbreak.newway.assertion;

import java.util.Optional;

import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.responses.ClusterTemplateV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.ClusterTemplateV4Type;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.clustertemplate.ClusterTemplateTestDto;

public class CheckClusterTemplateType implements AssertionV2<ClusterTemplateTestDto> {

    private ClusterTemplateV4Type expectedType;

    public CheckClusterTemplateType(ClusterTemplateV4Type expectedType) {
        this.expectedType = expectedType;
    }

    @Override
    public ClusterTemplateTestDto doAssertion(TestContext testContext, ClusterTemplateTestDto testDto, CloudbreakClient cloudbreakClient) {
        ClusterTemplateTestDto clusterTemplate = testContext.get(ClusterTemplateTestDto.class);
        Optional<ClusterTemplateV4Response> first = testDto.getResponses().stream().filter(ct -> ct.getName().equals(clusterTemplate.getName())).findFirst();
        if (!first.isPresent()) {
            throw new IllegalArgumentException("No element in the result");
        }

        ClusterTemplateV4Response clusterTemplateV4Response = first.get();

        if (!expectedType.equals(clusterTemplateV4Response.getType())) {
            throw new IllegalArgumentException(String
                    .format("Mismatch type result, %s expected but got %s", expectedType, clusterTemplateV4Response.getType()));
        }
        return testDto;
    }
}
