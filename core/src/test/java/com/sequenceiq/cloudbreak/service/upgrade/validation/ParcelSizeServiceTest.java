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
import com.sequenceiq.cloudbreak.service.image.ImageTestUtil;
import com.sequenceiq.cloudbreak.service.image.StatedImage;

@RunWith(MockitoJUnitRunner.class)
public class ParcelSizeServiceTest {

    private static final long CM_PACKAGE_SIZE = 3145728L;

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

    private StatedImage targetImage;

    @InjectMocks
    private ParcelSizeService underTest;

    @Before
    public void before() {
        targetImage = createTargetImage();
        when(restClientFactory.getOrCreateDefault()).thenReturn(client);
        when(client.target(any(String.class))).thenReturn(webTarget);
        when(webTarget.request()).thenReturn(request);
    }

    @Test
    public void testGetAllParcelSizeShouldReturnTheSizeOfTheParcels() throws CloudbreakException {
        Stack stack = new Stack();
        Set<String> parcelUrls = createParcelUrls();

        when(parcelUrlProvider.getRequiredParcelsFromImage(targetImage, stack)).thenReturn(parcelUrls);
        when(request.head()).thenReturn(response);
        when(response.getHeaderString("Content-Length")).thenReturn("1000000000");

        long actual = underTest.getRequiredFreeSpace(targetImage, stack);

        assertEquals(2929687L + CM_PACKAGE_SIZE, actual);
        verify(restClientFactory).getOrCreateDefault();
        verify(parcelUrlProvider).getRequiredParcelsFromImage(targetImage, stack);
        verify(paywallCredentialPopulator, times(3)).populateWebTarget(any(), eq(webTarget));
    }

    @Test(expected = CloudbreakException.class)
    public void testGetAllParcelSizeShouldThrowExceptionWhenTheParcelsAreNotAvailable() throws CloudbreakException {
        Stack stack = new Stack();
        Set<String> parcelUrls = createParcelUrls();

        when(parcelUrlProvider.getRequiredParcelsFromImage(targetImage, stack)).thenReturn(parcelUrls);
        when(request.head()).thenThrow(new ProcessingException("Error"));

        underTest.getRequiredFreeSpace(targetImage, stack);
        verify(restClientFactory).getOrCreateDefault();
        verify(parcelUrlProvider).getRequiredParcelsFromImage(targetImage, stack);
        verify(paywallCredentialPopulator).populateWebTarget(any(), eq(webTarget));
    }

    @Test(expected = CloudbreakException.class)
    public void testGetAllParcelSizeShouldThrowExceptionWhenTheParcelSizeIsZero() throws CloudbreakException {
        Stack stack = new Stack();
        Set<String> parcelUrls = createParcelUrls();

        when(parcelUrlProvider.getRequiredParcelsFromImage(targetImage, stack)).thenReturn(parcelUrls);
        when(request.head()).thenReturn(response);
        when(response.getHeaderString("Content-Length")).thenReturn("0");

        underTest.getRequiredFreeSpace(targetImage, stack);
        verify(restClientFactory).getOrCreateDefault();
        verify(parcelUrlProvider).getRequiredParcelsFromImage(targetImage, stack);
        verify(paywallCredentialPopulator).populateWebTarget(any(), eq(webTarget));
    }

    private Set<String> createParcelUrls() {
        return Set.of("http://parcel1", "http://parcel2", "http://parcel3");
    }

    private StatedImage createTargetImage() {
        return StatedImage.statedImage(ImageTestUtil.getImage(false, "IMAGE_ID", null), "IMAGE_CATALOG_URL", "IMAGE_CATALOG_NAME");
    }

}