package com.sequenceiq.cloudbreak.service.upgrade.validation;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.auth.PaywallCredentialPopulator;
import com.sequenceiq.cloudbreak.client.RestClientFactory;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.CloudbreakException;

@RunWith(MockitoJUnitRunner.class)
public class ParcelSizeServiceTest {

    private static final String IMAGE_CATALOG_URL = "image-catalog-url";

    private static final String IMAGE_CATALOG_NAME = "image-catalog-name";

    private static final String IMAGE_ID = "image-id";

    @Mock
    private RestClientFactory restClientFactory;

    @Mock
    private PaywallCredentialPopulator paywallCredentialPopulator;

    @Mock
    private ParcelUrlProvider parcelUrlProvider;

    @Mock
    private Client client;

    @Mock
    private WebTarget webTarget;

    @Mock
    private Invocation.Builder request;

    @Mock
    private Response response;

    @InjectMocks
    private ParcelSizeService underTest;

    @Before
    public void before() {
        when(restClientFactory.getOrCreateDefault()).thenReturn(client);
        when(client.target(any(String.class))).thenReturn(webTarget);
        when(webTarget.request()).thenReturn(request);
    }

    @Test
    public void testGetAllParcelSizeShouldReturnTheSizeOfTheParcels() throws CloudbreakException {
        Stack stack = new Stack();
        Set<String> parcelUrls = createParcelUrls();

        when(parcelUrlProvider.getRequiredParcelsFromImage(IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME, IMAGE_ID, stack)).thenReturn(parcelUrls);
        when(request.head()).thenReturn(response);
        when(response.getHeaderString("Content-Length")).thenReturn("1000000000");

        long actual = underTest.getAllParcelSize(IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME, IMAGE_ID, stack);

        assertEquals(2929687L, actual);
        verify(restClientFactory).getOrCreateDefault();
        verify(parcelUrlProvider).getRequiredParcelsFromImage(IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME, IMAGE_ID, stack);
        verify(paywallCredentialPopulator, times(3)).populateWebTarget(any(), eq(webTarget));
    }

    @Test(expected = CloudbreakException.class)
    public void testGetAllParcelSizeShouldThrowExceptionWhenTheParcelsAreNotAvailable() throws CloudbreakException {
        Stack stack = new Stack();
        Set<String> parcelUrls = createParcelUrls();

        when(parcelUrlProvider.getRequiredParcelsFromImage(IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME, IMAGE_ID, stack)).thenReturn(parcelUrls);
        when(request.head()).thenThrow(new ProcessingException("Error"));

        underTest.getAllParcelSize(IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME, IMAGE_ID, stack);
        verify(restClientFactory).getOrCreateDefault();
        verify(parcelUrlProvider).getRequiredParcelsFromImage(IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME, IMAGE_ID, stack);
        verify(paywallCredentialPopulator).populateWebTarget(any(), eq(webTarget));
    }

    @Test(expected = CloudbreakException.class)
    public void testGetAllParcelSizeShouldThrowExceptionWhenTheParcelSizeIsZero() throws CloudbreakException {
        Stack stack = new Stack();
        Set<String> parcelUrls = createParcelUrls();

        when(parcelUrlProvider.getRequiredParcelsFromImage(IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME, IMAGE_ID, stack)).thenReturn(parcelUrls);
        when(request.head()).thenReturn(response);
        when(response.getHeaderString("Content-Length")).thenReturn("0");

        underTest.getAllParcelSize(IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME, IMAGE_ID, stack);
        verify(restClientFactory).getOrCreateDefault();
        verify(parcelUrlProvider).getRequiredParcelsFromImage(IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME, IMAGE_ID, stack);
        verify(paywallCredentialPopulator).populateWebTarget(any(), eq(webTarget));
    }

    private Set<String> createParcelUrls() {
        return Set.of("http://parcel1", "http://parcel2", "http://parcel3");
    }

}