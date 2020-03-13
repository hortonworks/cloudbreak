package com.sequenceiq.datalake.configuration;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.datalake.service.sdx.CDPConfigKey;
import com.sequenceiq.sdx.api.model.SdxClusterShape;

@ExtendWith(MockitoExtension.class)
class CDPConfigTest {

    @Spy
    private Set<String> unsupportedRuntimes;

    @InjectMocks
    private CDPConfigService cdpConfigService;

    @Test
    void cdpStackRequests() {
        cdpConfigService.initCdpStackRequests();
//        assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.AWS, SdxClusterShape.LIGHT_DUTY, "7.0.2")));
        assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.AWS, SdxClusterShape.LIGHT_DUTY, "7.1.0")));
//        assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.AWS, SdxClusterShape.MEDIUM_DUTY_HA, "7.0.2")));
        assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.AWS, SdxClusterShape.MEDIUM_DUTY_HA, "7.1.0")));
//        assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.AZURE, SdxClusterShape.LIGHT_DUTY, "7.0.2")));
        assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.AZURE, SdxClusterShape.LIGHT_DUTY, "7.1.0")));
//        assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.AZURE, SdxClusterShape.MEDIUM_DUTY_HA, "7.0.2")));
        assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.AZURE, SdxClusterShape.MEDIUM_DUTY_HA, "7.1.0")));
//        assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.YARN, SdxClusterShape.LIGHT_DUTY, "7.0.2")));
        assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.YARN, SdxClusterShape.LIGHT_DUTY, "7.1.0")));
//        assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.YARN, SdxClusterShape.MEDIUM_DUTY_HA, "7.0.2")));
        assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.YARN, SdxClusterShape.MEDIUM_DUTY_HA, "7.1.0")));
//        assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.MOCK, SdxClusterShape.LIGHT_DUTY, "7.0.2")));
        assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.MOCK, SdxClusterShape.LIGHT_DUTY, "7.1.0")));
    }

    @Test
    void cdpDisabledStackRequests() {
//        when(unsupportedRuntimes.contains("7.0.2")).thenReturn(true);
        cdpConfigService.initCdpStackRequests();
        assertNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.AWS, SdxClusterShape.LIGHT_DUTY, "7.0.2")));
        assertNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.AWS, SdxClusterShape.MEDIUM_DUTY_HA, "7.0.2")));
        assertNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.AZURE, SdxClusterShape.LIGHT_DUTY, "7.0.2")));
        assertNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.AZURE, SdxClusterShape.MEDIUM_DUTY_HA, "7.0.2")));
        assertNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.YARN, SdxClusterShape.LIGHT_DUTY, "7.0.2")));
        assertNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.YARN, SdxClusterShape.MEDIUM_DUTY_HA, "7.0.2")));
        assertNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.MOCK, SdxClusterShape.LIGHT_DUTY, "7.0.2")));
        assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.AWS, SdxClusterShape.LIGHT_DUTY, "7.1.0")));
        assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.AWS, SdxClusterShape.MEDIUM_DUTY_HA, "7.1.0")));
        assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.AZURE, SdxClusterShape.LIGHT_DUTY, "7.1.0")));
        assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.AZURE, SdxClusterShape.MEDIUM_DUTY_HA, "7.1.0")));
        assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.YARN, SdxClusterShape.LIGHT_DUTY, "7.1.0")));
        assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.YARN, SdxClusterShape.MEDIUM_DUTY_HA, "7.1.0")));
        assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.MOCK, SdxClusterShape.LIGHT_DUTY, "7.1.0")));
    }

    @Test
    void cdpDisabledEveryStackRequests() {
        // when(unsupportedRuntimes.contains("7.0.2")).thenReturn(true);
        when(unsupportedRuntimes.contains("7.1.0")).thenReturn(true);
        cdpConfigService.initCdpStackRequests();
        assertNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.AWS, SdxClusterShape.LIGHT_DUTY, "7.0.2")));
        assertNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.AWS, SdxClusterShape.MEDIUM_DUTY_HA, "7.0.2")));
        assertNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.AZURE, SdxClusterShape.LIGHT_DUTY, "7.0.2")));
        assertNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.AZURE, SdxClusterShape.MEDIUM_DUTY_HA, "7.0.2")));
        assertNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.YARN, SdxClusterShape.LIGHT_DUTY, "7.0.2")));
        assertNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.YARN, SdxClusterShape.MEDIUM_DUTY_HA, "7.0.2")));
        assertNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.MOCK, SdxClusterShape.LIGHT_DUTY, "7.0.2")));
        assertNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.AWS, SdxClusterShape.LIGHT_DUTY, "7.1.0")));
        assertNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.AWS, SdxClusterShape.MEDIUM_DUTY_HA, "7.1.0")));
        assertNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.AZURE, SdxClusterShape.LIGHT_DUTY, "7.1.0")));
        assertNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.AZURE, SdxClusterShape.MEDIUM_DUTY_HA, "7.1.0")));
        assertNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.YARN, SdxClusterShape.LIGHT_DUTY, "7.1.0")));
        assertNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.YARN, SdxClusterShape.MEDIUM_DUTY_HA, "7.1.0")));
        assertNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.MOCK, SdxClusterShape.LIGHT_DUTY, "7.1.0")));
    }
}