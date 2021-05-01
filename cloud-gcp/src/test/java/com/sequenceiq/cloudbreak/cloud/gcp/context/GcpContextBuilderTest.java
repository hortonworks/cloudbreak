package com.sequenceiq.cloudbreak.cloud.gcp.context;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.api.services.compute.Compute;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.gcp.GcpConstants;
import com.sequenceiq.cloudbreak.cloud.gcp.client.GcpComputeFactory;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Region;

@ExtendWith(MockitoExtension.class)
public class GcpContextBuilderTest {

    @InjectMocks
    private GcpContextBuilder underTest;

    @Mock
    private GcpStackUtil gcpStackUtil;

    @Mock
    private GcpComputeFactory gcpComputeFactory;

    @Test
    public void testPlatform() {
        Assert.assertEquals(GcpConstants.GCP_PLATFORM, underTest.platform());
    }

    @Test
    public void testVariant() {
        Assert.assertEquals(GcpConstants.GCP_VARIANT, underTest.variant());
    }

    @Test
    public void testContextInit() {
        Compute compute = mock(Compute.class);
        CloudContext context = mock(CloudContext.class);
        CloudCredential cloudCredential = mock(CloudCredential.class);
        AuthenticatedContext auth = mock(AuthenticatedContext.class);
        when(auth.getCloudCredential()).thenReturn(cloudCredential);
        when(gcpStackUtil.getProjectId(any(CloudCredential.class))).thenReturn("ProjectId");
        when(gcpStackUtil.getServiceAccountId(any(CloudCredential.class))).thenReturn("ServiceAccountId");
        when(gcpComputeFactory.buildCompute(any(CloudCredential.class))).thenReturn(compute);
        when(context.getName()).thenReturn("name");
        when(context.getLocation()).thenReturn(Location.location(Region.region("location")));
        when(gcpStackUtil.noPublicIp(any(Network.class))).thenReturn(true);

        GcpContext gcpContext = underTest.contextInit(
                context,
                auth,
                mock(Network.class),
                new ArrayList<>(),
                true);
        Assert.assertEquals("ProjectId", gcpContext.getProjectId());
        Assert.assertEquals(true, gcpContext.getNoPublicIp());
        Assert.assertEquals("ServiceAccountId", gcpContext.getServiceAccountId());
        Assert.assertEquals("location", gcpContext.getLocation().getRegion().getRegionName());
        Assert.assertEquals("name", gcpContext.getName());
    }

}