package com.sequenceiq.cloudbreak.clusterproxy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.List;

import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class ClusterProxyProxyClientTest {

    private static final String CLUSTER_CRN = "crn";

    private static final String SERVICE_NAME = "serviceName";

    private static final String RESPONSE_BODY = "responseBody";

    @Mock
    private RestTemplate restTemplate;

    private ClusterProxyProxyClient underTest;

    @Captor
    private ArgumentCaptor<String> pathCaptor;

    @Captor
    private ArgumentCaptor<Object> requestCaptor;

    @Mock
    private RestClientResponseException restClientResponseException;

    @BeforeEach
    void setUp() {
        underTest = new ClusterProxyProxyClient(restTemplate, CLUSTER_CRN, SERVICE_NAME);
        lenient().when(restClientResponseException.getResponseBodyAsString()).thenReturn(RESPONSE_BODY);
    }

    @Test
    void get() {
        Object response = new Object();
        when(restTemplate.getForObject(pathCaptor.capture(), any())).thenReturn(response);

        Object result = underTest.get("get", Object.class);

        assertThat(result).isEqualTo(response);
        assertThat(pathCaptor.getValue()).isEqualTo("/crn/serviceName/get");
    }

    @Test
    void getWhenRestClientResponseException() {
        when(restTemplate.getForObject(anyString(), any())).thenThrow(restClientResponseException);

        validateRestClientResponseException(() -> underTest.get("get", Object.class));
    }

    @Test
    void getWhenException() {
        when(restTemplate.getForObject(anyString(), any())).thenThrow(RuntimeException.class);

        validateException(() -> underTest.get("get", Object.class));
    }

    @Test
    void post() {
        Object request = new Object();
        List<Object> response = List.of();
        when(restTemplate.postForObject(pathCaptor.capture(), requestCaptor.capture(), any())).thenReturn(response);

        Object result = underTest.post("post", request, List.class);

        assertThat(result).isEqualTo(response);
        assertThat(requestCaptor.getValue()).isEqualTo("{}");
        assertThat(pathCaptor.getValue()).isEqualTo("/crn/serviceName/post");
    }

    @Test
    void postWhenRestClientResponseException() {
        when(restTemplate.postForObject(anyString(), anyString(), any())).thenThrow(restClientResponseException);

        validateRestClientResponseException(() -> underTest.post("post", new Object(), Object.class));
    }

    @Test
    void postWhenException() {
        when(restTemplate.postForObject(anyString(), anyString(), any())).thenThrow(RuntimeException.class);

        validateException(() -> underTest.post("post", new Object(), Object.class));
    }

    private void validateRestClientResponseException(ThrowableAssert.ThrowingCallable callable) {
        assertThatThrownBy(callable)
                .isInstanceOf(ClusterProxyException.class)
                .hasMessage("Error getting response for cluster identifier '%s' with cluster proxy configuration for " +
                        "service '%s', Error Response Body '%s'", CLUSTER_CRN, SERVICE_NAME, RESPONSE_BODY);
    }

    private void validateException(ThrowableAssert.ThrowingCallable callable) {
        assertThatThrownBy(callable)
                .isInstanceOf(ClusterProxyException.class)
                .hasMessage("Error getting response for cluster identifier '%s' with cluster proxy configuration for service '%s'", CLUSTER_CRN, SERVICE_NAME);
    }

}
