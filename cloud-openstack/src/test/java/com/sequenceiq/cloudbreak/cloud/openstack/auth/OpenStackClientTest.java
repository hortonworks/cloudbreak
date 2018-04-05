package com.sequenceiq.cloudbreak.cloud.openstack.auth;

import static com.google.common.collect.Lists.newArrayList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.openstack4j.api.OSClient;
import org.openstack4j.api.compute.ComputeService;
import org.openstack4j.api.compute.ext.ZoneService;

import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;

@RunWith(MockitoJUnitRunner.class)
public class OpenStackClientTest {

    private OpenStackClient underTest = new OpenStackClient();

    @Test
    public void testGetZones() {
        OSClient<?> osClient = mock(OSClient.class);
        ComputeService computeService = mock(ComputeService.class);
        ZoneService zoneService = mock(ZoneService.class);

        List availabilityZones = newArrayList(availabilityZone("zone1"));

        when(osClient.compute()).thenReturn(computeService);
        when(computeService.zones()).thenReturn(zoneService);
        when(zoneService.list()).thenReturn(availabilityZones);

        List<AvailabilityZone> actual = underTest.getZones(osClient, "region");

        Assert.assertEquals(1, actual.size());
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

        Assert.assertEquals(0, actual.size());
    }

    private org.openstack4j.model.compute.ext.AvailabilityZone availabilityZone(String name) {
        return new org.openstack4j.model.compute.ext.AvailabilityZone() {
            @Override
            public ZoneState getZoneState() {
                return null;
            }

            @Override
            public Map<String, Map<String, ? extends NovaService>> getHosts() {
                return null;
            }

            @Override
            public String getZoneName() {
                return name;
            }
        };
    }

}
