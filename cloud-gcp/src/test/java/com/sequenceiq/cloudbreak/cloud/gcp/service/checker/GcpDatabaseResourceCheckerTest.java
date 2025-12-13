package com.sequenceiq.cloudbreak.cloud.gcp.service.checker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.List;

import org.apache.http.conn.ConnectTimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.api.services.sqladmin.SQLAdmin;
import com.google.api.services.sqladmin.model.Operation;
import com.google.api.services.sqladmin.model.OperationError;
import com.google.api.services.sqladmin.model.OperationErrors;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;

class GcpDatabaseResourceCheckerTest {

    private SQLAdmin.Operations.Get sqlAdminGetOperation;

    private SQLAdmin sqlAdmin;

    private AuthenticatedContext authenticatedContext;

    private GcpDatabaseResourceChecker underTest;

    @BeforeEach
    void setUp() throws IOException {
        sqlAdminGetOperation = mock(SQLAdmin.Operations.Get.class);
        sqlAdmin = mock(SQLAdmin.class);
        underTest = spy(new GcpDatabaseResourceChecker());

        CloudContext cloudContext = new CloudContext.Builder()
                .withName("aDatabaseStackName")
                .build();
        authenticatedContext = new AuthenticatedContext(cloudContext, null);

        String aProjectId = "aProjectId";
        doReturn(aProjectId).when(underTest).getProjectId(authenticatedContext);
        doReturn(sqlAdminGetOperation).when(underTest).getSqlAdminOperations(any(), any(), eq(aProjectId));
    }

    @Test
    void testCheckWhenOperationSdkCallThrowChildOfInterruptedExceptionDueToSocketTimeOutShouldThrowRetryableException() throws IOException {
        when(sqlAdminGetOperation.execute()).thenThrow(new SocketTimeoutException("Read timed out"));

        assertThrows(CloudConnectorException.class, () -> underTest.check(sqlAdmin, authenticatedContext, "anOperationId"));
    }

    @Test
    void testCheckWhenOperationSdkCallThrowChildOfInterruptedExceptionDueToConnectionTimeOutShouldThrowRetryableException() throws IOException {
        when(sqlAdminGetOperation.execute()).thenThrow(new ConnectTimeoutException("Connect timed out"));

        assertThrows(CloudConnectorException.class, () -> underTest.check(sqlAdmin, authenticatedContext, "anOperationId"));
    }

    @Test
    void testCheckWhenOperationSdkCallThrowRuntimeExceptionShouldThrowNonRetryableException() throws IOException {
        when(sqlAdminGetOperation.execute()).thenThrow(new IOException("Something unexpected bad happened."));

        assertThrows(IOException.class, () -> underTest.check(sqlAdmin, authenticatedContext, "anOperationId"));
    }

    @Test
    void testCheckWhenOperationSdkCallReturnWithOperationThatContainsErrorShouldThrowRetryableException() throws IOException {
        Operation operation = new Operation();
        OperationError operationError = new OperationError();
        operationError.setCode("UnExpectedErrorCode");
        operationError.setMessage("An unexpected error happened on our API...");
        operation.setError(new OperationErrors().setErrors(List.of(operationError)));
        when(sqlAdminGetOperation.execute()).thenReturn(operation);

        assertThrows(CloudConnectorException.class, () -> underTest.check(sqlAdmin, authenticatedContext, "anOperationId"));
    }

    @Test
    void testCheckWhenSdkCallSucceedsShouldReturnWithTheOperation() throws IOException {
        Operation operation = new Operation();
        when(sqlAdminGetOperation.execute()).thenReturn(operation);

        Operation result = underTest.check(sqlAdmin, authenticatedContext, "anOperationId");

        assertEquals(operation, result);
    }
}