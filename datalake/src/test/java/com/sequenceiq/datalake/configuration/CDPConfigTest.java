package com.sequenceiq.datalake.configuration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Set;

import org.hamcrest.collection.IsMapContaining;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.datalake.service.sdx.CDPConfigKey;
import com.sequenceiq.sdx.api.model.SdxClusterShape;

@ExtendWith(MockitoExtension.class)
class CDPConfigTest {

    @Spy
    private Set<String> unsupportedRuntimes;

    @InjectMocks
    private CDPConfig cdpConfig;

    @Test
    void cdpStackRequests() {
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

    @Test
    void cdpDisabledStackRequests() {
        when(unsupportedRuntimes.contains("7.0.2")).thenReturn(true);
        Map<CDPConfigKey, StackV4Request> stackV4RequestMap = cdpConfig.cdpStackRequests();
        assertThat(stackV4RequestMap, not(IsMapContaining.hasKey(new CDPConfigKey(CloudPlatform.AWS, SdxClusterShape.LIGHT_DUTY, "7.0.2"))));
        assertThat(stackV4RequestMap, not(IsMapContaining.hasKey(new CDPConfigKey(CloudPlatform.AWS, SdxClusterShape.MEDIUM_DUTY_HA, "7.0.2"))));
        assertThat(stackV4RequestMap, not(IsMapContaining.hasKey(new CDPConfigKey(CloudPlatform.AZURE, SdxClusterShape.LIGHT_DUTY, "7.0.2"))));
        assertThat(stackV4RequestMap, not(IsMapContaining.hasKey(new CDPConfigKey(CloudPlatform.AZURE, SdxClusterShape.MEDIUM_DUTY_HA, "7.0.2"))));
        assertThat(stackV4RequestMap, not(IsMapContaining.hasKey(new CDPConfigKey(CloudPlatform.YARN, SdxClusterShape.LIGHT_DUTY, "7.0.2"))));
        assertThat(stackV4RequestMap, not(IsMapContaining.hasKey(new CDPConfigKey(CloudPlatform.YARN, SdxClusterShape.MEDIUM_DUTY_HA, "7.0.2"))));
        assertThat(stackV4RequestMap, not(IsMapContaining.hasKey(new CDPConfigKey(CloudPlatform.MOCK, SdxClusterShape.LIGHT_DUTY, "7.0.2"))));
        assertThat(stackV4RequestMap, IsMapContaining.hasKey(new CDPConfigKey(CloudPlatform.AWS, SdxClusterShape.LIGHT_DUTY, "7.1.0")));
        assertThat(stackV4RequestMap, IsMapContaining.hasKey(new CDPConfigKey(CloudPlatform.AWS, SdxClusterShape.MEDIUM_DUTY_HA, "7.1.0")));
        assertThat(stackV4RequestMap, IsMapContaining.hasKey(new CDPConfigKey(CloudPlatform.AZURE, SdxClusterShape.LIGHT_DUTY, "7.1.0")));
        assertThat(stackV4RequestMap, IsMapContaining.hasKey(new CDPConfigKey(CloudPlatform.AZURE, SdxClusterShape.MEDIUM_DUTY_HA, "7.1.0")));
        assertThat(stackV4RequestMap, IsMapContaining.hasKey(new CDPConfigKey(CloudPlatform.YARN, SdxClusterShape.LIGHT_DUTY, "7.1.0")));
        assertThat(stackV4RequestMap, IsMapContaining.hasKey(new CDPConfigKey(CloudPlatform.YARN, SdxClusterShape.MEDIUM_DUTY_HA, "7.1.0")));
        assertThat(stackV4RequestMap, IsMapContaining.hasKey(new CDPConfigKey(CloudPlatform.MOCK, SdxClusterShape.LIGHT_DUTY, "7.1.0")));
    }

    @Test
    void cdpDisabledEveryStackRequests() {
        when(unsupportedRuntimes.contains("7.0.2")).thenReturn(true);
        when(unsupportedRuntimes.contains("7.1.0")).thenReturn(true);
        Map<CDPConfigKey, StackV4Request> stackV4RequestMap = cdpConfig.cdpStackRequests();
        assertEquals(0L, stackV4RequestMap.size());
        assertThat(stackV4RequestMap, not(IsMapContaining.hasKey(new CDPConfigKey(CloudPlatform.AWS, SdxClusterShape.LIGHT_DUTY, "7.0.2"))));
        assertThat(stackV4RequestMap, not(IsMapContaining.hasKey(new CDPConfigKey(CloudPlatform.AWS, SdxClusterShape.MEDIUM_DUTY_HA, "7.0.2"))));
        assertThat(stackV4RequestMap, not(IsMapContaining.hasKey(new CDPConfigKey(CloudPlatform.AZURE, SdxClusterShape.LIGHT_DUTY, "7.0.2"))));
        assertThat(stackV4RequestMap, not(IsMapContaining.hasKey(new CDPConfigKey(CloudPlatform.AZURE, SdxClusterShape.MEDIUM_DUTY_HA, "7.0.2"))));
        assertThat(stackV4RequestMap, not(IsMapContaining.hasKey(new CDPConfigKey(CloudPlatform.YARN, SdxClusterShape.LIGHT_DUTY, "7.0.2"))));
        assertThat(stackV4RequestMap, not(IsMapContaining.hasKey(new CDPConfigKey(CloudPlatform.YARN, SdxClusterShape.MEDIUM_DUTY_HA, "7.0.2"))));
        assertThat(stackV4RequestMap, not(IsMapContaining.hasKey(new CDPConfigKey(CloudPlatform.MOCK, SdxClusterShape.LIGHT_DUTY, "7.0.2"))));
        assertThat(stackV4RequestMap, not(IsMapContaining.hasKey(new CDPConfigKey(CloudPlatform.AWS, SdxClusterShape.LIGHT_DUTY, "7.1.0"))));
        assertThat(stackV4RequestMap, not(IsMapContaining.hasKey(new CDPConfigKey(CloudPlatform.AWS, SdxClusterShape.MEDIUM_DUTY_HA, "7.1.0"))));
        assertThat(stackV4RequestMap, not(IsMapContaining.hasKey(new CDPConfigKey(CloudPlatform.AZURE, SdxClusterShape.LIGHT_DUTY, "7.1.0"))));
        assertThat(stackV4RequestMap, not(IsMapContaining.hasKey(new CDPConfigKey(CloudPlatform.AZURE, SdxClusterShape.MEDIUM_DUTY_HA, "7.1.0"))));
        assertThat(stackV4RequestMap, not(IsMapContaining.hasKey(new CDPConfigKey(CloudPlatform.YARN, SdxClusterShape.LIGHT_DUTY, "7.1.0"))));
        assertThat(stackV4RequestMap, not(IsMapContaining.hasKey(new CDPConfigKey(CloudPlatform.YARN, SdxClusterShape.MEDIUM_DUTY_HA, "7.1.0"))));
        assertThat(stackV4RequestMap, not(IsMapContaining.hasKey(new CDPConfigKey(CloudPlatform.MOCK, SdxClusterShape.LIGHT_DUTY, "7.1.0"))));
    }
}