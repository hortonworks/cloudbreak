package com.sequenceiq.remoteenvironment.controller.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;

import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.remoteenvironment.exception.OnPremCMApiException;

@ExtendWith(MockitoExtension.class)
class OnPremCMApiExceptionMapperTest {
    @InjectMocks
    private OnPremCMApiExceptionMapper underTest;

    @ParameterizedTest
    @MethodSource("responseStatuses")
    void testGetResponseStatus(int cmStatusCode, Response.Status responseStatus) {
        Response.Status actualStatus = underTest.getResponseStatus(new OnPremCMApiException("msg", null, cmStatusCode));

        assertEquals(responseStatus, actualStatus);
    }

    private static Stream<Arguments> responseStatuses() {
        return Stream.of(
                Arguments.of(0, Response.Status.SERVICE_UNAVAILABLE),
                Arguments.of(300, Response.Status.INTERNAL_SERVER_ERROR),
                Arguments.of(400, Response.Status.BAD_GATEWAY),
                Arguments.of(401, Response.Status.BAD_GATEWAY),
                Arguments.of(403, Response.Status.BAD_GATEWAY),
                Arguments.of(404, Response.Status.NOT_FOUND),
                Arguments.of(505, Response.Status.BAD_GATEWAY),
                Arguments.of(500, Response.Status.INTERNAL_SERVER_ERROR)
        );
    }
}