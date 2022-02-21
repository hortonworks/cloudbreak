package com.sequenceiq.node.health.client;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;

import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Optional;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.thunderhead.telemetry.nodestatus.NodeStatusProto;
import com.sequenceiq.cloudbreak.client.RPCResponse;
import com.sequenceiq.cloudbreak.client.RpcListener;

@ExtendWith(MockitoExtension.class)
public class CdpNodeStatusMonitorClientTest {

    private CdpNodeStatusMonitorClient underTest;

    @Mock
    private Client client;

    @Mock
    private WebTarget rpcTarget;

    @Mock
    private RpcListener listener;

    @Mock
    private Invocation.Builder invocationBuilder;

    @Mock
    private Response response;

    @Mock
    private Response.StatusType statusType;

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        defaultMocks();
        underTest = new CdpNodeStatusMonitorClient(client, new URL("http://myurl"), Map.of(), listener, Optional.empty(), Optional.empty());
    }

    @Test
    public void testSystemMetricsReport() throws CdpNodeStatusMonitorClientException {
        // GIVEN
        String expectedResponse = responseFromFile("/responses/system_metrics_report.json");
        given(response.readEntity(String.class)).willReturn(expectedResponse);
        // WHEN
        RPCResponse<NodeStatusProto.NodeStatusReport> result = underTest.systemMetricsReport();
        // THEN
        assertNotNull(result.getResult().getNodesList().get(0).getSystemMetrics());
    }

    @Test
    public void testServicesReport() throws CdpNodeStatusMonitorClientException {
        // GIVEN
        String expectedResponse = responseFromFile("/responses/services_report.json");
        given(response.readEntity(String.class)).willReturn(expectedResponse);
        // WHEN
        RPCResponse<NodeStatusProto.NodeStatusReport> result = underTest.nodeServicesReport();
        // THEN
        assertNotNull(result.getResult().getNodesList().get(0).getServicesDetails());
    }

    @Test
    public void testNetworkReport() throws CdpNodeStatusMonitorClientException {
        // GIVEN
        String expectedResponse = responseFromFile("/responses/network_report.json");
        given(response.readEntity(String.class)).willReturn(expectedResponse);
        // WHEN
        RPCResponse<NodeStatusProto.NodeStatusReport> result = underTest.nodeNetworkReport();
        // THEN
        assertNotNull(result.getResult().getNodesList().get(0).getNetworkDetails());
    }

    @Test
    public void testMeteringReport() throws CdpNodeStatusMonitorClientException {
        // GIVEN
        String expectedResponse = responseFromFile("/responses/metering_report.json");
        given(response.readEntity(String.class)).willReturn(expectedResponse);
        // WHEN
        RPCResponse<NodeStatusProto.NodeStatusReport> result = underTest.nodeNetworkReport();
        // THEN
        assertNotNull(result.getResult().getNodesList().get(0).getMeteringDetails());
    }

    @Test
    public void testCmMetricsReport() throws CdpNodeStatusMonitorClientException {
        // GIVEN
        String expectedResponse = responseFromFile("/responses/cm_metrics_report.json");
        given(response.readEntity(String.class)).willReturn(expectedResponse);
        // WHEN
        RPCResponse<NodeStatusProto.CmMetricsReport> result = underTest.cmMetricsReport();
        // THEN
        assertNotNull(result.getResult());
    }

    @Test
    public void testSaltReport() throws CdpNodeStatusMonitorClientException {
        // GIVEN
        String expectedResponse = responseFromFile("/responses/salt_report.json");
        given(response.readEntity(String.class)).willReturn(expectedResponse);
        RPCResponse<NodeStatusProto.SaltHealthReport> result = underTest.saltReport();
        // THEN
        assertNotNull(result.getResult());
    }

    private String responseFromFile(String file) {
        String response = null;
        try (InputStream responseStream = CdpNodeStatusMonitorClientTest.class.getResourceAsStream(file)) {
            response = IOUtils.toString(responseStream, Charset.defaultCharset());
        } catch (Exception e) {
            // skip
        }
        return response;
    }

    private void defaultMocks() throws Exception {
        given(client.target(anyString())).willReturn(rpcTarget);
        given(rpcTarget.path(anyString())).willReturn(rpcTarget);
        given(rpcTarget.request()).willReturn(invocationBuilder);
        given(invocationBuilder.headers(any())).willReturn(invocationBuilder);
        given(invocationBuilder.get()).willReturn(response);
        given(response.bufferEntity()).willReturn(true);
        doNothing().when(listener).onBeforeResponseProcessed(response);
        given(response.getStatusInfo()).willReturn(statusType);
        given(statusType.getFamily()).willReturn(Response.Status.Family.SUCCESSFUL);
        given(response.getStatus()).willReturn(Response.Status.OK.getStatusCode());
    }
}
