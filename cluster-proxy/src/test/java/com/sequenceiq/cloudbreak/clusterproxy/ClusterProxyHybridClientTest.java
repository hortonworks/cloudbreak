package com.sequenceiq.cloudbreak.clusterproxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.Charset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import com.cloudera.cdp.servicediscovery.model.DescribeDatalakeAsApiRemoteDataContextResponse;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ClusterProxyHybridClientTest {

    private static final String CONTROL_PLANE = "controlPlane";

    private static final String ENVIRONMENT = "environment";

    private static final String DATALAKE = "datalake";

    private static final String ERROR_RESPONSE = "error";

    private static final String CLUSTER_PROXY_URL = "http://localhost:8080";

    @Mock
    private ClusterProxyConfiguration clusterProxyConfiguration;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ResponseEntity<Object> responseEntity;

    @Mock
    private DescribeDatalakeAsApiRemoteDataContextResponse describeDatalakeAsApiRemoteDataContextResponse;

    @InjectMocks
    private ClusterProxyHybridClient underTest = new ClusterProxyHybridClient(restTemplate);

    @BeforeEach
    public void setup() {
        when(clusterProxyConfiguration.getClusterProxyUrl()).thenReturn(CLUSTER_PROXY_URL);
        when(describeDatalakeAsApiRemoteDataContextResponse.getDatalake()).thenReturn(DATALAKE);
        when(responseEntity.getBody()).thenReturn(describeDatalakeAsApiRemoteDataContextResponse);
        when(restTemplate.postForEntity(anyString(), any(), any())).thenReturn(responseEntity);
    }

    @Test
    public void testGetRemoteDataContext() {
        String clusterProxyUrl = String.format(
                "%s/proxy/%s/PvcControlPlane/api/v1/servicediscovery/describeDatalakeAsApiRemoteDataContext", CLUSTER_PROXY_URL, CONTROL_PLANE);
        DescribeDatalakeAsApiRemoteDataContextResponse describeDatalakeAsApiRemoteDataContextResponse =
                underTest.getRemoteDataContext(CONTROL_PLANE, "user", ENVIRONMENT);
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(restTemplate).postForEntity(captor.capture(), any(), any());
        String clusterConfigUrl = captor.getValue();
        assertEquals(clusterProxyUrl, clusterConfigUrl);
        assertEquals(DATALAKE, describeDatalakeAsApiRemoteDataContextResponse.getDatalake());
    }

    @Test
    public void testGetRemoteDataContextThrowsRestClientResponseException() {
        String expectedErrorMessage = String.format("Error getting Remote Data Context for environment '%s' with cluster proxy configuration " +
                        "for cluster identifier '%s', Error Response Body '%s'", ENVIRONMENT, CONTROL_PLANE, ERROR_RESPONSE);
        when(restTemplate.postForEntity(anyString(), any(), any())).thenThrow(new RestClientResponseException("testMessage", 200, "statusText", null,
                ERROR_RESPONSE.getBytes(), Charset.defaultCharset()));
        ClusterProxyException e = assertThrows(ClusterProxyException.class, () ->
                underTest.getRemoteDataContext(CONTROL_PLANE, "user", ENVIRONMENT));
        assertEquals(expectedErrorMessage, e.getMessage());
    }

    @Test
    public void testGetRemoteDataContextThrowsException() {
        String expectedErrorMessage = String.format("Error reading Remote Data Context for cluster identifier '%s' and " +
                "environment crn '%s'", CONTROL_PLANE, ENVIRONMENT);
        when(restTemplate.postForEntity(anyString(), any(), any())).thenThrow(new RuntimeException("testMessage"));
        ClusterProxyException e = assertThrows(ClusterProxyException.class, () ->
                underTest.getRemoteDataContext(CONTROL_PLANE, "user", ENVIRONMENT));
        assertEquals(expectedErrorMessage, e.getMessage());
    }
}
