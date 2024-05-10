package com.sequenceiq.cloudbreak.service.parcel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Set;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.common.exception.UpgradeValidationFailedException;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.upgrade.validation.CmUrlProvider;
import com.sequenceiq.cloudbreak.service.upgrade.validation.ParcelUrlProvider;

@ExtendWith(MockitoExtension.class)
public class ParcelAvailabilityServiceTest {

    private static final String PARCEL_1 = "parcel1";

    private static final String PARCEL_2 = "parcel2";

    private static final String ARCHIVE_PARCEL = "https://archive.cloudera.com/parcel";

    private static final String CM_RPM = "https://archive.cloudera.com/cm-rpm";

    private static final long STACK_ID = 1L;

    @InjectMocks
    private ParcelAvailabilityService underTest;

    @Mock
    private ParcelUrlProvider parcelUrlProvider;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private ParcelAvailabilityRetrievalService parcelAvailabilityRetrievalService;

    @Mock
    private CmUrlProvider cmUrlProvider;

    @Mock
    private StackDto stackDto;

    private final Image image = createImage();

    @Test
    public void testValidateParcelAvailabilityShouldReturnTheAvailableParcels() {
        Set<String> requiredParcelsFromImage = createRequiredParcelsSet();
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        when(parcelUrlProvider.getRequiredParcelsFromImage(image, stackDto)).thenReturn(requiredParcelsFromImage);
        when(cmUrlProvider.getCmRpmUrl(image)).thenReturn(CM_RPM);

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
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        when(parcelUrlProvider.getRequiredParcelsFromImage(image, stackDto)).thenReturn(requiredParcelsFromImage);
        when(cmUrlProvider.getCmRpmUrl(image)).thenReturn(CM_RPM);

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
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        when(parcelUrlProvider.getRequiredParcelsFromImage(image, stackDto)).thenReturn(requiredParcelsFromImage);
        when(cmUrlProvider.getCmRpmUrl(image)).thenReturn(CM_RPM);

        createMockResponse(404, ARCHIVE_PARCEL);
        createMockResponse(200, PARCEL_1);
        createMockResponse(200, PARCEL_2);
        createMockResponse(200, CM_RPM);

        assertThrows(UpgradeValidationFailedException.class, () -> underTest.validateAvailability(image, STACK_ID));
    }

    @Test
    public void testValidateParcelAvailabilityShouldThrowExceptionWhenCmRpmIsNotAvailable() {
        Set<String> requiredParcelsFromImage = createRequiredParcelsSet();
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        when(parcelUrlProvider.getRequiredParcelsFromImage(image, stackDto)).thenReturn(requiredParcelsFromImage);
        when(cmUrlProvider.getCmRpmUrl(image)).thenReturn(CM_RPM);

        createMockResponse(200, ARCHIVE_PARCEL);
        createMockResponse(200, PARCEL_1);
        createMockResponse(200, PARCEL_2);
        createMockResponse(404, CM_RPM);

        assertThrows(UpgradeValidationFailedException.class, () -> underTest.validateAvailability(image, STACK_ID));
    }

    @Test
    public void testValidateParcelAvailabilityShouldThrowExceptionWhenTheWebTargetThrowsAnException() {
        Set<String> requiredParcelsFromImage = createRequiredParcelsSet();
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        when(parcelUrlProvider.getRequiredParcelsFromImage(image, stackDto)).thenReturn(requiredParcelsFromImage);
        when(cmUrlProvider.getCmRpmUrl(image)).thenReturn(CM_RPM);

        when(parcelAvailabilityRetrievalService.getHeadResponseForParcel(ARCHIVE_PARCEL)).thenThrow(new BadRequestException());

        createMockResponse(200, PARCEL_1);
        createMockResponse(200, PARCEL_2);
        createMockResponse(200, CM_RPM);

        assertThrows(UpgradeValidationFailedException.class, () -> underTest.validateAvailability(image, STACK_ID));
    }

    @Test
    public void testValidateParcelAvailabilityShouldNotThrowExceptionWhenTheWebTargetThrowsAnExceptionInCaseOfNonArchiveParcel() {
        Set<String> requiredParcelsFromImage = createRequiredParcelsSet();
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        when(parcelUrlProvider.getRequiredParcelsFromImage(image, stackDto)).thenReturn(requiredParcelsFromImage);
        when(cmUrlProvider.getCmRpmUrl(image)).thenReturn(CM_RPM);

        when(parcelAvailabilityRetrievalService.getHeadResponseForParcel(PARCEL_1)).thenThrow(new BadRequestException());

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
        Response response = Response.status(status).build();
        when(parcelAvailabilityRetrievalService.getHeadResponseForParcel(url)).thenReturn(response);
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
