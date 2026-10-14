package com.sequenceiq.cloudbreak.cloud.gcp.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;

import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpResponseException;
import com.google.api.services.sqladmin.SQLAdmin;
import com.google.api.services.sqladmin.model.Operation;
import com.google.api.services.sqladmin.model.OperationsListResponse;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = GcpDatabaseOperationRetryServiceTest.TestAppContext.class)
@TestPropertySource(properties = {
        "cb.gcp.database.conflict.retry.attemptCount=3",
        "cb.gcp.database.conflict.retry.delayMs=1",
        "cb.gcp.database.conflict.retry.maxDelayMs=1"
})
class GcpDatabaseOperationRetryServiceTest {

    private static final String PROJECT_ID = "project-id";

    private static final String INSTANCE_NAME = "server-1";

    @Inject
    private GcpDatabaseOperationRetryService underTest;

    private SQLAdmin sqlAdmin;

    private SQLAdmin.Instances.Patch patch;

    private SQLAdmin.Operations.List operationsList;

    @BeforeEach
    void setUp() throws IOException {
        sqlAdmin = mock(SQLAdmin.class);
        patch = mock(SQLAdmin.Instances.Patch.class);
        SQLAdmin.Operations operations = mock(SQLAdmin.Operations.class);
        operationsList = mock(SQLAdmin.Operations.List.class);
        when(sqlAdmin.operations()).thenReturn(operations);
        when(operations.list(PROJECT_ID)).thenReturn(operationsList);
        when(operationsList.setInstance(anyString())).thenReturn(operationsList);
        when(operationsList.setMaxResults(anyLong())).thenReturn(operationsList);
    }

    @Test
    void testRetriedWhenConflictThenSucceeds() throws IOException {
        Operation operation = new Operation().setName("operation-1");
        when(operationsList.execute()).thenReturn(new OperationsListResponse().setItems(List.of(
                new Operation().setName("backup").setOperationType("BACKUP_VOLUME").setStatus("RUNNING"),
                new Operation().setName("old").setOperationType("UPDATE").setStatus("DONE"))));
        when(patch.execute()).thenThrow(googleJsonResponseException(409)).thenReturn(operation);

        Operation result = underTest.executeRetryingOnConflict(patch, sqlAdmin, PROJECT_ID, INSTANCE_NAME);

        assertSame(operation, result);
        verify(patch, times(2)).execute();
        verify(operationsList).setInstance(INSTANCE_NAME);
        verify(operationsList, times(1)).execute();
    }

    @Test
    void testConflictExceptionThrownWhenConflictPersists() throws IOException {
        GoogleJsonResponseException conflict = googleJsonResponseException(409);
        when(operationsList.execute()).thenReturn(new OperationsListResponse());
        when(patch.execute()).thenThrow(conflict);

        GcpDatabaseOperationConflictException exception = assertThrows(GcpDatabaseOperationConflictException.class,
                () -> underTest.executeRetryingOnConflict(patch, sqlAdmin, PROJECT_ID, INSTANCE_NAME));

        assertSame(conflict, exception.getCause());
        assertEquals(409, exception.getStatusCode());
        assertSame(conflict.getDetails(), exception.getDetails());
        assertEquals(conflict.getMessage(), exception.getMessage());
        verify(patch, times(3)).execute();
        verify(operationsList, times(2)).execute();
    }

    @Test
    void testNotRetriedWhenNotConflict() throws IOException {
        GoogleJsonResponseException badRequest = googleJsonResponseException(400);
        when(patch.execute()).thenThrow(badRequest);

        GoogleJsonResponseException exception = assertThrows(GoogleJsonResponseException.class,
                () -> underTest.executeRetryingOnConflict(patch, sqlAdmin, PROJECT_ID, INSTANCE_NAME));

        assertSame(badRequest, exception);
        assertFalse(exception instanceof GcpDatabaseOperationConflictException);
        assertEquals(400, exception.getStatusCode());
        verify(patch, times(1)).execute();
        verify(sqlAdmin, never()).operations();
    }

    @Test
    void testRetriedWhenListingOperationsFails() throws IOException {
        Operation operation = new Operation().setName("operation-1");
        when(operationsList.execute()).thenThrow(new IOException("list failed"));
        when(patch.execute()).thenThrow(googleJsonResponseException(409)).thenReturn(operation);

        Operation result = underTest.executeRetryingOnConflict(patch, sqlAdmin, PROJECT_ID, INSTANCE_NAME);

        assertSame(operation, result);
        verify(patch, times(2)).execute();
    }

    private GoogleJsonResponseException googleJsonResponseException(int code) {
        GoogleJsonError details = new GoogleJsonError();
        details.setCode(code);
        details.setMessage("error " + code);
        return new GoogleJsonResponseException(new HttpResponseException.Builder(code, "error " + code, new HttpHeaders())
                .setMessage(code + " error " + code), details);
    }

    @Configuration
    @EnableRetry
    @Import(GcpDatabaseOperationRetryService.class)
    static class TestAppContext {
    }
}
