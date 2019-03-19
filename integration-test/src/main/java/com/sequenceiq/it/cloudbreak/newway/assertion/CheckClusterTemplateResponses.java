package com.sequenceiq.it.cloudbreak.newway.assertion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.clustertemplate.ClusterTemplateTestDto;

public class CheckClusterTemplateResponses implements AssertionV2<ClusterTemplateTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CheckClusterTemplateResponses.class);

    private int expectedSize;

    public CheckClusterTemplateResponses(int expectedSize) {
        this.expectedSize = expectedSize;
    }

    @Override
    public ClusterTemplateTestDto doAssertion(TestContext testContext, ClusterTemplateTestDto testDto, CloudbreakClient client) {
        if (testDto.getResponses().size() != expectedSize) {
            throw new IllegalArgumentException(String.format("expected size is %s but got %s", expectedSize, testDto.getResponses().size()));
        }
        return testDto;
    }
}
