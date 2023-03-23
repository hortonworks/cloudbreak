package com.sequenceiq.consumption.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.WebApplicationException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionHandler;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v2.environment.endpoint.EnvironmentV2Endpoint;

@ExtendWith(MockitoExtension.class)
public class EnvironmentServiceTest {

    private static final String ENV_CRN = "env-crn";

    @Mock
    private EnvironmentV2Endpoint environmentEndpoint;

    @Mock
    private WebApplicationExceptionHandler webApplicationExceptionHandler;

    @Mock
    private DetailedEnvironmentResponse response;

    @InjectMocks
    private EnvironmentService underTest;

    @Test
    public void testGetByCrn() {
        when(environmentEndpoint.getByCrn(ENV_CRN)).thenReturn(response);

        DetailedEnvironmentResponse result = underTest.getByCrn(ENV_CRN);

        verify(environmentEndpoint).getByCrn(ENV_CRN);
        assertEquals(response, result);
    }

    @Test
    public void testGetByCrnNotFound() {
        NotFoundException ex = new NotFoundException("error");
        when(environmentEndpoint.getByCrn(ENV_CRN)).thenThrow(ex);

        NotFoundException e = assertThrows(NotFoundException.class, () -> underTest.getByCrn(ENV_CRN));

        assertEquals(ex, e);
        verify(environmentEndpoint).getByCrn(ENV_CRN);
    }

    @Test
    public void testGetByCrnWebApplicationExceptionHandled() {
        WebApplicationException ex = new WebApplicationException("error");
        when(environmentEndpoint.getByCrn(ENV_CRN)).thenThrow(ex);
        InternalServerErrorException serverErrorEx = new InternalServerErrorException("handled error");
        when(webApplicationExceptionHandler.handleException(ex)).thenThrow(serverErrorEx);

        InternalServerErrorException e = assertThrows(InternalServerErrorException.class, () -> underTest.getByCrn(ENV_CRN));

        assertEquals(serverErrorEx, e);
        verify(environmentEndpoint).getByCrn(ENV_CRN);
        verify(webApplicationExceptionHandler).handleException(ex);
    }
}


