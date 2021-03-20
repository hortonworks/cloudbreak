package com.sequenceiq.environment.platformresource.v1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.ws.rs.BadRequestException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.convert.ConversionService;

import com.sequenceiq.authorization.service.CommonPermissionCheckingUtils;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.cloud.model.nosql.CloudNoSqlTables;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformNoSqlTablesResponse;
import com.sequenceiq.environment.platformresource.PlatformParameterService;
import com.sequenceiq.environment.platformresource.PlatformResourceRequest;

@ExtendWith(MockitoExtension.class)
class CredentialPlatformResourceControllerTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:123:user:123";

    private static final String CREDENTIAL_CRN = "crn:cdp:environments:us-west-1:123:credential:123";

    @Mock
    private ConversionService convertersionService;

    @Mock
    private PlatformParameterService platformParameterService;

    @Mock
    private CommonPermissionCheckingUtils commonPermissionCheckingUtils;

    @InjectMocks
    private CredentialPlatformResourceController underTest;

    @Test
    void getNoSqlTables() {
        CloudNoSqlTables noSqlTables = new CloudNoSqlTables();
        PlatformResourceRequest platformResourceRequest = new PlatformResourceRequest();
        PlatformNoSqlTablesResponse response = new PlatformNoSqlTablesResponse();
        when(platformParameterService.getPlatformResourceRequest(any(), any(), any(), any(), any(), any())).thenReturn(platformResourceRequest);
        when(platformParameterService.getNoSqlTables(any(PlatformResourceRequest.class))).thenReturn(noSqlTables);
        when(convertersionService.convert(noSqlTables, PlatformNoSqlTablesResponse.class)).thenReturn(response);
        doNothing().when(commonPermissionCheckingUtils).checkPermissionForUserOnResource(any(), any(), any());

        PlatformNoSqlTablesResponse result = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.getNoSqlTables(null,
                    CREDENTIAL_CRN, "region", "aws", "az"));

        verify(platformParameterService).getNoSqlTables(platformResourceRequest);
        verify(convertersionService).convert(noSqlTables, PlatformNoSqlTablesResponse.class);
        assertEquals(response, result);

    }

    @Test
    void testGetNoSqlTablesShouldThrowBadRequestExceptionWhenTheSpecifiedCredentialCrnIsInvalid() {
        Assertions.assertThrows(BadRequestException.class, () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.getNoSqlTables(null,
                "new", "region", "aws", "az")));

    }
}
