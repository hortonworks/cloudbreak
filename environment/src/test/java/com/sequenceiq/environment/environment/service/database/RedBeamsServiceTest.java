package com.sequenceiq.environment.environment.service.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Set;

import jakarta.ws.rs.WebApplicationException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentDatabaseServerCertificateStatusV4Request;
import com.sequenceiq.environment.exception.RedbeamsOperationFailedException;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.DatabaseServerV4Endpoint;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerCertificateStatusV4Responses;

@ExtendWith(MockitoExtension.class)
class RedBeamsServiceTest {

    private static final String ACTOR = "crn:cdp:iam:us-west-1:cloudera:user:__internal__actor__";

    @Mock
    private DatabaseServerV4Endpoint databaseServerV4Endpoint;

    @Mock
    private WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor;

    @InjectMocks
    private RedBeamsService redBeamsService;

    @Test
    public void testListDatabaseServersCertificateStatusByEnvironmentCrnsSuccess() {
        EnvironmentDatabaseServerCertificateStatusV4Request request = new EnvironmentDatabaseServerCertificateStatusV4Request();
        request.setEnvironmentCrns(Set.of("env1", "env2"));

        DatabaseServerCertificateStatusV4Responses expectedResponse = new DatabaseServerCertificateStatusV4Responses();
        when(databaseServerV4Endpoint.listDatabaseServersCertificateStatus(any(), anyString())).thenReturn(expectedResponse);

        DatabaseServerCertificateStatusV4Responses actualResponse = ThreadBasedUserCrnProvider.doAs(ACTOR, () ->
                redBeamsService.listDatabaseServersCertificateStatusByEnvironmentCrns(request, ACTOR));

        assertEquals(expectedResponse, actualResponse);
    }

    @Test
    public void testListDatabaseServersCertificateStatusByEnvironmentCrnsWebApplicationException() {
        EnvironmentDatabaseServerCertificateStatusV4Request request = new EnvironmentDatabaseServerCertificateStatusV4Request();
        request.setEnvironmentCrns(Set.of("env1", "env2"));

        WebApplicationException webApplicationException = new WebApplicationException("Error");
        when(databaseServerV4Endpoint.listDatabaseServersCertificateStatus(any(), anyString())).thenThrow(webApplicationException);
        when(webApplicationExceptionMessageExtractor.getErrorMessage(webApplicationException)).thenReturn("Extracted error message");

        RedbeamsOperationFailedException redbeamsOperationFailedException = assertThrows(
                RedbeamsOperationFailedException.class,
                () -> ThreadBasedUserCrnProvider.doAs(ACTOR, () ->
                        redBeamsService.listDatabaseServersCertificateStatusByEnvironmentCrns(request, ACTOR)));
            assertEquals("Extracted error message", redbeamsOperationFailedException.getMessage());
    }

}