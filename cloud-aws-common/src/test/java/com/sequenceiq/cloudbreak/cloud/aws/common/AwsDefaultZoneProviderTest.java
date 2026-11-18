package com.sequenceiq.cloudbreak.cloud.aws.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;

class AwsDefaultZoneProviderTest {

    private static final String GLOBAL_DEFAULT_ZONE = "us-east-1";

    private AwsDefaultZoneProvider underTest;

    @BeforeEach
    void setUp() {
        underTest = new AwsDefaultZoneProvider();
        ReflectionTestUtils.setField(underTest, "awsZoneParameterDefault", GLOBAL_DEFAULT_ZONE);
        ReflectionTestUtils.setField(underTest, "awsGovZoneParameterDefault", "us-gov-west-1");
    }

    @Test
    void getDefaultZoneFallsBackToGlobalDefaultWhenCredentialHasNoAwsParameter() {
        CloudCredential credential = new CloudCredential("id", "name", "account");

        String result = underTest.getDefaultZone(new AwsCredentialView(credential));

        assertThat(result).isEqualTo(GLOBAL_DEFAULT_ZONE);
    }

    @Test
    void getDefaultZoneUsesCredentialRegionWhenConfigured() {
        CloudCredential credential = new CloudCredential("id", "name",
                Map.of(AwsCredentialView.AWS, Map.of(AwsCredentialView.DEFAULT_REGION_KEY, "eu-central-1")),
                "account");

        String result = underTest.getDefaultZone(new AwsCredentialView(credential));

        assertThat(result).isEqualTo("eu-central-1");
    }
}
