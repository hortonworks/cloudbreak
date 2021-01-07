package com.sequenceiq.cloudbreak.cloud.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

public class CloudResourceTest {

    @Test
    void isStackAwareWhenDefault() {
        CloudResource cloudResource = generateCloudResourceStub().build();

        assertThat(cloudResource).isNotNull();
        assertThat(cloudResource.isStackAware()).isTrue();
    }

    @Test
    void isStackAwareWhenTrueIsSet() {
        CloudResource cloudResource = generateCloudResourceStub().stackAware(true).build();

        assertThat(cloudResource).isNotNull();
        assertThat(cloudResource.isStackAware()).isTrue();
    }

    @Test
    void isNonStackAwareWhenFalseIsSet() {
        CloudResource cloudResource = generateCloudResourceStub().stackAware(false).build();

        assertThat(cloudResource).isNotNull();
        assertThat(cloudResource.isStackAware()).isFalse();
    }

    private CloudResource.Builder generateCloudResourceStub() {
        return CloudResource.builder()
                .type(ResourceType.AZURE_MANAGED_IMAGE)
                .name("test-image")
                .status(CommonStatus.REQUESTED)
                .params(Map.of());
    }

}