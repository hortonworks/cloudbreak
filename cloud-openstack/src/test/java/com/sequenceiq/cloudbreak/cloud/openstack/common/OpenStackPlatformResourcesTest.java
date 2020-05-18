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
import org.openstack4j.model.compute.Flavor;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmTypes;
import com.sequenceiq.cloudbreak.cloud.openstack.auth.OpenStackClient;

@RunWith(MockitoJUnitRunner.class)
public class OpenStackPlatformResourcesTest {

    @InjectMocks
    private final OpenStackPlatformResources underTest = new OpenStackPlatformResources();

    @Mock
    private OpenStackAvailabilityZoneProvider openStackAvailabilityZoneProvider;

    @Mock
    private OpenStackClient openStackClient;

    @Mock
    private Flavor flavor;

    @Test
    public void testVirtualMachines() {
        CloudCredential cloudCredential = new CloudCredential("crn", "name");
        OSClient osClient = mock(OSClient.class);
        String regionName = "region1";
        Set<String> regionsFromOpenStack = Sets.newHashSet(regionName);
        List<AvailabilityZone> availabilityZones = newArrayList(new AvailabilityZone("zone1"));

        when(flavor.getRam()).thenReturn(8192);
        when(openStackClient.createOSClient(cloudCredential)).thenReturn(osClient);
        when(openStackClient.getRegion(cloudCredential)).thenReturn(regionsFromOpenStack);
        when(openStackClient.getFlavors(osClient)).thenReturn((List) Collections.singletonList(flavor));
        when(openStackAvailabilityZoneProvider.getAvailabilityZones(openStackClient, osClient, regionName, cloudCredential))
                .thenReturn(availabilityZones);

        CloudVmTypes actual = underTest.virtualMachines(cloudCredential, null, null);

        Assert.assertEquals(1L, actual.getCloudVmResponses().get("zone1").size());
        Assert.assertEquals(1L, actual.getDefaultCloudVmResponses().size());
        Assert.assertEquals("8.0", actual.getCloudVmResponses().get("zone1").iterator().next().getMetaData().getProperties().get("Memory"));
        Assert.assertNotNull(actual.getDefaultCloudVmResponses().get(regionName));

    }

    @Test
    public void testVirtualMachinesWhenTypesEmpty() {
        CloudCredential cloudCredential = new CloudCredential("crn", "name");
        OSClient osClient = mock(OSClient.class);
        String regionName = "region1";
        Set<String> regionsFromOpenStack = Sets.newHashSet(regionName);
        List<AvailabilityZone> availabilityZones = newArrayList(new AvailabilityZone("zone1"));

        when(openStackClient.createOSClient(cloudCredential)).thenReturn(osClient);
        when(openStackClient.getRegion(cloudCredential)).thenReturn(regionsFromOpenStack);
        when(openStackClient.getFlavors(osClient)).thenReturn(emptyList());
        when(openStackAvailabilityZoneProvider.getAvailabilityZones(openStackClient, osClient, regionName, cloudCredential))
                .thenReturn(availabilityZones);

        CloudVmTypes actual = underTest.virtualMachines(cloudCredential, null, null);

        verify(openStackClient, times(1)).getFlavors(osClient);

        Assert.assertEquals(0L, actual.getCloudVmResponses().get("zone1").size());
        Assert.assertNull(actual.getDefaultCloudVmResponses().get(regionName));
    }
}
