package com.sequenceiq.cloudbreak.cloud.azure.image.marketplace;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.sequenceiq.cloudbreak.cloud.azure.rest.AzureRestOperationsService;
import com.sequenceiq.cloudbreak.cloud.azure.rest.AzureRestResponseException;
import com.sequenceiq.cloudbreak.cloud.azure.rest.AzureRestTemplateFactory;

@ExtendWith(MockitoExtension.class)
public class AzureRestOperationServiceTest {

    private static final String TOKEN = "myToken";

    private static final MyRequest REQUEST_BODY = new MyRequest("HELLO");

    private static final MyResponse RESPONSE_BODY = new MyResponse("WORLD");

    @Mock
    private AzureRestTemplateFactory azureRestTemplateFactory;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private AzureRestOperationsService underTest;

    private URI uri = URI.create("http://path.domain.com");

    static List<HttpStatus> httpStatusWith2xx() {
        return Arrays.asList(HttpStatus.values()).stream().filter(HttpStatus::is2xxSuccessful).collect(Collectors.toList());
    }

    static List<HttpStatus> httpStatusWithNot2xx() {
        return Arrays.asList(HttpStatus.values()).stream().filter(httpStatus -> !httpStatus.is2xxSuccessful()).collect(Collectors.toList());
    }

    @BeforeEach
    void setup() {
        when(azureRestTemplateFactory.create()).thenReturn(restTemplate);
    }

    @ParameterizedTest
    @MethodSource(value = "httpStatusWith2xx")
    void testHttpGetReturnsWhenResponse2xx(HttpStatus httpStatus) {
        when(restTemplate.exchange(any(), eq(MyResponse.class))).thenReturn(new ResponseEntity<>(RESPONSE_BODY, httpStatus));

        Object response = underTest.httpGet(uri, MyResponse.class, TOKEN);

        assertThat(response, instanceOf(MyResponse.class));
        ArgumentCaptor<RequestEntity> argumentCaptor = ArgumentCaptor.forClass(RequestEntity.class);
        verify(restTemplate).exchange(argumentCaptor.capture(), any(Class.class));
        assertEquals("Bearer " + TOKEN, argumentCaptor.getValue().getHeaders().get(HttpHeaders.AUTHORIZATION).get(0));
        assertEquals(HttpMethod.GET, argumentCaptor.getValue().getMethod());
    }

    @ParameterizedTest
    @MethodSource(value = "httpStatusWithNot2xx")
    void testHttpGetActionThrowsWhenHttpStatusCodeException(HttpStatus httpStatus) {
        when(restTemplate.exchange(any(), eq(MyResponse.class))).thenThrow(new HttpClientErrorException(httpStatus));

        Exception exception = assertThrows(AzureRestResponseException.class,
                () -> underTest.httpGet(uri, MyResponse.class, TOKEN));

        assertThat(exception.getMessage(), startsWith("Error during http GET operation"));
    }

    @ParameterizedTest
    @MethodSource(value = "httpStatusWith2xx")
    void testHttpPutReturnsWhenResponse2xx(HttpStatus httpStatus) {
        when(restTemplate.exchange(any(), eq(MyResponse.class))).thenReturn(new ResponseEntity<>(RESPONSE_BODY, httpStatus));

        Object response = underTest.httpPut(uri, REQUEST_BODY, MyResponse.class, TOKEN);

        assertThat(response, instanceOf(MyResponse.class));
        ArgumentCaptor<RequestEntity> argumentCaptor = ArgumentCaptor.forClass(RequestEntity.class);
        verify(restTemplate).exchange(argumentCaptor.capture(), any(Class.class));
        assertEquals("Bearer " + TOKEN, argumentCaptor.getValue().getHeaders().get(HttpHeaders.AUTHORIZATION).get(0));
        assertEquals(HttpMethod.PUT, argumentCaptor.getValue().getMethod());
        assertThat(argumentCaptor.getValue().getBody(), instanceOf(MyRequest.class));
        assertEquals("HELLO", ((MyRequest) argumentCaptor.getValue().getBody()).message);
    }

    @ParameterizedTest
    @MethodSource(value = "httpStatusWithNot2xx")
    void testHttpPutActionThrowsWhenHttpStatusCodeException(HttpStatus httpStatus) {
        when(restTemplate.exchange(any(), eq(MyResponse.class))).thenThrow(new HttpClientErrorException(httpStatus));

        Exception exception = assertThrows(AzureRestResponseException.class,
                () -> underTest.httpPut(uri, REQUEST_BODY, MyResponse.class, TOKEN));

        assertThat(exception.getMessage(), startsWith("Error during http PUT operation"));
    }

    private static class MyResponse {
        private String message;

        MyResponse(String message) {
            this.message = message;
        }

        String getMessage() {
            return message;
        }
    }

    private static class MyRequest {
        private String message;

        MyRequest(String message) {
            this.message = message;
        }

        static MyRequest getREQUEST() {
            return REQUEST_BODY;
        }
    }

}