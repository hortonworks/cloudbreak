package com.sequenceiq.cloudbreak.cloud.gcp.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.sqladmin.model.Operation;
import com.google.api.services.sqladmin.model.OperationError;
import com.google.api.services.sqladmin.model.OperationErrors;
import com.sequenceiq.cloudbreak.cloud.gcp.GcpResourceException;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
public class GcpDatabaseServerBaseServiceTest {

    private GcpDatabaseServerBaseService underTest = new TestGcpDatabaseServerBaseService();

    @Test
    public void testResourceType() {
        assertTrue(underTest.resourceType().equals(ResourceType.GCP_DATABASE));
    }

    @Test
    public void testGetDatabaseCloudResource() {
        CloudResource gcpDatabase = underTest.getDatabaseCloudResource("test", "az1");

        assertEquals(ResourceType.GCP_DATABASE, gcpDatabase.getType());
        assertEquals("test", gcpDatabase.getName());
    }

    @Test
    public void testCheckException() {
        GoogleJsonResponseException googleJsonResponseException = mock(GoogleJsonResponseException.class);
        GoogleJsonError googleJsonError = mock(GoogleJsonError.class);

        when(googleJsonResponseException.getDetails()).thenReturn(googleJsonError);
        when(googleJsonError.getMessage()).thenReturn("error");

        String result = underTest.checkException(googleJsonResponseException);

        assertEquals("error", result);
    }

    @Test
    public void testVerifyOperation() {
        Operation operation = mock(Operation.class);
        OperationErrors operationErrors = mock(OperationErrors.class);
        OperationError operationError = mock(OperationError.class);
        CloudResource cloudResource = mock(CloudResource.class);

        when(operation.getError()).thenReturn(operationErrors);
        when(cloudResource.getName()).thenReturn("google-resource");
        when(operationErrors.isEmpty()).thenReturn(false);
        when(operationError.getMessage()).thenReturn("error1");
        when(operationErrors.getErrors()).thenReturn(List.of(operationError));


        GcpResourceException gcpResourceException = assertThrows(GcpResourceException.class,
                () -> underTest.verifyOperation(operation, List.of(cloudResource)));
        assertEquals("Failed to execute database operation: error1,: [ resourceType: GCP_DATABASE,  resourceName: google-resource ]",
                gcpResourceException.getMessage());
    }

}