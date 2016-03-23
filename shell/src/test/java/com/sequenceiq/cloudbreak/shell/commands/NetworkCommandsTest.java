package com.sequenceiq.cloudbreak.shell.commands;

import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.api.endpoint.NetworkEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.TopologyEndpoint;
import com.sequenceiq.cloudbreak.api.model.IdJson;
import com.sequenceiq.cloudbreak.api.model.NetworkJson;
import com.sequenceiq.cloudbreak.api.model.TopologyResponse;
import com.sequenceiq.cloudbreak.client.CloudbreakClient;
import com.sequenceiq.cloudbreak.shell.completion.NetworkId;
import com.sequenceiq.cloudbreak.shell.completion.NetworkName;
import com.sequenceiq.cloudbreak.shell.model.CloudbreakContext;
import com.sequenceiq.cloudbreak.shell.model.Hints;
import com.sequenceiq.cloudbreak.shell.transformer.ExceptionTransformer;
import com.sequenceiq.cloudbreak.shell.transformer.ResponseTransformer;

public class NetworkCommandsTest {
    private static final Long ID = 50L;
    private static final String NAME = "dummyName";

    @InjectMocks
    private NetworkCommands underTest;

    @Mock
    private NetworkEndpoint networkEndpoint;

    @Mock
    private CloudbreakContext context;

    @Mock
    private CloudbreakClient cloudbreakClient;

    @Mock
    private ResponseTransformer responseTransformer;

    @Mock
    private ExceptionTransformer exceptionTransformer;

    @Mock
    private TopologyEndpoint topologyEndpoint;

    @Mock
    private TopologyResponse topologyResponse;

    @Mock
    private Map<String, String> contextMap;

    private ArgumentCaptor<NetworkJson> networkCaptor;

    @Before
    public void setUp() throws Exception {
        underTest = new NetworkCommands();
        MockitoAnnotations.initMocks(this);
        given(cloudbreakClient.topologyEndpoint()).willReturn(topologyEndpoint);
        given(cloudbreakClient.networkEndpoint()).willReturn(networkEndpoint);
        given(networkEndpoint.postPrivate(any(NetworkJson.class))).willReturn(new IdJson(1L));
        given(networkEndpoint.postPublic(any(NetworkJson.class))).willReturn(new IdJson(1L));
        given(networkEndpoint.get(anyLong())).willReturn(new NetworkJson());
        given(networkEndpoint.getPublic(anyString())).willReturn(new NetworkJson());
        given(exceptionTransformer.transformToRuntimeException(any(Exception.class))).willThrow(RuntimeException.class);
        given(context.getNetworksByProvider()).willReturn(contextMap);
        networkCaptor = ArgumentCaptor.forClass(NetworkJson.class);
    }

    @Test
    public void testCreateAwsNetworkPublic() {
        underTest.createAwsNetwork("name", "subnet", null, null, null, true, null, null);
        verify(networkEndpoint, times(0)).postPrivate(any(NetworkJson.class));
        verify(networkEndpoint, times(1)).postPublic(any(NetworkJson.class));
        verify(context, times(1)).setHint(any(Hints.class));
        verify(context, times(1)).putNetwork(anyString(), anyString());
        verify(context, times(1)).setActiveNetworkId(anyString());
    }

    @Test
    public void testCreateAwsNetworkPrivate() {
        underTest.createAwsNetwork("name", "subnet", null, null, null, false, null, null);
        verify(networkEndpoint, times(1)).postPrivate(any(NetworkJson.class));
        verify(networkEndpoint, times(0)).postPublic(any(NetworkJson.class));
        verify(context, times(1)).setHint(any(Hints.class));
        verify(context, times(1)).putNetwork(anyString(), anyString());
        verify(context, times(1)).setActiveNetworkId(anyString());
    }

    @Test
    public void testCreateAwsNetworkWithPlatform() {
        given(topologyEndpoint.getPublics()).willReturn(Collections.singleton(topologyResponse));
        given(topologyResponse.getId()).willReturn(1L);
        given(topologyResponse.getCloudPlatform()).willReturn("AWS");
        underTest.createAwsNetwork("name", "subnet", null, null, null, null, null, 1L);
        verify(cloudbreakClient, times(1)).topologyEndpoint();
        verify(topologyEndpoint, times(1)).getPublics();
        verify(context, times(1)).setHint(any(Hints.class));
        verify(context, times(1)).putNetwork(anyString(), anyString());
        verify(context, times(1)).setActiveNetworkId(anyString());
    }

    @Test(expected = RuntimeException.class)
    public void testCreateAwsNetworkWithPlatformWhichNotFound() {
        underTest.createAwsNetwork("name", "subnet", null, null, null, null, null, 1L);
    }

    @Test
    public void testCreateAwsNetworkWithoutExistingVpc() {
        underTest.createAwsNetwork("name", "subnet", null, null, null, null, null, null);
        verify(networkEndpoint, times(1)).postPrivate(networkCaptor.capture());
        assertEquals("Only empty parameters allowed", Collections.emptyMap(), networkCaptor.getValue().getParameters());
    }

    @Test
    public void testCreateAwsNetworkWithExistingVpcWithoutSubnet() {
        underTest.createAwsNetwork("name", "subnet", "vpcId", "internetGatewayId", null, null, null, null);
        verify(networkEndpoint, times(1)).postPrivate(networkCaptor.capture());
        Map<String, String> params = new HashMap<>();
        params.put("internetGatewayId", "internetGatewayId");
        params.put("vpcId", "vpcId");
        assertEquals("Only internetGatewayId and vpcId allowed", params, networkCaptor.getValue().getParameters());
    }

    @Test
    public void testCreateAwsNetworkWithExistingVpcWithSubnet() {
        underTest.createAwsNetwork("name", "subnet", "vpcId", "internetGatewayId", "subnetId", null, null, null);
        verify(networkEndpoint, times(1)).postPrivate(networkCaptor.capture());
        Map<String, String> params = new HashMap<>();
        params.put("internetGatewayId", "internetGatewayId");
        params.put("vpcId", "vpcId");
        params.put("subnetId", "subnetId");
        assertEquals("Everything is required", params, networkCaptor.getValue().getParameters());
    }

    @Test
    public void testCreateAzureNetworkPublic() {
        underTest.createAzureNetwork("name", "addressPrefix", "subnet", null, null, null, true, null, null);
        verify(networkEndpoint, times(0)).postPrivate(any(NetworkJson.class));
        verify(networkEndpoint, times(1)).postPublic(any(NetworkJson.class));
        verify(context, times(1)).setHint(any(Hints.class));
        verify(context, times(1)).putNetwork(anyString(), anyString());
        verify(context, times(1)).setActiveNetworkId(anyString());
    }

    @Test
    public void testCreateAzureNetworkPrivate() {
        underTest.createAzureNetwork("name", "addressPrefix", "subnet", null, null, null, false, null, null);
        verify(networkEndpoint, times(1)).postPrivate(any(NetworkJson.class));
        verify(networkEndpoint, times(0)).postPublic(any(NetworkJson.class));
        verify(context, times(1)).setHint(any(Hints.class));
        verify(context, times(1)).putNetwork(anyString(), anyString());
        verify(context, times(1)).setActiveNetworkId(anyString());
    }

    @Test
    public void testCreateAzureNetworkWithPlatform() {
        given(topologyEndpoint.getPublics()).willReturn(Collections.singleton(topologyResponse));
        given(topologyResponse.getId()).willReturn(1L);
        given(topologyResponse.getCloudPlatform()).willReturn("AZURE_RM");
        underTest.createAzureNetwork("name", "addressPrefix", "subnet", null, null, null, null, null, 1L);
        verify(cloudbreakClient, times(1)).topologyEndpoint();
        verify(topologyEndpoint, times(1)).getPublics();
        verify(context, times(1)).setHint(any(Hints.class));
        verify(context, times(1)).putNetwork(anyString(), anyString());
        verify(context, times(1)).setActiveNetworkId(anyString());
    }

    @Test(expected = RuntimeException.class)
    public void testCreateAzureNetworkWithPlatformWhichNotFound() {
        underTest.createAzureNetwork("name", "addressPrefix", "subnet", null, null, null, null, null, 1L);
    }

    @Test
    public void testCreateAzureNetworkWithoutExistingVpc() {
        underTest.createAzureNetwork("name", "addressPrefix", "subnet", null, null, null, null, null, null);
        verify(networkEndpoint, times(1)).postPrivate(networkCaptor.capture());
        assertEquals("Only address prefix allowed", Collections.singletonMap("addressPrefix", "addressPrefix"), networkCaptor.getValue().getParameters());
    }

    @Test
    public void testCreateAzureNetworkWithExistingVpc() {
        underTest.createAzureNetwork("name", "addressPrefix", "subnet", "resourceGroupName", "networkId", "subnetId", null, null, null);
        verify(networkEndpoint, times(1)).postPrivate(networkCaptor.capture());
        Map<String, String> params = new HashMap<>();
        params.put("addressPrefix", "addressPrefix");
        params.put("networkId", "networkId");
        params.put("resourceGroupName", "resourceGroupName");
        params.put("subnetId", "subnetId");
        assertEquals("Everything is required", params, networkCaptor.getValue().getParameters());
    }

    @Test
    public void testCreateGcpNetworkPublic() {
        underTest.createGcpNetwork("name", "subnet", "networkId", "subnetId", true, null, null);
        verify(networkEndpoint, times(0)).postPrivate(any(NetworkJson.class));
        verify(networkEndpoint, times(1)).postPublic(any(NetworkJson.class));
        verify(context, times(1)).setHint(any(Hints.class));
        verify(context, times(1)).putNetwork(anyString(), anyString());
        verify(context, times(1)).setActiveNetworkId(anyString());
    }

    @Test
    public void testCreateGcpNetworkPrivate() {
        underTest.createGcpNetwork("name", "subnet", "networkId", "subnetId", false, null, null);
        verify(networkEndpoint, times(1)).postPrivate(any(NetworkJson.class));
        verify(networkEndpoint, times(0)).postPublic(any(NetworkJson.class));
        verify(context, times(1)).setHint(any(Hints.class));
        verify(context, times(1)).putNetwork(anyString(), anyString());
        verify(context, times(1)).setActiveNetworkId(anyString());
    }

    @Test
    public void testCreateGcpNetworkWithPlatform() {
        given(topologyEndpoint.getPublics()).willReturn(Collections.singleton(topologyResponse));
        given(topologyResponse.getId()).willReturn(1L);
        given(topologyResponse.getCloudPlatform()).willReturn("GCP");
        underTest.createGcpNetwork("name", "subnet", null, null, null, null, 1L);
        verify(cloudbreakClient, times(1)).topologyEndpoint();
        verify(topologyEndpoint, times(1)).getPublics();
        verify(context, times(1)).setHint(any(Hints.class));
        verify(context, times(1)).putNetwork(anyString(), anyString());
        verify(context, times(1)).setActiveNetworkId(anyString());
    }

    @Test(expected = RuntimeException.class)
    public void testCreateGcpNetworkWithPlatformWhichNotFound() {
        underTest.createGcpNetwork("name", "subnet", null, null, null, null, 1L);
    }

    @Test
    public void testCreateGcpNetworkWithoutExistingVpc() {
        underTest.createGcpNetwork("name", "subnet", null, null, null, null, null);
        verify(networkEndpoint, times(1)).postPrivate(networkCaptor.capture());
        assertEquals("Only empty parameters allowed", Collections.emptyMap(), networkCaptor.getValue().getParameters());
    }

    @Test
    public void testCreateGcpNetworkWithExistingVpc() {
        underTest.createGcpNetwork("name", "subnet", "netId", null, null, null, null);
        verify(networkEndpoint, times(1)).postPrivate(networkCaptor.capture());
        assertEquals("Only network id allowed", Collections.singletonMap("networkId", "netId"), networkCaptor.getValue().getParameters());
    }

    @Test
    public void testCreateopenStackNetworkPublic() {
        underTest.createOpenStackNetwork("name", "subnet", "pubNetId", null, null, null, true, null, null);
        verify(networkEndpoint, times(0)).postPrivate(any(NetworkJson.class));
        verify(networkEndpoint, times(1)).postPublic(any(NetworkJson.class));
        verify(context, times(1)).setHint(any(Hints.class));
        verify(context, times(1)).putNetwork(anyString(), anyString());
        verify(context, times(1)).setActiveNetworkId(anyString());
    }

    @Test
    public void testCreateopenStackNetworkPrivate() {
        underTest.createOpenStackNetwork("name", "subnet", "pubNetId", null, null, null, false, null, null);
        verify(networkEndpoint, times(1)).postPrivate(any(NetworkJson.class));
        verify(networkEndpoint, times(0)).postPublic(any(NetworkJson.class));
        verify(context, times(1)).setHint(any(Hints.class));
        verify(context, times(1)).putNetwork(anyString(), anyString());
        verify(context, times(1)).setActiveNetworkId(anyString());
    }

    @Test
    public void testCreateopenStackNetworkWithPlatform() {
        given(topologyEndpoint.getPublics()).willReturn(Collections.singleton(topologyResponse));
        given(topologyResponse.getId()).willReturn(1L);
        given(topologyResponse.getCloudPlatform()).willReturn("OPENSTACK");
        underTest.createOpenStackNetwork("name", "subnet", "pubNetId", null, null, null, null, null, 1L);
        verify(cloudbreakClient, times(1)).topologyEndpoint();
        verify(topologyEndpoint, times(1)).getPublics();
        verify(context, times(1)).setHint(any(Hints.class));
        verify(context, times(1)).putNetwork(anyString(), anyString());
        verify(context, times(1)).setActiveNetworkId(anyString());
    }

    @Test(expected = RuntimeException.class)
    public void testCreateopenStackNetworkWithPlatformWhichNotFound() {
        underTest.createOpenStackNetwork("name", "subnet", "pubNetId", null, null, null, null, null, 1L);
    }

    @Test
    public void testCreateopenStackNetworkWithoutExistingVpc() {
        underTest.createOpenStackNetwork("name", "subnet", "pubNetId", null, null, null, null, null, null);
        verify(networkEndpoint, times(1)).postPrivate(networkCaptor.capture());
        assertEquals("Only public network id allowed", Collections.singletonMap("publicNetId", "pubNetId"), networkCaptor.getValue().getParameters());
    }

    @Test
    public void testCreateopenStackNetworkWithExistingVpcWithSubnet() {
        underTest.createOpenStackNetwork("name", "subnet", "pubNetId", "netId", "routerId", "subnetId", null, null, null);
        verify(networkEndpoint, times(1)).postPrivate(networkCaptor.capture());
        Map<String, String> params = new HashMap<>();
        params.put("publicNetId", "pubNetId");
        params.put("networkId", "netId");
        params.put("routerId", "routerId");
        params.put("subnetId", "subnetId");
        assertEquals("Everything is required", params, networkCaptor.getValue().getParameters());
    }

    @Test
    public void testCreateopenStackNetworkWithExistingVpcWithoutSubnet() {
        underTest.createOpenStackNetwork("name", "subnet", "pubNetId", "netId", "routerId", null, null, null, null);
        verify(networkEndpoint, times(1)).postPrivate(networkCaptor.capture());
        Map<String, String> params = new HashMap<>();
        params.put("publicNetId", "pubNetId");
        params.put("networkId", "netId");
        params.put("routerId", "routerId");
        assertEquals("Subnet id not allowed", params, networkCaptor.getValue().getParameters());
    }

    @Test
    public void testSelectById() throws Exception {
        given(contextMap.containsKey(anyLong())).willReturn(true);
        given(contextMap.get(anyString())).willReturn("test");
        underTest.selectNetwork(new NetworkId(ID.toString()), null);
        verify(context, times(1)).setHint(any(Hints.class));
        verify(context, times(1)).putNetwork(anyString(), anyString());
        verify(context, times(1)).setActiveNetworkId(anyString());
        verify(contextMap, times(1)).containsKey(anyString());
        verify(contextMap, times(1)).get(anyString());
        verify(networkEndpoint, times(0)).getPublic(anyString());
    }

    @Test
    public void testSelectByIdButNotFound() throws Exception {
        given(contextMap.containsKey(anyLong())).willReturn(false);
        underTest.selectNetwork(new NetworkId(ID.toString()), null);
        verify(context, times(0)).setHint(any(Hints.class));
        verify(context, times(0)).putNetwork(anyString(), anyString());
        verify(context, times(0)).setActiveNetworkId(anyString());
        verify(contextMap, times(1)).containsKey(anyString());
        verify(contextMap, times(0)).get(anyString());
        verify(networkEndpoint, times(0)).getPublic(anyString());
    }

    @Test
    public void testSelectByName() throws Exception {
        underTest.selectNetwork(null, new NetworkName(NAME));
        verify(context, times(1)).setHint(any(Hints.class));
        verify(context, times(1)).putNetwork(anyString(), anyString());
        verify(context, times(1)).setActiveNetworkId(anyString());
        verify(context, times(0)).getNetworksByProvider();
        verify(networkEndpoint, times(1)).getPublic(anyString());
    }

    @Test
    public void testSelectByNameButNotFound() throws Exception {
        given(networkEndpoint.getPublic(anyString())).willReturn(null);
        underTest.selectNetwork(null, new NetworkName(NAME));
        verify(context, times(0)).setHint(any(Hints.class));
        verify(context, times(0)).putNetwork(anyString(), anyString());
        verify(context, times(0)).setActiveNetworkId(anyString());
        verify(context, times(0)).getNetworksByProvider();
        verify(networkEndpoint, times(1)).getPublic(anyString());
    }

    @Test
    public void testSelectWithoutIdAndName() throws Exception {
        underTest.selectNetwork(null, null);
        verify(context, times(0)).setHint(any(Hints.class));
        verify(context, times(0)).putNetwork(anyString(), anyString());
        verify(context, times(0)).setActiveNetworkId(anyString());
        verify(context, times(0)).getNetworksByProvider();
        verify(networkEndpoint, times(0)).getPublic(anyString());
    }

    @Test
    public void testDeleteById() throws Exception {
        underTest.deleteNetwork(new NetworkId(ID.toString()), null);
        verify(networkEndpoint, times(1)).delete(anyLong());
        verify(networkEndpoint, times(0)).deletePublic(anyString());
    }

    @Test
    public void testDeleteByName() throws Exception {
        underTest.deleteNetwork(null, new NetworkName(NAME));
        verify(networkEndpoint, times(0)).delete(anyLong());
        verify(networkEndpoint, times(1)).deletePublic(anyString());
    }

    @Test
    public void testDeleteWithoutIdAndName() throws Exception {
        underTest.deleteNetwork(null, null);
        verify(networkEndpoint, times(0)).delete(anyLong());
        verify(networkEndpoint, times(0)).deletePublic(anyString());
    }

    @Test
    public void testShowById() throws Exception {
        underTest.showNetwork(new NetworkId(ID.toString()), null);
        verify(networkEndpoint, times(1)).get(anyLong());
        verify(networkEndpoint, times(0)).getPublic(anyString());
    }

    @Test
    public void testShowByName() throws Exception {
        underTest.showNetwork(null, new NetworkName(NAME));
        verify(networkEndpoint, times(0)).get(anyLong());
        verify(networkEndpoint, times(1)).getPublic(anyString());
    }

    @Test
    public void testShowWithoutIdAndName() throws Exception {
        underTest.showNetwork(null, null);
        verify(networkEndpoint, times(0)).get(anyLong());
        verify(networkEndpoint, times(0)).getPublic(anyString());
    }
}
