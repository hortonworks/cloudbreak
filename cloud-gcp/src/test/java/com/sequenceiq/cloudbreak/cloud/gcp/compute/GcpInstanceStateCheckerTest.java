package com.sequenceiq.cloudbreak.cloud.gcp.compute;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpResponseException;
import com.google.api.client.http.HttpStatusCodes;
import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.Instance;
import com.google.api.services.compute.model.Operation;
import com.sequenceiq.cloudbreak.cloud.gcp.GcpResourceException;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContext;
import com.sequenceiq.cloudbreak.cloud.gcp.service.checker.AbstractGcpComputeBaseResourceChecker;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;

@ExtendWith(MockitoExtension.class)
class GcpInstanceStateCheckerTest {

    private static final String PROJECT_ID = "aProjectId";

    private static final String AVAILABILITY_ZONE = "anAzId";

    private static final String OPERATION_ID = "anOperationIdentifier";

    @Mock
    private GcpStackUtil gcpStackUtil;

    @Mock
    private GcpContext gcpContext;

    @Mock
    private Compute gcpCompute;

    @InjectMocks
    private GcpInstanceStateChecker underTest;

    @Test
    void checkBasedOnOperationWhenNoInstancesSpecified() {
        List<CloudVmInstanceStatus> actual = underTest.checkBasedOnOperation(gcpContext, List.of());

        verifyNoInteractions(gcpStackUtil);
        assertTrue(actual.isEmpty());
    }

    @Test
    void checkBasedOnOperationWhenGetOperationFailsWithIOException() throws IOException {
        stubGcpContext();
        CloudInstance cloudInstance = getCloudInstanceWithOperation(AVAILABILITY_ZONE, OPERATION_ID);
        when(gcpStackUtil.zoneOperation(gcpCompute, PROJECT_ID, OPERATION_ID, AVAILABILITY_ZONE)).thenThrow(new IOException("Something went wrong!"));

        assertThrows(GcpResourceException.class, () -> underTest.checkBasedOnOperation(gcpContext, List.of(cloudInstance)));

        verify(gcpStackUtil).zoneOperation(gcpCompute, PROJECT_ID, OPERATION_ID, AVAILABILITY_ZONE);
    }

    @Test
    void checkBasedOnOperationWhenGetOperationFailsWithGoogleJsonExceptionWithoutErrorCode() throws IOException {
        stubGcpContext();
        CloudInstance cloudInstance = getCloudInstanceWithOperation(AVAILABILITY_ZONE, OPERATION_ID);
        HttpResponseException.Builder builder = new HttpResponseException.Builder(HttpStatusCodes.STATUS_CODE_BAD_GATEWAY,
                "Bad gateway: internal server error", new HttpHeaders());
        GoogleJsonResponseException googleJsonResponseException = new GoogleJsonResponseException(builder, null);
        when(gcpStackUtil.zoneOperation(gcpCompute, PROJECT_ID, OPERATION_ID, AVAILABILITY_ZONE)).thenThrow(googleJsonResponseException);

        assertThrows(GcpResourceException.class, () -> underTest.checkBasedOnOperation(gcpContext, List.of(cloudInstance)));

        verify(gcpStackUtil).zoneOperation(gcpCompute, PROJECT_ID, OPERATION_ID, AVAILABILITY_ZONE);
    }

    @Test
    void checkBasedOnOperationWhenGetOperationFailsWithGoogleJsonExceptionWithServiceUnavailableAsRelatedErrorCode() throws IOException {
        stubGcpContext();
        CloudInstance cloudInstance = getCloudInstanceWithOperation(AVAILABILITY_ZONE, OPERATION_ID);
        HttpResponseException.Builder builder = new HttpResponseException.Builder(HttpStatusCodes.STATUS_CODE_BAD_GATEWAY,
                "Bad gateway: internal server error", new HttpHeaders());
        GoogleJsonError googleErrorDetail = new GoogleJsonError();
        googleErrorDetail.setCode(HttpStatusCodes.STATUS_CODE_SERVICE_UNAVAILABLE);
        GoogleJsonResponseException googleJsonResponseException = new GoogleJsonResponseException(builder, googleErrorDetail);
        when(gcpStackUtil.zoneOperation(gcpCompute, PROJECT_ID, OPERATION_ID, AVAILABILITY_ZONE)).thenThrow(googleJsonResponseException);

        assertThrows(GcpResourceException.class, () -> underTest.checkBasedOnOperation(gcpContext, List.of(cloudInstance)));

        verify(gcpStackUtil).zoneOperation(gcpCompute, PROJECT_ID, OPERATION_ID, AVAILABILITY_ZONE);
    }

    @Test
    void checkBasedOnOperationWhenGetOperationSucceedsAndFinishedButTheOperationTypeIsNotSupported() throws Exception {
        stubGcpContext();
        CloudInstance cloudInstance = getCloudInstanceWithOperation(AVAILABILITY_ZONE, OPERATION_ID);
        Compute.ZoneOperations.Get get = mock(Compute.ZoneOperations.Get.class);
        Operation operation = getOperation(OPERATION_ID, "DONE", "UNKNOWN");
        when(get.execute()).thenReturn(operation);
        when(gcpStackUtil.zoneOperation(gcpCompute, PROJECT_ID, OPERATION_ID, AVAILABILITY_ZONE)).thenReturn(get);
        when(gcpStackUtil.isOperationFinished(operation)).thenReturn(Boolean.TRUE);

        assertThrows(GcpResourceException.class, () -> underTest.checkBasedOnOperation(gcpContext, List.of(cloudInstance)));

        verify(gcpStackUtil).zoneOperation(gcpCompute, PROJECT_ID, OPERATION_ID, AVAILABILITY_ZONE);
    }

    @ParameterizedTest(name = "checkBasedOnOperationWhenGetOperationReturns with operation status: {0}, type: {1} then the expected instance status is: {3}")
    @MethodSource("checkBasedOnOperationWhenGetOperationReturnsData")
    void checkBasedOnOperationWhenGetOperationReturns(String operationStatus, String operationType, boolean operationFinished,
            InstanceStatus expectedInstanceStatus) throws Exception {
        stubGcpContext();
        CloudInstance cloudInstance = getCloudInstanceWithOperation(AVAILABILITY_ZONE, OPERATION_ID);
        Compute.ZoneOperations.Get get = mock(Compute.ZoneOperations.Get.class);
        Operation operation = getOperation(OPERATION_ID, operationStatus, operationType);
        when(get.execute()).thenReturn(operation);
        when(gcpStackUtil.zoneOperation(gcpCompute, PROJECT_ID, OPERATION_ID, AVAILABILITY_ZONE)).thenReturn(get);
        when(gcpStackUtil.isOperationFinished(operation)).thenReturn(operationFinished);

        List<CloudVmInstanceStatus> vmStatuses = underTest.checkBasedOnOperation(gcpContext, List.of(cloudInstance));

        verify(gcpStackUtil).zoneOperation(gcpCompute, PROJECT_ID, OPERATION_ID, AVAILABILITY_ZONE);
        assertFalse(vmStatuses.isEmpty());
        assertTrue(vmStatuses.stream().allMatch(vmStatus -> expectedInstanceStatus.equals(vmStatus.getStatus())));
    }

    static Object[][] checkBasedOnOperationWhenGetOperationReturnsData() {
        return new Object[][]{
                // operationStatus, operationType, operationFinished, expectedInstanceStatus
                {"RUNNING", "start", Boolean.FALSE, InstanceStatus.IN_PROGRESS},
                {"RUNNING", "startWithEncryptionKey", Boolean.FALSE, InstanceStatus.IN_PROGRESS},
                {"RUNNING", "stop", Boolean.FALSE, InstanceStatus.IN_PROGRESS},
                {"RUNNING", "delete", Boolean.FALSE, InstanceStatus.IN_PROGRESS},
                {"DONE", "start", Boolean.TRUE, InstanceStatus.STARTED},
                {"DONE", "startWithEncryptionKey", Boolean.TRUE, InstanceStatus.STARTED},
                {"DONE", "stop", Boolean.TRUE, InstanceStatus.STOPPED},
                {"DONE", "delete", Boolean.TRUE, InstanceStatus.TERMINATED},
        };
    }

    @ParameterizedTest(name = "checkBasedOnOperation when operation get fails with {0} status code and GCP instance status: {1} "
            + "and expected instance status: {2} ")
    @MethodSource("checkBasedOnOperationWhenGetOperationFailsWithNotFoundGoogleJsonExceptionData")
    void checkBasedOnOperationWhenGetOperationFailsWithNotFoundGoogleJsonExceptionAndInstanceCouldBeGet(int operationErrorStatusCode, String gcpInstanceStatus,
            InstanceStatus expectedInstanceStatus) throws IOException {
        stubGcpContext();
        String instanceId = "instanceId";
        CloudInstance cloudInstance = getCloudInstanceWithOperation(AVAILABILITY_ZONE, OPERATION_ID, instanceId);
        HttpResponseException.Builder builder = new HttpResponseException.Builder(HttpStatusCodes.STATUS_CODE_BAD_GATEWAY,
                "Bad gateway: internal server error", new HttpHeaders());
        GoogleJsonError googleErrorDetail = new GoogleJsonError();
        googleErrorDetail.setCode(operationErrorStatusCode);
        GoogleJsonResponseException googleJsonResponseException = new GoogleJsonResponseException(builder, googleErrorDetail);
        when(gcpStackUtil.zoneOperation(gcpCompute, PROJECT_ID, OPERATION_ID, AVAILABILITY_ZONE)).thenThrow(googleJsonResponseException);
        Instance instance = new Instance();
        instance.setStatus(gcpInstanceStatus);
        when(gcpStackUtil.getComputeInstanceWithId(gcpCompute, PROJECT_ID, AVAILABILITY_ZONE, instanceId)).thenReturn(instance);

        List<CloudVmInstanceStatus> vmStatuses = underTest.checkBasedOnOperation(gcpContext, List.of(cloudInstance));

        verify(gcpStackUtil).zoneOperation(gcpCompute, PROJECT_ID, OPERATION_ID, AVAILABILITY_ZONE);
        verify(gcpStackUtil).getComputeInstanceWithId(gcpCompute, PROJECT_ID, AVAILABILITY_ZONE, instanceId);
        assertFalse(vmStatuses.isEmpty());
        assertTrue(vmStatuses.stream().allMatch(vmStatus -> expectedInstanceStatus.equals(vmStatus.getStatus())));
    }

    static Object[][] checkBasedOnOperationWhenGetOperationFailsWithNotFoundGoogleJsonExceptionData() {
        return new Object[][]{
                // operationErrorStatusCode, gcpInstanceStatus, expectedInstanceStatus
                {HttpStatusCodes.STATUS_CODE_NOT_FOUND, "IN_PROGRESS", InstanceStatus.IN_PROGRESS},
                {HttpStatusCodes.STATUS_CODE_NOT_FOUND, "RUNNING", InstanceStatus.STARTED},
                {HttpStatusCodes.STATUS_CODE_NOT_FOUND, "TERMINATED", InstanceStatus.STOPPED},
                {HttpStatusCodes.STATUS_CODE_FORBIDDEN, "IN_PROGRESS", InstanceStatus.IN_PROGRESS},
                {HttpStatusCodes.STATUS_CODE_FORBIDDEN, "RUNNING", InstanceStatus.STARTED},
                {HttpStatusCodes.STATUS_CODE_FORBIDDEN, "TERMINATED", InstanceStatus.STOPPED},
        };
    }

    @Test
    void checkBasedOnOperationWhenGetOperationFailsWithGoogleJsonExceptionWithForbiddenAsRelatedErrorCodeAndInstanceGetFailsWithIOException()
            throws IOException {
        stubGcpContext();
        String instanceId = "instanceId";
        CloudInstance cloudInstance = getCloudInstanceWithOperation(AVAILABILITY_ZONE, OPERATION_ID, instanceId);
        HttpResponseException.Builder builder = new HttpResponseException.Builder(HttpStatusCodes.STATUS_CODE_BAD_GATEWAY,
                "Bad gateway: internal server error", new HttpHeaders());
        GoogleJsonError googleErrorDetail = new GoogleJsonError();
        googleErrorDetail.setCode(HttpStatusCodes.STATUS_CODE_FORBIDDEN);
        GoogleJsonResponseException googleJsonResponseException = new GoogleJsonResponseException(builder, googleErrorDetail);
        when(gcpStackUtil.zoneOperation(gcpCompute, PROJECT_ID, OPERATION_ID, AVAILABILITY_ZONE)).thenThrow(googleJsonResponseException);
        when(gcpStackUtil.getComputeInstanceWithId(gcpCompute, PROJECT_ID, AVAILABILITY_ZONE, instanceId)).thenThrow(new IOException("something went wrong"));

        assertThrows(GcpResourceException.class, () -> underTest.checkBasedOnOperation(gcpContext, List.of(cloudInstance)));

        verify(gcpStackUtil).zoneOperation(gcpCompute, PROJECT_ID, OPERATION_ID, AVAILABILITY_ZONE);
        verify(gcpStackUtil).getComputeInstanceWithId(gcpCompute, PROJECT_ID, AVAILABILITY_ZONE, instanceId);
    }

    @Test
    void checkBasedOnOperationWhenGetOperationFailsWithGoogleJsonExceptionWithForbiddenAsRelatedErrorCodeAndInstanceGetFailsWithInternalServerErrorGoogleExc()
            throws IOException {
        stubGcpContext();
        String instanceId = "instanceId";
        CloudInstance cloudInstance = getCloudInstanceWithOperation(AVAILABILITY_ZONE, OPERATION_ID, instanceId);
        HttpResponseException.Builder builder = new HttpResponseException.Builder(HttpStatusCodes.STATUS_CODE_FORBIDDEN,
                "Bad gateway: internal server error", new HttpHeaders());
        GoogleJsonError googleErrorDetail = new GoogleJsonError();
        googleErrorDetail.setCode(HttpStatusCodes.STATUS_CODE_FORBIDDEN);
        when(gcpStackUtil.zoneOperation(gcpCompute, PROJECT_ID, OPERATION_ID, AVAILABILITY_ZONE))
                .thenThrow(new GoogleJsonResponseException(builder, googleErrorDetail));

        GoogleJsonError googleErrorDetail2 = new GoogleJsonError();
        googleErrorDetail2.setCode(HttpStatusCodes.STATUS_CODE_SERVER_ERROR);
        when(gcpStackUtil.getComputeInstanceWithId(gcpCompute, PROJECT_ID, AVAILABILITY_ZONE, instanceId))
                .thenThrow(new GoogleJsonResponseException(builder, googleErrorDetail2));

        assertThrows(GcpResourceException.class, () -> underTest.checkBasedOnOperation(gcpContext, List.of(cloudInstance)));

        verify(gcpStackUtil).zoneOperation(gcpCompute, PROJECT_ID, OPERATION_ID, AVAILABILITY_ZONE);
        verify(gcpStackUtil).getComputeInstanceWithId(gcpCompute, PROJECT_ID, AVAILABILITY_ZONE, instanceId);
    }

    @Test
    void checkBasedOnOperationWhenGetOperationFailsWithGoogleJsonExceptionWithForbiddenAsRelatedErrorCodeAndInstanceGetFailsWithNotFoundErrorGoogleExc()
            throws IOException {
        stubGcpContext();
        String instanceId = "instanceId";
        CloudInstance cloudInstance = getCloudInstanceWithOperation(AVAILABILITY_ZONE, OPERATION_ID, instanceId);
        HttpResponseException.Builder builder = new HttpResponseException.Builder(HttpStatusCodes.STATUS_CODE_FORBIDDEN,
                "Bad gateway: internal server error", new HttpHeaders());
        GoogleJsonError googleErrorDetail = new GoogleJsonError();
        googleErrorDetail.setCode(HttpStatusCodes.STATUS_CODE_FORBIDDEN);
        when(gcpStackUtil.zoneOperation(gcpCompute, PROJECT_ID, OPERATION_ID, AVAILABILITY_ZONE))
                .thenThrow(new GoogleJsonResponseException(builder, googleErrorDetail));

        GoogleJsonError googleErrorDetail2 = new GoogleJsonError();
        googleErrorDetail2.setCode(HttpStatusCodes.STATUS_CODE_FORBIDDEN);
        when(gcpStackUtil.getComputeInstanceWithId(gcpCompute, PROJECT_ID, AVAILABILITY_ZONE, instanceId))
                .thenThrow(new GoogleJsonResponseException(builder, googleErrorDetail2));

        List<CloudVmInstanceStatus> vmStatuses = underTest.checkBasedOnOperation(gcpContext, List.of(cloudInstance));

        verify(gcpStackUtil).zoneOperation(gcpCompute, PROJECT_ID, OPERATION_ID, AVAILABILITY_ZONE);
        verify(gcpStackUtil).getComputeInstanceWithId(gcpCompute, PROJECT_ID, AVAILABILITY_ZONE, instanceId);
        assertFalse(vmStatuses.isEmpty());
        assertTrue(vmStatuses.stream().allMatch(vmStatus -> InstanceStatus.TERMINATED_BY_PROVIDER.equals(vmStatus.getStatus())));
    }

    private CloudInstance getCloudInstanceWithOperation(String availabilityZone, String operationId) {
        return getCloudInstanceWithOperation(availabilityZone, operationId, "instanceId");
    }

    private CloudInstance getCloudInstanceWithOperation(String availabilityZone, String operationId, String instanceId) {
        CloudInstance cloudInstance = new CloudInstance(instanceId, null, null, "subnetId", availabilityZone);
        cloudInstance.putParameter(AbstractGcpComputeBaseResourceChecker.OPERATION_ID, operationId);
        return cloudInstance;
    }

    private void stubGcpContext() {
        when(gcpContext.getProjectId()).thenReturn(PROJECT_ID);
        when(gcpContext.getCompute()).thenReturn(gcpCompute);
    }

    private Operation getOperation(String anOperationIdentifier, String status, String operationType) {
        Operation operation = new Operation();
        operation.setName(anOperationIdentifier);
        operation.setStatus(status);
        operation.setOperationType(operationType);
        return operation;
    }
}