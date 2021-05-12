package com.sequenceiq.cloudbreak.cloud.gcp;

import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;

import org.junit.Assert;
import org.junit.Test;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;

public class GcpAuthenticatorTest {

    private GcpAuthenticator underTest = new GcpAuthenticator();

    @Test
    public void testPlatform() {
        Platform platform = underTest.platform();
        Assert.assertEquals(GcpConstants.GCP_PLATFORM, platform);
    }

    @Test
    public void testVariant() {
        Variant variant = underTest.variant();
        Assert.assertEquals(GcpConstants.GCP_VARIANT, variant);
    }

    @Test
    public void testAuthenticate() {
        CloudContext cloudContext = CloudContext.Builder.builder()
                .withAccountId("accountId")
                .withAccountUUID("accountUUID")
                .withCrn("crn")
                .withId(1L)
                .withLocation(location(region("region")))
                .withPlatform("platform")
                .withUserId("userId")
                .withUserName("userName")
                .withWorkspaceId(1L)
                .build();
        CloudCredential cloudCredential = new CloudCredential();
        AuthenticatedContext expected = new AuthenticatedContext(cloudContext, cloudCredential);
        AuthenticatedContext result = underTest.authenticate(cloudContext, cloudCredential);
        Assert.assertEquals(result.getCloudContext(), expected.getCloudContext());
        Assert.assertEquals(result.getCloudCredential(), expected.getCloudCredential());
    }
}