package com.sequenceiq.environment.platformresource.v1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.convert.ConversionService;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.cloud.model.nosql.CloudNoSqlTables;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformNoSqlTablesResponse;
import com.sequenceiq.environment.platformresource.PlatformParameterService;
import com.sequenceiq.environment.platformresource.PlatformResourceRequest;

@ExtendWith(MockitoExtension.class)
class PlatformResourceControllerTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:123:user:123";

    @Mock
    private ConversionService convertersionService;

    @Mock
    private PlatformParameterService platformParameterService;

    @InjectMocks
    private PlatformResourceController underTest;

    @Test
    void getNoSqlTables() {
        ThreadBasedUserCrnProvider.removeUserCrn();
        ThreadBasedUserCrnProvider.setUserCrn(USER_CRN);
        CloudNoSqlTables noSqlTables = new CloudNoSqlTables();
        PlatformResourceRequest platformResourceRequest = new PlatformResourceRequest();
        PlatformNoSqlTablesResponse response = new PlatformNoSqlTablesResponse();
        when(platformParameterService.getPlatformResourceRequest(any(), any(), any(), any(), any(), any())).thenReturn(platformResourceRequest);
        when(platformParameterService.getNoSqlTables(any(PlatformResourceRequest.class))).thenReturn(noSqlTables);
        when(convertersionService.convert(noSqlTables, PlatformNoSqlTablesResponse.class)).thenReturn(response);

        PlatformNoSqlTablesResponse result = underTest.getNoSqlTables("cred",
                "crn", "region", "aws", "az");

        verify(platformParameterService).getNoSqlTables(platformResourceRequest);
        verify(convertersionService).convert(noSqlTables, PlatformNoSqlTablesResponse.class);
        assertEquals(response, result);
    }
}
