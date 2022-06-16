package com.sequenceiq.cloudbreak.service.parcel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.PaywallCredentialPopulator;
import com.sequenceiq.cloudbreak.client.RestClientFactory;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.common.exception.UpgradeValidationFailedException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.upgrade.validation.CmUrlProvider;
import com.sequenceiq.cloudbreak.service.upgrade.validation.ParcelUrlProvider;

@ExtendWith(MockitoExtension.class)
public class ParcelAvailabilityServiceTest {

    private static final String PARCEL_1 = "parcel1";

    private static final String PARCEL_2 = "parcel2";

    private static final String ARCHIVE_PARCEL = "https://archive.cloudera.com/parcel";

    private static final String CM_RPM = "cm-rpm";

    private static final long STACK_ID = 1L;

    @InjectMocks
    private ParcelAvailabilityService underTest;

    @Mock
    private ParcelUrlProvider parcelUrlProvider;

    @Mock
    private StackService stackService;

    @Mock
    private RestClientFactory restClientFactory;

    @Mock
    private PaywallCredentialPopulator paywallCredentialPopulator;

    @Mock
    private Client client;

    @Mock
    private CmUrlProvider cmUrlProvider;

    private final Image image = createImage();

    private final Stack stack = new Stack();

    @Test
    public void testValidateParcelAvailabilityShouldReturnTheAvailableParcels() {
        Set<String> requiredParcelsFromImage = createRequiredParcelsSet();
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);
        when(parcelUrlProvider.getRequiredParcelsFromImage(image, stack)).thenReturn(requiredParcelsFromImage);
        when(cmUrlProvider.getCmRpmUrl(image)).thenReturn(CM_RPM);

        when(restClientFactory.getOrCreateDefault()).thenReturn(client);

        Response response1 = createMockResponse(200, PARCEL_1);
        Response response2 = createMockResponse(200, PARCEL_2);
        Response response3 = createMockResponse(200, ARCHIVE_PARCEL);
        Response response4 = createMockResponse(200, CM_RPM);

        Set<Response> actual = underTest.validateAvailability(image, STACK_ID);

        assertTrue(actual.contains(response1));
        assertTrue(actual.contains(response2));
        assertTrue(actual.contains(response3));
        assertFalse(actual.contains(response4));
        assertEquals(3, actual.size());
    }

    @Test
    public void testValidateParcelAvailabilityShouldNotThrowExceptionWhenANonArchiveParcelIsNotAvailable() {
        Set<String> requiredParcelsFromImage = createRequiredParcelsSet();
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);
        when(parcelUrlProvider.getRequiredParcelsFromImage(image, stack)).thenReturn(requiredParcelsFromImage);
        when(cmUrlProvider.getCmRpmUrl(image)).thenReturn(CM_RPM);

        when(restClientFactory.getOrCreateDefault()).thenReturn(client);

        Response response1 = createMockResponse(404, PARCEL_1);
        Response response2 = createMockResponse(200, PARCEL_2);
        Response response3 = createMockResponse(200, ARCHIVE_PARCEL);
        Response response4 = createMockResponse(200, CM_RPM);

        Set<Response> actual = underTest.validateAvailability(image, STACK_ID);

        assertTrue(actual.contains(response1));
        assertTrue(actual.contains(response2));
        assertTrue(actual.contains(response3));
        assertFalse(actual.contains(response4));
        assertEquals(3, actual.size());
    }

    @Test
    public void testValidateParcelAvailabilityShouldThrowExceptionWhenAParcelIsNotAvailable() {
        Set<String> requiredParcelsFromImage = createRequiredParcelsSet();
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);
        when(parcelUrlProvider.getRequiredParcelsFromImage(image, stack)).thenReturn(requiredParcelsFromImage);
        when(cmUrlProvider.getCmRpmUrl(image)).thenReturn(CM_RPM);

        when(restClientFactory.getOrCreateDefault()).thenReturn(client);

        createMockResponse(404, ARCHIVE_PARCEL);
        createMockResponse(200, PARCEL_1);
        createMockResponse(200, PARCEL_2);
        createMockResponse(200, CM_RPM);

        assertThrows(UpgradeValidationFailedException.class, () -> underTest.validateAvailability(image, STACK_ID));
    }

    @Test
    public void testValidateParcelAvailabilityShouldThrowExceptionWhenTheWebTargetThrowsAnException() {
        Set<String> requiredParcelsFromImage = createRequiredParcelsSet();
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);
        when(parcelUrlProvider.getRequiredParcelsFromImage(image, stack)).thenReturn(requiredParcelsFromImage);
        when(cmUrlProvider.getCmRpmUrl(image)).thenReturn(CM_RPM);

        when(restClientFactory.getOrCreateDefault()).thenReturn(client);

        WebTarget webTarget = mock(WebTarget.class);
        when(client.target(ARCHIVE_PARCEL)).thenReturn(webTarget);
        Invocation.Builder request = mock(Invocation.Builder.class);
        when(webTarget.request()).thenReturn(request);
        when(request.head()).thenThrow(new BadRequestException());

        createMockResponse(200, PARCEL_1);
        createMockResponse(200, PARCEL_2);
        createMockResponse(200, CM_RPM);

        assertThrows(UpgradeValidationFailedException.class, () -> underTest.validateAvailability(image, STACK_ID));
    }

    @Test
    public void testValidateParcelAvailabilityShouldNotThrowExceptionWhenTheWebTargetThrowsAnExceptionInCaseOfNonArchiveParcel() {
        Set<String> requiredParcelsFromImage = createRequiredParcelsSet();
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);
        when(parcelUrlProvider.getRequiredParcelsFromImage(image, stack)).thenReturn(requiredParcelsFromImage);
        when(cmUrlProvider.getCmRpmUrl(image)).thenReturn(CM_RPM);

        when(restClientFactory.getOrCreateDefault()).thenReturn(client);

        WebTarget webTarget = mock(WebTarget.class);
        when(client.target(PARCEL_1)).thenReturn(webTarget);
        Invocation.Builder request = mock(Invocation.Builder.class);
        when(webTarget.request()).thenReturn(request);
        when(request.head()).thenThrow(new BadRequestException());

        Response response3 = createMockResponse(200, ARCHIVE_PARCEL);
        Response response2 = createMockResponse(200, PARCEL_2);
        Response response4 = createMockResponse(200, CM_RPM);

        Set<Response> actual = underTest.validateAvailability(image, STACK_ID);

        assertTrue(actual.contains(response2));
        assertTrue(actual.contains(response3));
        assertFalse(actual.contains(response4));
        assertEquals(2, actual.size());
    }

    private Response createMockResponse(int status, String url) {
        WebTarget webTarget = mock(WebTarget.class);
        Invocation.Builder request = mock(Invocation.Builder.class);
        Response response = mock(Response.class);
        when(client.target(url)).thenReturn(webTarget);
        when(webTarget.request()).thenReturn(request);
        when(request.head()).thenReturn(response);
        when(response.getStatus()).thenReturn(status);
        return response;
    }

    private Image createImage() {
        return new Image(null, null, null, null, null, "image-id", null, null, null, null, null, null, null, null, null, true, null, null);
    }

    private Set<String> createRequiredParcelsSet() {
        Set<String> requiredParcelsFromImage = new HashSet<>();
        requiredParcelsFromImage.add(PARCEL_1);
        requiredParcelsFromImage.add(PARCEL_2);
        requiredParcelsFromImage.add(ARCHIVE_PARCEL);
        return requiredParcelsFromImage;
    }

}