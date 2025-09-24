package com.sequenceiq.cloudbreak.service.parcel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.stubbing.OngoingStubbing;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.sequenceiq.cloudbreak.auth.PaywallCredentialPopulator;
import com.sequenceiq.cloudbreak.client.RestClientFactory;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.stack.CentralCDHVersionCoordinator;
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
    private ParcelAvailabilityRetrievalService underTest;

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
    private CentralCDHVersionCoordinator centralCDHVersionCoordinator;

    @MockBean
    private ImageReaderService imageReaderService;

    @MockBean
    private RestClientFactory restClientFactory;

    @MockBean
    private PaywallCredentialPopulator paywallCredentialPopulator;

    @SpyBean
    private ParcelAvailabilityRetrievalService parcelAvailabilityRetrievalService;

    @MockBean
    private Client client;

    @BeforeEach
    void setUp() {
        when(restClientFactory.getOrCreateWithFollowRedirects()).thenReturn(client);
    }

    @Test
    void testGetHeadResponseForParcelShouldReturnResponseForNonRetryableStatus() {
        setUpMocks(0, null, List.of(200));
        Response actual = underTest.getHeadResponseForParcel(ARCHIVE_PARCEL);
        assertEquals(actual.getStatus(), 200);
        verify(restClientFactory, times(1)).getOrCreateWithFollowRedirects();
    }

    @Test
    void testGetHeadResponseForParcelShouldReturnResponseAfterRetryWithRetryableStatus() {
        setUpMocks(0, null, List.of(403, 403, 200));
        Response actual = underTest.getHeadResponseForParcel(ARCHIVE_PARCEL);
        assertEquals(actual.getStatus(), 200);
        verify(restClientFactory, times(3)).getOrCreateWithFollowRedirects();
    }

    @Test
    void testGetHeadResponseForParcelShouldReturnResponseAfterRetryWithRetryableAndNonTryableStatus() {
        setUpMocks(0, null, List.of(403, 403, 404, 200));
        Response actual = underTest.getHeadResponseForParcel(ARCHIVE_PARCEL);
        assertEquals(actual.getStatus(), 404);
        verify(restClientFactory, times(3)).getOrCreateWithFollowRedirects();
    }

    @Test
    void testGetHeadResponseForParcelShouldReturnResponseAfterMaxRetryWithRetryableStatus() {
        setUpMocks(0, null, List.of(403, 403, 403, 403, 200));
        Response actual = underTest.getHeadResponseForParcel(ARCHIVE_PARCEL);
        assertEquals(actual.getStatus(), 200);
        verify(restClientFactory, times(5)).getOrCreateWithFollowRedirects();
    }

    @Test
    void testGetHeadResponseForParcelShouldThrowExceptionAfterRetryExhaustedWithRetryableStatus() {
        setUpMocks(0, null, List.of(403, 403, 403, 403, 403, 200));
        Response actual = underTest.getHeadResponseForParcel(ARCHIVE_PARCEL);
        assertEquals(actual.getStatus(), 403);
        verify(restClientFactory, times(5)).getOrCreateWithFollowRedirects();
    }

    @Test
    void testGetHeadResponseForParcelShouldReturnResponseAfterRetryWithException() {
        setUpMocks(4, new RuntimeException(ERROR_MESSAGE), List.of(200));
        Response actual = underTest.getHeadResponseForParcel(ARCHIVE_PARCEL);
        assertEquals(actual.getStatus(), 200);
        verify(restClientFactory, times(5)).getOrCreateWithFollowRedirects();
    }

    @Test
    void testGetHeadResponseForParcelThrowsExceptionAfterRetryExhaustedWithException() {
        setUpMocks(5, new RuntimeException(ERROR_MESSAGE), List.of(200));
        RuntimeException runtimeException = assertThrows(RuntimeException.class, () -> underTest.getHeadResponseForParcel(ARCHIVE_PARCEL));
        assertEquals(runtimeException.getMessage(), ERROR_MESSAGE);
        verify(restClientFactory, times(5)).getOrCreateWithFollowRedirects();
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
