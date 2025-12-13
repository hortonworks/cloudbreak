package com.sequenceiq.cloudbreak.cloud.gcp;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;

@ExtendWith(MockitoExtension.class)
public class GcpIdentityServiceTest {

    @InjectMocks
    private GcpIdentityService underTest;

    @Mock
    private GcpStackUtil gcpStackUtil;

    @Test
    public void testPlatform() {
        Platform platform = underTest.platform();
        assertEquals(GcpConstants.GCP_PLATFORM, platform);
    }

    @Test
    public void testVariant() {
        Variant variant = underTest.variant();
        assertEquals(GcpConstants.GCP_VARIANT, variant);
    }

    @Test
    public void testAccountID() {
        when(gcpStackUtil.getProjectId(any(CloudCredential.class))).thenReturn("accountId");
        String accountId = underTest.getAccountId("region", new CloudCredential());
        assertEquals(accountId, "accountId");
    }
}