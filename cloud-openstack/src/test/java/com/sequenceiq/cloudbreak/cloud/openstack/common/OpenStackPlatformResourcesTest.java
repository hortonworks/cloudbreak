package com.sequenceiq.cloudbreak.cloud.openstack.common;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.emptyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openstack4j.api.OSClient;
import org.openstack4j.model.common.Link;
import org.openstack4j.model.compute.Flavor;
import org.openstack4j.model.compute.builder.FlavorBuilder;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmTypes;
import com.sequenceiq.cloudbreak.cloud.openstack.auth.OpenStackClient;

@RunWith(MockitoJUnitRunner.class)
public class OpenStackPlatformResourcesTest {

    @InjectMocks
    private OpenStackPlatformResources underTest = new OpenStackPlatformResources();

    @Mock
    private OpenStackClient openStackClient;

    @Test
    public void testVirtualMachines() {
        CloudCredential cloudCredential = new CloudCredential(0L, "name");
        OSClient osClient = mock(OSClient.class);
        String regionName = "region1";
        Set<String> regionsFromOpenStack = Sets.newHashSet(regionName);
        List<AvailabilityZone> availabilityZones = newArrayList(new AvailabilityZone("zone1"));

        Flavor flavor8GB = flavor(8192);

        when(openStackClient.createOSClient(cloudCredential)).thenReturn(osClient);
        when(openStackClient.getRegion(cloudCredential)).thenReturn(regionsFromOpenStack);
        when(openStackClient.getZones(osClient, regionName)).thenReturn(availabilityZones);
        when(openStackClient.getFlavors(osClient)).thenReturn((List) Collections.singletonList(flavor8GB));

        CloudVmTypes actual = underTest.virtualMachines(cloudCredential, null, null);

        Assert.assertEquals(1, actual.getCloudVmResponses().get("zone1").size());
        Assert.assertEquals(1, actual.getDefaultCloudVmResponses().size());
        Assert.assertEquals("8.0", actual.getCloudVmResponses().get("zone1").iterator().next().getMetaData().getProperties().get("Memory"));
        Assert.assertNotNull(actual.getDefaultCloudVmResponses().get(regionName));

    }

    @Test
    public void testVirtualMachinesWhenTypesEmpty() {
        CloudCredential cloudCredential = new CloudCredential(0L, "name");
        OSClient osClient = mock(OSClient.class);
        String regionName = "region1";
        Set<String> regionsFromOpenStack = Sets.newHashSet(regionName);
        List<AvailabilityZone> availabilityZones = newArrayList(new AvailabilityZone("zone1"));

        when(openStackClient.createOSClient(cloudCredential)).thenReturn(osClient);
        when(openStackClient.getRegion(cloudCredential)).thenReturn(regionsFromOpenStack);
        when(openStackClient.getZones(osClient, regionName)).thenReturn(availabilityZones);
        when(openStackClient.getFlavors(osClient)).thenReturn(emptyList());

        CloudVmTypes actual = underTest.virtualMachines(cloudCredential, null, null);

        verify(openStackClient, times(1)).getFlavors(osClient);

        Assert.assertEquals(0, actual.getCloudVmResponses().get("zone1").size());
        Assert.assertNull(actual.getDefaultCloudVmResponses().get(regionName));

    }

    private Flavor flavor(int ram) {
        return new Flavor() {
            @Override
            public String getId() {
                return null;
            }

            @Override
            public String getName() {
                return null;
            }

            @Override
            public int getRam() {
                return ram;
            }

            @Override
            public int getVcpus() {
                return 0;
            }

            @Override
            public int getDisk() {
                return 0;
            }

            @Override
            public int getSwap() {
                return 0;
            }

            @Override
            public float getRxtxFactor() {
                return 0;
            }

            @Override
            public int getEphemeral() {
                return 0;
            }

            @Override
            public int getRxtxQuota() {
                return 0;
            }

            @Override
            public int getRxtxCap() {
                return 0;
            }

            @Override
            public Boolean isPublic() {
                return false;
            }

            @Override
            public Boolean isDisabled() {
                return false;
            }

            @Override
            public List<? extends Link> getLinks() {
                return null;
            }

            @Override
            public FlavorBuilder toBuilder() {
                return null;
            }
        };
    }

}
