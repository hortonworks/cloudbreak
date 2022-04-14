package com.sequenceiq.it.cloudbreak.assertion.distrox;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.util.DistroxUtil;

public class DistroxScaleThresholdAssertions implements Assertion<DistroXTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistroxScaleThresholdAssertions.class);

    private final String hostGroupName;

    private final int scaleUpTarget;

    private final long threshold;

    public DistroxScaleThresholdAssertions(String hostGroupName, int scaleUpTarget, long threshold) {
        this.hostGroupName = hostGroupName;
        this.scaleUpTarget = scaleUpTarget;
        this.threshold = threshold;
    }

    private float getScalingThreshold() {
        float scalingPercentage = (float) threshold / 100;
        return scaleUpTarget * scalingPercentage;
    }

    @Override
    public DistroXTestDto doAssertion(TestContext testContext, DistroXTestDto testDto, CloudbreakClient client) throws Exception {
        DistroxUtil distroxUtil = new DistroxUtil();
        List<String> instanceIds = distroxUtil.getInstanceIds(testDto, client, hostGroupName);
        LOGGER.info(String.format("Found instanceIds after DistroX scaling: [%s]", instanceIds));
        int instanceCount = instanceIds.size();

        assertThat(
                String.format("Available '%s' node count '%d' is NOT match with the required (at least) scale percentage '%d'.",
                        hostGroupName, instanceCount, threshold),
                (float) instanceCount,
                greaterThanOrEqualTo(getScalingThreshold()));

        return testDto;
    }
}