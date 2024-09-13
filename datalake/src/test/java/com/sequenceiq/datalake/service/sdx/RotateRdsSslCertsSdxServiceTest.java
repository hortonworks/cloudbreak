package com.sequenceiq.datalake.service.sdx;

import static com.sequenceiq.datalake.service.sdx.SdxService.WORKSPACE_ID_DEFAULT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.ExceptionResponse;

@ExtendWith(MockitoExtension.class)
@DisplayName("Rotate RDS SSL certificates related SDX service tests")
public class RotateRdsSslCertsSdxServiceTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:hortonworks:user:perdos@hortonworks.com";

    @Mock
    private StackV4Endpoint stackV4Endpoint;

    @Mock
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Mock
    private RegionAwareInternalCrnGenerator regionAwareInternalCrnGenerator;

    @InjectMocks
    private SdxService underTest;

    @Test
    void testValidateRdsSslCertRotation() {
        String dlCrn = "dlCrn";
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn:cdp:datalake:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.validateRdsSslCertRotation(dlCrn));

        verify(stackV4Endpoint).validateRotateRdsCertificateByCrnInternal(WORKSPACE_ID_DEFAULT, dlCrn, USER_CRN);
    }

    @Test
    void testValidateRdsSslCertRotationWhenValidationFailsWithBadRequest() {
        String dlCrn = "dlCrn";
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn:cdp:datalake:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        jakarta.ws.rs.BadRequestException badRequestException = new jakarta.ws.rs.BadRequestException("Uh-Oh validation failed");
        doThrow(badRequestException)
                .when(stackV4Endpoint).validateRotateRdsCertificateByCrnInternal(WORKSPACE_ID_DEFAULT, dlCrn, USER_CRN);

        BadRequestException actualException = assertThrows(BadRequestException.class,
                () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.validateRdsSslCertRotation(dlCrn)));

        verify(stackV4Endpoint).validateRotateRdsCertificateByCrnInternal(WORKSPACE_ID_DEFAULT, dlCrn, USER_CRN);
        assertEquals("Validation failed RDS SSL certificate rotation is not triggerable", actualException.getMessage());
    }

    @Test
    void testValidateRdsSslCertRotationWhenValidationFailsWithBadRequestExceptionResponseHasEntity() {
        String dlCrn = "dlCrn";
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn:cdp:datalake:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        String expectedMessage = "Uh-Oh validation failed";
        Response jaxRsResp = mock(Response.class);
        when(jaxRsResp.hasEntity()).thenReturn(true);
        when(jaxRsResp.readEntity(eq(ExceptionResponse.class))).thenReturn(new ExceptionResponse(expectedMessage));
        jakarta.ws.rs.BadRequestException badRequestException = mock(jakarta.ws.rs.BadRequestException.class);
        when(badRequestException.getResponse()).thenReturn(jaxRsResp);
        doThrow(badRequestException)
                .when(stackV4Endpoint).validateRotateRdsCertificateByCrnInternal(WORKSPACE_ID_DEFAULT, dlCrn, USER_CRN);

        BadRequestException actualException = assertThrows(BadRequestException.class,
                () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.validateRdsSslCertRotation(dlCrn)));

        verify(stackV4Endpoint).validateRotateRdsCertificateByCrnInternal(WORKSPACE_ID_DEFAULT, dlCrn, USER_CRN);
        assertEquals(expectedMessage, actualException.getMessage());
    }

    @Test
    void testValidateRdsSslCertRotationWhenValidationFailsWithNonBadRequest() {
        String dlCrn = "dlCrn";
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn("crn:cdp:datalake:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        doThrow(new jakarta.ws.rs.InternalServerErrorException("Uh-oh"))
                .when(stackV4Endpoint).validateRotateRdsCertificateByCrnInternal(WORKSPACE_ID_DEFAULT, dlCrn, USER_CRN);

        IllegalStateException actualException = assertThrows(IllegalStateException.class,
                () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.validateRdsSslCertRotation(dlCrn)));

        verify(stackV4Endpoint).validateRotateRdsCertificateByCrnInternal(WORKSPACE_ID_DEFAULT, dlCrn, USER_CRN);
        assertEquals("Failed to validate RDS SSL certificate rotation for SDX", actualException.getMessage());
    }
}
