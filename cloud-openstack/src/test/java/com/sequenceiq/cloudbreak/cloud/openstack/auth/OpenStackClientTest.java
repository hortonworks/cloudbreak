package com.sequenceiq.cloudbreak.cloud.openstack.auth;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openstack4j.api.OSClient;
import org.openstack4j.api.compute.ComputeService;
import org.openstack4j.api.compute.ext.ZoneService;

import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;

@RunWith(MockitoJUnitRunner.class)
public class OpenStackClientTest {

    private final OpenStackClient underTest = new OpenStackClient();

    @Mock
    private org.openstack4j.model.compute.ext.AvailabilityZone availabilityZone;

    @Test
    public void testGetZones() {
        OSClient<?> osClient = mock(OSClient.class);
        ComputeService computeService = mock(ComputeService.class);
        ZoneService zoneService = mock(ZoneService.class);

        when(availabilityZone.getZoneName()).thenReturn("zone1");
        when(osClient.compute()).thenReturn(computeService);
        when(computeService.zones()).thenReturn(zoneService);
        List availabilityZones = Collections.singletonList(availabilityZone);
        when(zoneService.list()).thenReturn(availabilityZones);

        List<AvailabilityZone> actual = underTest.getZones(osClient, "region");

        Assert.assertEquals(1L, actual.size());
        Assert.assertEquals("zone1", actual.get(0).value());
    }

    @Test
    public void testGetZonesWhenAvailabiltyZonesEmpt() {
        OSClient<?> osClient = mock(OSClient.class);
        ComputeService computeService = mock(ComputeService.class);
        ZoneService zoneService = mock(ZoneService.class);

        when(osClient.compute()).thenReturn(computeService);
        when(computeService.zones()).thenReturn(zoneService);
        when(zoneService.list()).thenReturn(Collections.emptyList());

        List<AvailabilityZone> actual = underTest.getZones(osClient, "region");

        Assert.assertEquals(0L, actual.size());
    }
}
