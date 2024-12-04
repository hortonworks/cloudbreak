package com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.gateway.topology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.gateway.topology.GatewayTopologyV4Response;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.ExposedServices;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.GatewayTopology;

public class GatewayTopologyToGatewayTopologyV4ResponseConverterTest {

    private static final String TOPOLOGY = "myTopology";

    private GatewayTopologyToGatewayTopologyV4ResponseConverter underTest;

    @Before
    public void setUp() {
        underTest = new GatewayTopologyToGatewayTopologyV4ResponseConverter();
    }

    @Test
    public void testConvertWithNoServicesAndException() throws IOException {
        GatewayTopology source = mock(GatewayTopology.class);
        when(source.getTopologyName()).thenReturn(TOPOLOGY);

        Json json = mock(Json.class);
        when(source.getExposedServices()).thenReturn(json);
        when(json.getValue()).thenReturn("{}");
        when(json.get(ExposedServices.class)).thenThrow(new IOException("Foo"));

        GatewayTopologyV4Response result = underTest.convert(source);

        assertNotNull(result);
        assertEquals(TOPOLOGY, result.getTopologyName());
        assertEquals(Collections.emptyList(), result.getExposedServices());
    }

    @Test
    public void testConvertWithNoServicesAndEmptyList() throws IOException {
        GatewayTopology source = mock(GatewayTopology.class);
        when(source.getTopologyName()).thenReturn(TOPOLOGY);

        ExposedServices exposedServices = mock(ExposedServices.class);
        List<String> services = Collections.emptyList();
        when(exposedServices.getServices()).thenReturn(services);

        Json json = mock(Json.class);
        when(json.get(ExposedServices.class)).thenReturn(exposedServices);
        when(json.getValue()).thenReturn("{}");
        when(source.getExposedServices()).thenReturn(json);

        GatewayTopologyV4Response result = underTest.convert(source);

        assertNotNull(result);
        assertEquals(TOPOLOGY, result.getTopologyName());
        assertEquals(services, result.getExposedServices());
    }

    @Test
    public void testConvertWithSomeServices() throws IOException {
        GatewayTopology source = mock(GatewayTopology.class);
        when(source.getTopologyName()).thenReturn(TOPOLOGY);

        ExposedServices exposedServices = mock(ExposedServices.class);
        List<String> services = List.of("SERVICE1", "SERVICE2");
        when(exposedServices.getServices()).thenReturn(services);

        Json json = mock(Json.class);
        when(source.getExposedServices()).thenReturn(json);
        when(json.getValue()).thenReturn("{}");
        when(json.get(ExposedServices.class)).thenReturn(exposedServices);

        GatewayTopologyV4Response result = underTest.convert(source);

        assertNotNull(result);
        assertEquals(TOPOLOGY, result.getTopologyName());
        assertEquals(services, result.getExposedServices());
    }

}