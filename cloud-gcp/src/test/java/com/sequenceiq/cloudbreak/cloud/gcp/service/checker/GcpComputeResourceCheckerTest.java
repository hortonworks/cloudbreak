package com.sequenceiq.cloudbreak.cloud.gcp.service.checker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.List;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpResponseException;
import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.Operation;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContext;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Region;

@ExtendWith(MockitoExtension.class)
class GcpComputeResourceCheckerTest {

    private static final String PROJECT_ID = "test-project";

    private static final String OPERATION_ID = "op-123";

    private static final String AVAILABILITY_ZONE = "us-east1-b";

    @Mock
    private GcpStackUtil gcpStackUtil;

    @InjectMocks
    private GcpComputeResourceChecker underTest;

    private GcpContext context;

    private Compute compute;

    private Location location;

    @BeforeEach
    void setUp() {
        compute = mock(Compute.class);
        location = Location.location(Region.region("us-east1"), AvailabilityZone.availabilityZone(AVAILABILITY_ZONE));
        context = mock(GcpContext.class);
        lenient().when(context.getCompute()).thenReturn(compute);
        lenient().when(context.getProjectId()).thenReturn(PROJECT_ID);
        lenient().when(context.getLocation()).thenReturn(location);
        lenient().when(context.getName()).thenReturn("test-stack");
    }

    @Test
    void checkWhenNullOperationInfoReturnsNull() throws IOException {
        assertNull(underTest.check(context, null, List.of()));
    }

    @Test
    void checkWhenNullOperationIdReturnsNull() throws IOException {
        assertNull(underTest.check(context, new OperationInfo(OperationType.GLOBAL, null), List.of()));
    }

    @Test
    void checkWhenGlobalTypeCallsGlobalOperationDirectly() throws IOException {
        Compute.GlobalOperations.Get get = mock(Compute.GlobalOperations.Get.class);
        Operation expected = new Operation();
        when(gcpStackUtil.globalOperations(compute, PROJECT_ID, OPERATION_ID)).thenReturn(get);
        when(get.execute()).thenReturn(expected);

        Operation result = underTest.check(context, new OperationInfo(OperationType.GLOBAL, OPERATION_ID), List.of());

        assertEquals(expected, result);
        verify(gcpStackUtil).globalOperations(compute, PROJECT_ID, OPERATION_ID);
        verify(gcpStackUtil, never()).regionOperations(any(), any(), any(), any());
        verify(gcpStackUtil, never()).zoneOperation(any(), any(), any(), any());
    }

    @Test
    void checkWhenRegionalTypeCallsRegionOperationDirectly() throws IOException {
        Compute.RegionOperations.Get get = mock(Compute.RegionOperations.Get.class);
        Operation expected = new Operation();
        when(gcpStackUtil.regionOperations(eq(compute), eq(PROJECT_ID), eq(OPERATION_ID), eq(location.getRegion()))).thenReturn(get);
        when(get.execute()).thenReturn(expected);

        Operation result = underTest.check(context, new OperationInfo(OperationType.REGIONAL, OPERATION_ID), List.of());

        assertEquals(expected, result);
        verify(gcpStackUtil).regionOperations(compute, PROJECT_ID, OPERATION_ID, location.getRegion());
        verify(gcpStackUtil, never()).globalOperations(any(), any(), any());
        verify(gcpStackUtil, never()).zoneOperation(any(), any(), any(), any());
    }

    @Test
    void checkWhenZonalTypeCallsZoneOperationWithResourceZone() throws IOException {
        Compute.ZoneOperations.Get get = mock(Compute.ZoneOperations.Get.class);
        Operation expected = new Operation();
        CloudResource resource = mock(CloudResource.class);
        when(resource.getAvailabilityZone()).thenReturn("us-east1-c");
        when(gcpStackUtil.zoneOperation(compute, PROJECT_ID, OPERATION_ID, "us-east1-c")).thenReturn(get);
        when(get.execute()).thenReturn(expected);

        Operation result = underTest.check(context, new OperationInfo(OperationType.ZONAL, OPERATION_ID), List.of(resource));

        assertEquals(expected, result);
        verify(gcpStackUtil).zoneOperation(compute, PROJECT_ID, OPERATION_ID, "us-east1-c");
        verify(gcpStackUtil, never()).globalOperations(any(), any(), any());
        verify(gcpStackUtil, never()).regionOperations(any(), any(), any(), any());
    }

    @Test
    void checkWhenZonalTypeAndResourceHasNoZoneFallsBackToContextZone() throws IOException {
        Compute.ZoneOperations.Get get = mock(Compute.ZoneOperations.Get.class);
        Operation expected = new Operation();
        CloudResource resource = mock(CloudResource.class);
        when(resource.getAvailabilityZone()).thenReturn(null);
        when(gcpStackUtil.zoneOperation(compute, PROJECT_ID, OPERATION_ID, AVAILABILITY_ZONE)).thenReturn(get);
        when(get.execute()).thenReturn(expected);

        Operation result = underTest.check(context, new OperationInfo(OperationType.ZONAL, OPERATION_ID), List.of(resource));

        assertEquals(expected, result);
        verify(gcpStackUtil).zoneOperation(compute, PROJECT_ID, OPERATION_ID, AVAILABILITY_ZONE);
    }

    @Test
    void checkWhenOperationHasErrorThrowsCloudConnectorException() throws IOException {
        Compute.GlobalOperations.Get get = mock(Compute.GlobalOperations.Get.class);
        when(gcpStackUtil.globalOperations(compute, PROJECT_ID, OPERATION_ID)).thenReturn(get);
        when(get.execute()).thenReturn(operationWithError("RESOURCE_ERROR", "something went wrong"));

        assertThrows(CloudConnectorException.class,
                () -> underTest.check(context, new OperationInfo(OperationType.GLOBAL, OPERATION_ID), List.of()));
    }

    @Test
    void checkWhenUnknownTypeUsesFallbackChainGlobalThenRegionalThenZonal() throws IOException {
        CloudResource resource = mock(CloudResource.class);
        when(resource.getAvailabilityZone()).thenReturn(AVAILABILITY_ZONE);

        GoogleJsonResponseException notFound = notFoundException();
        Compute.GlobalOperations.Get globalGet = mock(Compute.GlobalOperations.Get.class);
        Compute.RegionOperations.Get regionGet = mock(Compute.RegionOperations.Get.class);
        Compute.ZoneOperations.Get zoneGet = mock(Compute.ZoneOperations.Get.class);
        Operation expected = new Operation();

        when(gcpStackUtil.globalOperations(compute, PROJECT_ID, OPERATION_ID)).thenReturn(globalGet);
        when(globalGet.execute()).thenThrow(notFound);
        when(gcpStackUtil.regionOperations(eq(compute), eq(PROJECT_ID), eq(OPERATION_ID), any())).thenReturn(regionGet);
        when(regionGet.execute()).thenThrow(notFound);
        when(gcpStackUtil.zoneOperation(eq(compute), eq(PROJECT_ID), eq(OPERATION_ID), eq(AVAILABILITY_ZONE))).thenReturn(zoneGet);
        when(zoneGet.execute()).thenReturn(expected);

        Operation result = underTest.check(context, new OperationInfo(OperationType.UNKNOWN, OPERATION_ID), List.of(resource));

        assertEquals(expected, result);
    }

    @Test
    void checkWhenFallbackChainGlobal403AlsoFallsThrough() throws IOException {
        Compute.GlobalOperations.Get globalGet = mock(Compute.GlobalOperations.Get.class);
        Compute.RegionOperations.Get regionGet = mock(Compute.RegionOperations.Get.class);
        Operation expected = new Operation();

        when(gcpStackUtil.globalOperations(compute, PROJECT_ID, OPERATION_ID)).thenReturn(globalGet);
        when(globalGet.execute()).thenThrow(forbiddenException());
        when(gcpStackUtil.regionOperations(eq(compute), eq(PROJECT_ID), eq(OPERATION_ID), any())).thenReturn(regionGet);
        when(regionGet.execute()).thenReturn(expected);

        Operation result = underTest.check(context, new OperationInfo(OperationType.UNKNOWN, OPERATION_ID), List.of());

        assertEquals(expected, result);
        verify(gcpStackUtil, never()).zoneOperation(any(), any(), any(), any());
    }

    @Test
    void checkWhenFallbackStopsOnNon404Non403Exception() throws IOException {
        GoogleJsonError details = new GoogleJsonError();
        details.set("code", HttpStatus.SC_INTERNAL_SERVER_ERROR);
        GoogleJsonResponseException serverError = new GoogleJsonResponseException(
                new HttpResponseException.Builder(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Internal Server Error", new HttpHeaders()), details);

        Compute.GlobalOperations.Get globalGet = mock(Compute.GlobalOperations.Get.class);
        when(gcpStackUtil.globalOperations(compute, PROJECT_ID, OPERATION_ID)).thenReturn(globalGet);
        when(globalGet.execute()).thenThrow(serverError);

        assertThrows(GoogleJsonResponseException.class,
                () -> underTest.check(context, new OperationInfo(OperationType.UNKNOWN, OPERATION_ID), List.of()));
        verify(gcpStackUtil, never()).regionOperations(any(), any(), any(), any());
    }

    @Test
    void checkWhenFallbackStopsWhenDetailsAreNull() throws IOException {
        GoogleJsonResponseException noDetails = new GoogleJsonResponseException(
                new HttpResponseException.Builder(HttpStatus.SC_NOT_FOUND, "Not Found", new HttpHeaders()), null);

        Compute.GlobalOperations.Get globalGet = mock(Compute.GlobalOperations.Get.class);
        when(gcpStackUtil.globalOperations(compute, PROJECT_ID, OPERATION_ID)).thenReturn(globalGet);
        when(globalGet.execute()).thenThrow(noDetails);

        assertThrows(GoogleJsonResponseException.class,
                () -> underTest.check(context, new OperationInfo(OperationType.UNKNOWN, OPERATION_ID), List.of()));
        verify(gcpStackUtil, never()).regionOperations(any(), any(), any(), any());
    }

    @Test
    void checkWhenInterruptedIOExceptionOnDirectDispatchThrowsCloudConnectorException() throws IOException {
        Compute.GlobalOperations.Get globalGet = mock(Compute.GlobalOperations.Get.class);
        when(gcpStackUtil.globalOperations(compute, PROJECT_ID, OPERATION_ID)).thenReturn(globalGet);
        when(globalGet.execute()).thenThrow(new SocketTimeoutException("Read timed out"));

        assertThrows(CloudConnectorException.class,
                () -> underTest.check(context, new OperationInfo(OperationType.GLOBAL, OPERATION_ID), List.of()));
    }

    @Test
    void checkWhenInterruptedIOExceptionInFallbackThrowsCloudConnectorException() throws IOException {
        Compute.GlobalOperations.Get globalGet = mock(Compute.GlobalOperations.Get.class);
        when(gcpStackUtil.globalOperations(compute, PROJECT_ID, OPERATION_ID)).thenReturn(globalGet);
        when(globalGet.execute()).thenThrow(new SocketTimeoutException("Read timed out"));

        assertThrows(CloudConnectorException.class,
                () -> underTest.check(context, new OperationInfo(OperationType.UNKNOWN, OPERATION_ID), List.of()));
    }

    @Test
    void checkWhenNullOperationTypeUsesDefaultFallbackChain() throws IOException {
        CloudResource resource = mock(CloudResource.class);
        when(resource.getAvailabilityZone()).thenReturn(AVAILABILITY_ZONE);

        GoogleJsonResponseException notFound = notFoundException();
        Compute.GlobalOperations.Get globalGet = mock(Compute.GlobalOperations.Get.class);
        Compute.RegionOperations.Get regionGet = mock(Compute.RegionOperations.Get.class);
        Compute.ZoneOperations.Get zoneGet = mock(Compute.ZoneOperations.Get.class);
        Operation expected = new Operation();

        when(gcpStackUtil.globalOperations(compute, PROJECT_ID, OPERATION_ID)).thenReturn(globalGet);
        when(globalGet.execute()).thenThrow(notFound);
        when(gcpStackUtil.regionOperations(eq(compute), eq(PROJECT_ID), eq(OPERATION_ID), any())).thenReturn(regionGet);
        when(regionGet.execute()).thenThrow(notFound);
        when(gcpStackUtil.zoneOperation(eq(compute), eq(PROJECT_ID), eq(OPERATION_ID), eq(AVAILABILITY_ZONE))).thenReturn(zoneGet);
        when(zoneGet.execute()).thenReturn(expected);

        Operation result = underTest.check(context, new OperationInfo(null, OPERATION_ID), List.of(resource));

        assertEquals(expected, result);
    }

    private GoogleJsonResponseException notFoundException() {
        GoogleJsonError details = new GoogleJsonError();
        details.set("code", HttpStatus.SC_NOT_FOUND);
        return new GoogleJsonResponseException(
                new HttpResponseException.Builder(HttpStatus.SC_NOT_FOUND, "Not Found", new HttpHeaders()), details);
    }

    private GoogleJsonResponseException forbiddenException() {
        GoogleJsonError details = new GoogleJsonError();
        details.set("code", HttpStatus.SC_FORBIDDEN);
        return new GoogleJsonResponseException(
                new HttpResponseException.Builder(HttpStatus.SC_FORBIDDEN, "Forbidden", new HttpHeaders()), details);
    }

    private Operation operationWithError(String code, String message) {
        Operation operation = new Operation();
        Operation.Error error = new Operation.Error();
        Operation.Error.Errors errors = new Operation.Error.Errors();
        errors.setCode(code);
        errors.setMessage(message);
        error.setErrors(List.of(errors));
        operation.setError(error);
        return operation;
    }
}
