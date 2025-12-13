package com.sequenceiq.cloudbreak.cloud.gcp;

import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;

class GcpAuthenticatorTest {

    private GcpAuthenticator underTest = new GcpAuthenticator();

    @Test
    void testPlatform() {
        Platform platform = underTest.platform();
        assertEquals(GcpConstants.GCP_PLATFORM, platform);
    }

    @Test
    void testVariant() {
        Variant variant = underTest.variant();
        assertEquals(GcpConstants.GCP_VARIANT, variant);
    }

    @Test
    void testAuthenticate() {
        CloudContext cloudContext = CloudContext.Builder.builder()
                .withAccountId("accountId")
                .withCrn("crn")
                .withId(1L)
                .withLocation(location(region("region")))
                .withPlatform("platform")
                .withUserName("userName")
                .withWorkspaceId(1L)
                .build();
        CloudCredential cloudCredential = new CloudCredential();
        AuthenticatedContext expected = new AuthenticatedContext(cloudContext, cloudCredential);
        AuthenticatedContext result = underTest.authenticate(cloudContext, cloudCredential);
        assertEquals(result.getCloudContext(), expected.getCloudContext());
        assertEquals(result.getCloudCredential(), expected.getCloudCredential());
    }
}