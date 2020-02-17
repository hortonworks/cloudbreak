package com.sequenceiq.datalake.configuration;

import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Map;

import org.hamcrest.collection.IsMapContaining;
import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.datalake.service.sdx.CDPConfigKey;
import com.sequenceiq.sdx.api.model.SdxClusterShape;

class CDPConfigTest {

    @Test
    void cdpStackRequests() {
        CDPConfig cdpConfig = new CDPConfig();
        Map<CDPConfigKey, StackV4Request> stackV4RequestMap = cdpConfig.cdpStackRequests();
        assertThat(stackV4RequestMap, IsMapContaining.hasKey(new CDPConfigKey(CloudPlatform.AWS, SdxClusterShape.LIGHT_DUTY, "7.0.2")));
        assertThat(stackV4RequestMap, IsMapContaining.hasKey(new CDPConfigKey(CloudPlatform.AWS, SdxClusterShape.LIGHT_DUTY, "7.1.0")));
        assertThat(stackV4RequestMap, IsMapContaining.hasKey(new CDPConfigKey(CloudPlatform.AWS, SdxClusterShape.MEDIUM_DUTY_HA, "7.0.2")));
        assertThat(stackV4RequestMap, IsMapContaining.hasKey(new CDPConfigKey(CloudPlatform.AWS, SdxClusterShape.MEDIUM_DUTY_HA, "7.1.0")));
        assertThat(stackV4RequestMap, IsMapContaining.hasKey(new CDPConfigKey(CloudPlatform.AZURE, SdxClusterShape.LIGHT_DUTY, "7.0.2")));
        assertThat(stackV4RequestMap, IsMapContaining.hasKey(new CDPConfigKey(CloudPlatform.AZURE, SdxClusterShape.LIGHT_DUTY, "7.1.0")));
        assertThat(stackV4RequestMap, IsMapContaining.hasKey(new CDPConfigKey(CloudPlatform.AZURE, SdxClusterShape.MEDIUM_DUTY_HA, "7.0.2")));
        assertThat(stackV4RequestMap, IsMapContaining.hasKey(new CDPConfigKey(CloudPlatform.AZURE, SdxClusterShape.MEDIUM_DUTY_HA, "7.1.0")));
        assertThat(stackV4RequestMap, IsMapContaining.hasKey(new CDPConfigKey(CloudPlatform.YARN, SdxClusterShape.LIGHT_DUTY, "7.0.2")));
        assertThat(stackV4RequestMap, IsMapContaining.hasKey(new CDPConfigKey(CloudPlatform.YARN, SdxClusterShape.LIGHT_DUTY, "7.1.0")));
        assertThat(stackV4RequestMap, IsMapContaining.hasKey(new CDPConfigKey(CloudPlatform.YARN, SdxClusterShape.MEDIUM_DUTY_HA, "7.0.2")));
        assertThat(stackV4RequestMap, IsMapContaining.hasKey(new CDPConfigKey(CloudPlatform.YARN, SdxClusterShape.MEDIUM_DUTY_HA, "7.1.0")));
        assertThat(stackV4RequestMap, IsMapContaining.hasKey(new CDPConfigKey(CloudPlatform.MOCK, SdxClusterShape.LIGHT_DUTY, "7.0.2")));
        assertThat(stackV4RequestMap, IsMapContaining.hasKey(new CDPConfigKey(CloudPlatform.MOCK, SdxClusterShape.LIGHT_DUTY, "7.1.0")));
    }
}