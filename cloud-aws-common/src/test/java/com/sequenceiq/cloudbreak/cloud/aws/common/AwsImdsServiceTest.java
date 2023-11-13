package com.sequenceiq.cloudbreak.cloud.aws.common;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Image;

@ExtendWith(MockitoExtension.class)
public class AwsImdsServiceTest {

    @InjectMocks
    private AwsImdsService underTest;

    @Mock
    private EntitlementService entitlementService;

    @Test
    void testImdsWhenNotEntitled() {
        when(entitlementService.isAwsImdsV2Enforced(any())).thenReturn(Boolean.FALSE);
        assertFalse(underTest.isImdsV2Supported(cloudStack(Map.of("imds", "v2")), null));
    }

    @Test
    void testImdsWhenNotPresent() {
        when(entitlementService.isAwsImdsV2Enforced(any())).thenReturn(Boolean.TRUE);
        assertFalse(underTest.isImdsV2Supported(cloudStack(Map.of()), null));
        assertFalse(underTest.isImdsV2Supported(cloudStack(null), null));
        assertFalse(underTest.isImdsV2Supported(null, null));
    }

    @Test
    void testImdsWhenNotV2() {
        when(entitlementService.isAwsImdsV2Enforced(any())).thenReturn(Boolean.TRUE);
        assertFalse(underTest.isImdsV2Supported(cloudStack(Map.of("imds", "anything_else")), null));
    }

    @Test
    void testImdsWhenV2() {
        when(entitlementService.isAwsImdsV2Enforced(any())).thenReturn(Boolean.TRUE);
        assertTrue(underTest.isImdsV2Supported(cloudStack(Map.of("imds", "v2")), null));
    }

    private CloudStack cloudStack(Map<String, String> packageVersions) {
        Image image = new Image(null, null, null, null, null, null, null, packageVersions, null, null);
        return new CloudStack(List.of(), null, image, Map.of(), Map.of(), null, null, null, null, null, null, null);
    }
}
