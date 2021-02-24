package com.sequenceiq.cloudbreak.cloud.gcp.service.checker;

import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.stream.Stream;

import javax.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpResponseException;
import com.sequenceiq.cloudbreak.cloud.gcp.GcpResourceException;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
class AbstractGcpBaseResourceCheckerTest {

    private static final String RESOURCE_NAME = "something";

    @Mock
    private GoogleJsonResponseException mockGoogleJsonResponseException;

    private AbstractGcpBaseResourceChecker underTest;

    @BeforeEach
    void setUp() {
        underTest = new AbstractGcpBaseResourceChecker() {
            @Override
            protected void exceptionHandler(GoogleJsonResponseException ex, String name, ResourceType resourceType) {
                super.exceptionHandler(ex, name, resourceType);
            }
        };
    }

    @ParameterizedTest
    @EnumSource(ResourceType.class)
    void testExceptionHandlerWhenGoogleJsonResponseExceptionIsNullThenIllegalArgumentExceptionShouldCome(ResourceType resourceType) {
        assertThrows(IllegalArgumentException.class, () -> underTest.exceptionHandler(null, RESOURCE_NAME, resourceType));
    }

    @ParameterizedTest
    @EnumSource(ResourceType.class)
    void testExceptionHandlerWhenGoogleJsonErrorIsNullThenGcpResourceExceptionShouldCome(ResourceType resourceType) {
        String expectedExceptionMessage = "somethingAwful";
        when(mockGoogleJsonResponseException.getMessage()).thenReturn(expectedExceptionMessage);
        GcpResourceException expectedException = assertThrows(GcpResourceException.class,
                () -> underTest.exceptionHandler(mockGoogleJsonResponseException, RESOURCE_NAME, resourceType));

        assertNotNull(expectedException.getMessage());
        assertThat(expectedException.getMessage()).contains(expectedExceptionMessage);
    }

    @ParameterizedTest
    @EnumSource(ResourceType.class)
    void testExceptionHandlerWhenGoogleJsonErrorIsNotNullButHasNoEntryForCodeThenGcpResourceExceptionShouldCome(ResourceType resourceType) throws IOException {
        String expectedDetailMessage = "somethingTerrible";
        GoogleJsonError detail = new GoogleJsonError();
        detail.setMessage(expectedDetailMessage);

        HttpResponse mockHttpResponse = createMockHttpResponse(500);

        GoogleJsonResponseException exception = new GoogleJsonResponseException(new HttpResponseException.Builder(mockHttpResponse), detail);

        GcpResourceException expectedException = assertThrows(GcpResourceException.class,
                () -> underTest.exceptionHandler(exception, RESOURCE_NAME, resourceType));

        assertNotNull(expectedException.getMessage());
        assertThat(expectedException.getMessage()).contains(expectedDetailMessage);
    }

    @ParameterizedTest
    @MethodSource("httpStatusAndResourceTypeProviderWithoutNotFoundHttpStatus")
    void testExceptionHandlerWhenGoogleJsonErrorHasEntryForCodeButItIsNotNotFoundThenGcpResourceExceptionShouldCome(int statusCode, ResourceType resourceType)
            throws IOException {
        String expectedDetailMessage = "somethingTerrible";
        GoogleJsonError detail = new GoogleJsonError();
        detail.set("code", statusCode);
        detail.setMessage(expectedDetailMessage);

        HttpResponse mockHttpResponse = createMockHttpResponse(statusCode);

        GoogleJsonResponseException exception = new GoogleJsonResponseException(new HttpResponseException.Builder(mockHttpResponse), detail);

        GcpResourceException expectedException = assertThrows(GcpResourceException.class,
                () -> underTest.exceptionHandler(exception, RESOURCE_NAME, resourceType));

        assertNotNull(expectedException.getMessage());
        assertThat(expectedException.getMessage()).contains(expectedDetailMessage);
    }

    @ParameterizedTest
    @EnumSource(ResourceType.class)
    void testExceptionHandlerWhenGoogleJsonErrorHasEntryForCodeAndItIsNotFoundThenNoExceptionShouldCome(ResourceType resourceType)
            throws IOException {
        String expectedDetailMessage = "somethingTerrible";
        GoogleJsonError detail = new GoogleJsonError();
        detail.set("code", NOT_FOUND.getStatusCode());
        detail.setMessage(expectedDetailMessage);

        HttpResponse mockHttpResponse = createMockHttpResponse(NOT_FOUND.getStatusCode());

        GoogleJsonResponseException exception = new GoogleJsonResponseException(new HttpResponseException.Builder(mockHttpResponse), detail);

        assertDoesNotThrow(() -> underTest.exceptionHandler(exception, RESOURCE_NAME, resourceType));
    }

    static Stream<Arguments> httpStatusAndResourceTypeProviderWithoutNotFoundHttpStatus() {
        Stream.Builder<Arguments> argumentBuilder = Stream.builder();
        for (Response.Status status : Response.Status.values()) {
            if (status.equals(NOT_FOUND)) {
                continue;
            }
            for (ResourceType resourceType : ResourceType.values()) {
                argumentBuilder.add(Arguments.of(status.getStatusCode(), resourceType));
            }
        }
        return argumentBuilder.build();
    }

    private HttpResponse createMockHttpResponse(int statusCode) throws IOException {
        HttpResponse mockHttpResponse = mock(HttpResponse.class);
        when(mockHttpResponse.getStatusCode()).thenReturn(statusCode);
        when(mockHttpResponse.getStatusMessage()).thenReturn("someStatus");
        when(mockHttpResponse.getHeaders()).thenReturn(mock(HttpHeaders.class));
        when(mockHttpResponse.parseAsString()).thenReturn("somethingParsed");
        return mockHttpResponse;
    }

}
