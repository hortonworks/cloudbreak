package com.sequenceiq.cloudbreak.cm.util;

import static com.sequenceiq.cloudbreak.cm.util.ClouderaManagerHostHealthUtil.HOST_SCM_HEALTH;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;

import com.cloudera.api.swagger.model.ApiHealthCheck;
import com.cloudera.api.swagger.model.ApiHealthSummary;
import com.cloudera.api.swagger.model.ApiHost;

class ClouderaManagerHostHealthUtilTest {

    @Test
    void returnsEmptyForNullHealthChecks() {
        ApiHost host = new ApiHost();
        host.setHealthChecks(null);

        assertThat(ClouderaManagerHostHealthUtil.getHostScmHealthChecks(host)).isEmpty();
    }

    @Test
    void returnsEmptyForEmptyHealthChecks() {
        ApiHost host = new ApiHost().healthChecks(List.of());

        assertThat(ClouderaManagerHostHealthUtil.getHostScmHealthChecks(host)).isEmpty();
    }

    @Test
    void returnsEmptyWhenNoAgentHealthCheckIsPresent() {
        ApiHost host = new ApiHost().healthChecks(List.of(new ApiHealthCheck().name("OTHER_CHECK").summary(ApiHealthSummary.BAD)));

        assertThat(ClouderaManagerHostHealthUtil.getHostScmHealthChecks(host)).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(ApiHealthSummary.class)
    @NullSource
    void preservesSummaryAndExplanation(ApiHealthSummary summary) {
        ApiHealthCheck check = new ApiHealthCheck().name(HOST_SCM_HEALTH).summary(summary).explanation("Agent status details");
        ApiHost host = new ApiHost().healthChecks(List.of(
                new ApiHealthCheck().name("OTHER_CHECK").summary(ApiHealthSummary.BAD), check));

        assertThat(ClouderaManagerHostHealthUtil.getHostScmHealthChecks(host)).containsExactly(check);
    }

    @Test
    void preservesDuplicateChecksInOrder() {
        ApiHealthCheck goodCheck = new ApiHealthCheck().name(HOST_SCM_HEALTH).summary(ApiHealthSummary.GOOD);
        ApiHealthCheck badCheck = new ApiHealthCheck().name(HOST_SCM_HEALTH).summary(ApiHealthSummary.BAD);
        ApiHost host = new ApiHost().healthChecks(List.of(goodCheck, badCheck));

        assertThat(ClouderaManagerHostHealthUtil.getHostScmHealthChecks(host)).containsExactly(goodCheck, badCheck);
    }
}
