package com.sequenceiq.environment.environment.service.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Set;

import jakarta.ws.rs.WebApplicationException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentDatabaseServerCertificateStatusV4Request;
import com.sequenceiq.environment.exception.RedbeamsOperationFailedException;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.DatabaseServerV4Endpoint;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerCertificateStatusV4Responses;

@ExtendWith(MockitoExtension.class)
class RedBeamsServiceTest {

    @Mock
    private DatabaseServerV4Endpoint databaseServerV4Endpoint;

    @Mock
    private WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor;

    @InjectMocks
    private RedBeamsService redBeamsService;

    @Test
    public void testListDatabaseServersCertificateStatusSuccess() {
        EnvironmentDatabaseServerCertificateStatusV4Request request = new EnvironmentDatabaseServerCertificateStatusV4Request();
        request.setEnvironmentCrns(Set.of("env1", "env2"));

        DatabaseServerCertificateStatusV4Responses expectedResponse = new DatabaseServerCertificateStatusV4Responses();
        when(databaseServerV4Endpoint.listDatabaseServersCertificateStatus(any())).thenReturn(expectedResponse);

        DatabaseServerCertificateStatusV4Responses actualResponse = redBeamsService.listDatabaseServersCertificateStatus(request);

        assertEquals(expectedResponse, actualResponse);
    }

    @Test
    public void testListDatabaseServersCertificateStatusWebApplicationException() {
        EnvironmentDatabaseServerCertificateStatusV4Request request = new EnvironmentDatabaseServerCertificateStatusV4Request();
        request.setEnvironmentCrns(Set.of("env1", "env2"));

        WebApplicationException webApplicationException = new WebApplicationException("Error");
        when(databaseServerV4Endpoint.listDatabaseServersCertificateStatus(any())).thenThrow(webApplicationException);
        when(webApplicationExceptionMessageExtractor.getErrorMessage(webApplicationException)).thenReturn("Extracted error message");

        RedbeamsOperationFailedException redbeamsOperationFailedException = assertThrows(
                RedbeamsOperationFailedException.class, () -> redBeamsService.listDatabaseServersCertificateStatus(request));
            assertEquals("Extracted error message", redbeamsOperationFailedException.getMessage());
    }

}