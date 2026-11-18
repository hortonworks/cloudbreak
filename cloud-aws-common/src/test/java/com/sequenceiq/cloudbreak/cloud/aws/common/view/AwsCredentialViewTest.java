package com.sequenceiq.cloudbreak.cloud.aws.common.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;

class AwsCredentialViewTest {

    @Test
    void getDefaultRegionReturnsNullWhenAwsParameterIsAbsent() {
        CloudCredential credential = new CloudCredential("id", "name", "account");

        AwsCredentialView underTest = new AwsCredentialView(credential);

        assertThat(underTest.getDefaultRegion()).isNull();
        assertThat(underTest.getKeyBased()).isNull();
        assertThat(underTest.getRoleBased()).isNull();
    }

    @Test
    void getDefaultRegionReturnsConfiguredRegionWhenPresent() {
        Map<String, Object> aws = new HashMap<>();
        aws.put(AwsCredentialView.DEFAULT_REGION_KEY, "eu-west-1");
        CloudCredential credential = new CloudCredential("id", "name", Map.of(AwsCredentialView.AWS, aws), "account");

        AwsCredentialView underTest = new AwsCredentialView(credential);

        assertThat(underTest.getDefaultRegion()).isEqualTo("eu-west-1");
    }
}
