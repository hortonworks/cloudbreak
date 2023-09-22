package com.sequenceiq.cloudbreak.converter.spi;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.workspace.model.Tenant;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;

@ExtendWith(MockitoExtension.class)
class CloudContextProviderTest {

    @InjectMocks
    private CloudContextProvider underTest;

    @Test
    void testGetCloudContext() {
        Stack stack = new Stack();
        stack.setRegion("region");
        stack.setAvailabilityZone("availabilityZone");
        stack.setId(1L);
        stack.setName("stackName");
        stack.setOriginalName("originalStackName");
        stack.setResourceCrn("crn:cdp:datahub:us-west-1:e7b1345f-4ae1-4594-9113-fc91f22ef8bd:cluster:aa8997d3-527d-4e7f-af8a-7f7cd10eb8f7");
        stack.setCloudPlatform("cloudPlatform");
        stack.setPlatformVariant("platformVariant");
        Workspace workspace = new Workspace();
        workspace.setId(0L);
        Tenant tenant = new Tenant();
        tenant.setId(2L);
        workspace.setTenant(tenant);
        stack.setWorkspace(workspace);

        CloudContext cloudContext = underTest.getCloudContext(stack);

        assertEquals(stack.getId(), cloudContext.getId());
        assertEquals(stack.getName(), cloudContext.getName());
        assertEquals(stack.getOriginalName(), cloudContext.getOriginalName());
        assertEquals(stack.getResourceCrn(), cloudContext.getCrn());
        assertEquals(Platform.platform(stack.getCloudPlatform()), cloudContext.getPlatform());
        assertEquals(Variant.variant(stack.getPlatformVariant()), cloudContext.getVariant());
        assertEquals(location(region(stack.getRegion()), availabilityZone(stack.getAvailabilityZone())), cloudContext.getLocation());
        assertEquals("e7b1345f-4ae1-4594-9113-fc91f22ef8bd", cloudContext.getAccountId());
        assertEquals(2L, cloudContext.getTenantId());
    }

}