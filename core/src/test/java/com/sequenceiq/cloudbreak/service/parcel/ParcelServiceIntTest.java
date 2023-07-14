package com.sequenceiq.cloudbreak.service.parcel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.stubbing.OngoingStubbing;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.sequenceiq.cloudbreak.auth.PaywallCredentialPopulator;
import com.sequenceiq.cloudbreak.client.RestClientFactory;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterComponentUpdater;
import com.sequenceiq.cloudbreak.service.upgrade.sync.component.ImageReaderService;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {ParcelService.class})
@TestPropertySource(properties = {"cb.parcel.retry.maxAttempts=5", "cb.parcel.retry.backOffDelay=5",
        "cb.parcel.retry.backOffMultiplier=2"})
public class ParcelServiceIntTest {

    private static final String ARCHIVE_PARCEL = "https://archive.cloudera.com/parcel";

    private static final String ERROR_MESSAGE = "Failed to fetch the head";

    @Autowired
    private ParcelService underTest;

    @MockBean
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @MockBean
    private ClouderaManagerProductTransformer clouderaManagerProductTransformer;

    @MockBean
    private ParcelFilterService parcelFilterService;

    @MockBean
    private ClusterApiConnectors clusterApiConnectors;

    @MockBean
    private ClusterComponentUpdater clusterComponentUpdater;

    @MockBean
    private ImageReaderService imageReaderService;

    @MockBean
    private RestClientFactory restClientFactory;

    @MockBean
    private PaywallCredentialPopulator paywallCredentialPopulator;

    @MockBean
    private Client client;

    @Test
    void testGetHeadResponseForParcelShouldReturnResponseForNonRetryableStatus() {
        when(restClientFactory.getOrCreateDefault()).thenReturn(client);
        setUpMocks(0, null, List.of(200));
        Optional<Response> actual = underTest.getHeadResponseForParcel(ARCHIVE_PARCEL);
        assertEquals(actual.get().getStatus(), 200);
        verify(restClientFactory, times(1)).getOrCreateDefault();
    }

    @Test
    void testGetHeadResponseForParcelShouldReturnResponseAfterRetryWithRetryableStatus() {
        when(restClientFactory.getOrCreateDefault()).thenReturn(client);
        setUpMocks(0, null, List.of(403, 403, 200));
        Optional<Response> actual = underTest.getHeadResponseForParcel(ARCHIVE_PARCEL);
        assertEquals(actual.get().getStatus(), 200);
        verify(restClientFactory, times(3)).getOrCreateDefault();
    }

    @Test
    void testGetHeadResponseForParcelShouldReturnResponseAfterRetryWithRetryableAndNonTryableStatus() {
        when(restClientFactory.getOrCreateDefault()).thenReturn(client);
        setUpMocks(0, null, List.of(403, 403, 404, 200));
        Optional<Response> actual = underTest.getHeadResponseForParcel(ARCHIVE_PARCEL);
        assertEquals(actual.get().getStatus(), 404);
        verify(restClientFactory, times(3)).getOrCreateDefault();
    }

    @Test
    void testGetHeadResponseForParcelShouldReturnResponseAfterMaxRetryWithRetryableStatus() {
        when(restClientFactory.getOrCreateDefault()).thenReturn(client);
        setUpMocks(0, null, List.of(403, 403, 403, 403, 200));
        Optional<Response> actual = underTest.getHeadResponseForParcel(ARCHIVE_PARCEL);
        assertEquals(actual.get().getStatus(), 200);
        verify(restClientFactory, times(5)).getOrCreateDefault();
    }

    @Test
    void testGetHeadResponseForParcelShouldThrowExceptionAfterRetryExhaustedWithRetryableStatus() {
        when(restClientFactory.getOrCreateDefault()).thenReturn(client);
        setUpMocks(0, null, List.of(403, 403, 403, 403, 403, 200));
        Optional<Response> actual = underTest.getHeadResponseForParcel(ARCHIVE_PARCEL);
        assertEquals(actual.get().getStatus(), 403);
        verify(restClientFactory, times(5)).getOrCreateDefault();
    }

    @Test
    void testGetHeadResponseForParcelShouldReturnResponseAfterRetryWithException() {
        when(restClientFactory.getOrCreateDefault()).thenReturn(client);
        setUpMocks(4, new RuntimeException(ERROR_MESSAGE), List.of(200));
        Optional<Response> actual = underTest.getHeadResponseForParcel(ARCHIVE_PARCEL);
        assertEquals(actual.get().getStatus(), 200);
        verify(restClientFactory, times(5)).getOrCreateDefault();
    }

    @Test
    void testGetHeadResponseForParcelThrowsExceptionAfterRetryExhaustedWithException() {
        when(restClientFactory.getOrCreateDefault()).thenReturn(client);
        setUpMocks(5, new RuntimeException(ERROR_MESSAGE), List.of(200));
        RuntimeException runtimeException = assertThrows(RuntimeException.class, () -> underTest.getHeadResponseForParcel(ARCHIVE_PARCEL));
        assertEquals(runtimeException.getMessage(), ERROR_MESSAGE);
        verify(restClientFactory, times(5)).getOrCreateDefault();
    }

    private void setUpMocks(int exceptionCount, Exception e, List<Integer> statuses) {
        WebTarget webTarget = mock(WebTarget.class);
        Invocation.Builder request = mock(Invocation.Builder.class);
        when(client.target(ARCHIVE_PARCEL)).thenReturn(webTarget);
        when(webTarget.request()).thenReturn(request);
        OngoingStubbing<Response> ongoingStubbing = null;
        while (exceptionCount-- > 0) {
            ongoingStubbing = (ongoingStubbing == null) ? when(request.head()).thenThrow(e) : ongoingStubbing.thenThrow(e);
        }
        for (int status :statuses) {
            Response response = Response.status(status).build();
            ongoingStubbing = (ongoingStubbing == null) ? when(request.head()).thenReturn(response) : ongoingStubbing.thenReturn(response);
        }
    }
}