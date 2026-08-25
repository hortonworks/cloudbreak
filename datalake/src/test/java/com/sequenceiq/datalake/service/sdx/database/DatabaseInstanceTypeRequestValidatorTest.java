package com.sequenceiq.datalake.service.sdx.database;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseRequest;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.service.database.DatabaseInstanceTypeCapabilityValidator;
import com.sequenceiq.cloudbreak.service.database.DatabaseInstanceTypeValidationInput;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.LocationResponse;
import com.sequenceiq.environment.api.v1.platformresource.EnvironmentPlatformResourceEndpoint;
import com.sequenceiq.environment.api.v1.platformresource.model.DatabaseVmTypeMetaJson;
import com.sequenceiq.environment.api.v1.platformresource.model.DatabaseVmTypeResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformDatabaseCapabilitiesResponse;
import com.sequenceiq.sdx.api.model.SdxDatabaseRequest;

@ExtendWith(MockitoExtension.class)
class DatabaseInstanceTypeRequestValidatorTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:accountId:user:userId";

    @InjectMocks
    private DatabaseInstanceTypeRequestValidator underTest;

    @Mock
    private EnvironmentPlatformResourceEndpoint environmentPlatformResourceEndpoint;

    @Mock
    private DatabaseInstanceTypeCapabilityValidator capabilityValidator;

    @Mock
    private AzureDatabaseAttributesService azureDatabaseAttributesService;

    @Test
    void blankInstanceTypeShouldNotCallEndpoint() {
        SdxDatabaseRequest sdxDbRequest = new SdxDatabaseRequest();
        sdxDbRequest.setDatabaseInstanceType("");
        DetailedEnvironmentResponse env = createEnv();

        underTest.validateIfPresent(sdxDbRequest, null, env, Architecture.X86_64, USER_CRN);

        verify(environmentPlatformResourceEndpoint, never()).getDatabaseCapabilities(
                anyString(), anyString(), anyString(), any(), any(), anyString());
    }

    @Test
    void sdxRequestInstanceTypeTakesPrecedenceOverInternal() {
        SdxDatabaseRequest sdxDbRequest = new SdxDatabaseRequest();
        sdxDbRequest.setDatabaseInstanceType("db.m5.xlarge");
        DatabaseRequest internalRequest = new DatabaseRequest();
        internalRequest.setDatabaseInstanceType("db.m5.large");
        DetailedEnvironmentResponse env = createEnv();
        PlatformDatabaseCapabilitiesResponse response = createCapabilitiesResponse();

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            when(environmentPlatformResourceEndpoint.getDatabaseCapabilities(
                    anyString(), anyString(), anyString(), any(), any(), anyString()))
                    .thenReturn(response);

            underTest.validateIfPresent(sdxDbRequest, internalRequest, env, Architecture.X86_64, USER_CRN);

            verify(capabilityValidator).validate(any(DatabaseInstanceTypeValidationInput.class));
        });
    }

    @Test
    void internalRequestUsedWhenSdxRequestHasNoInstanceType() {
        SdxDatabaseRequest sdxDbRequest = new SdxDatabaseRequest();
        DatabaseRequest internalRequest = new DatabaseRequest();
        internalRequest.setDatabaseInstanceType("db.m5.large");
        DetailedEnvironmentResponse env = createEnv();
        PlatformDatabaseCapabilitiesResponse response = createCapabilitiesResponse();

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            when(environmentPlatformResourceEndpoint.getDatabaseCapabilities(
                    anyString(), anyString(), anyString(), any(), any(), anyString()))
                    .thenReturn(response);

            underTest.validateIfPresent(sdxDbRequest, internalRequest, env, Architecture.X86_64, USER_CRN);

            verify(capabilityValidator).validate(any(DatabaseInstanceTypeValidationInput.class));
        });
    }

    @Test
    void badRequestExceptionShouldPropagate() {
        SdxDatabaseRequest sdxDbRequest = new SdxDatabaseRequest();
        sdxDbRequest.setDatabaseInstanceType("db.invalid");
        DetailedEnvironmentResponse env = createEnv();
        PlatformDatabaseCapabilitiesResponse response = createCapabilitiesResponse();

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            when(environmentPlatformResourceEndpoint.getDatabaseCapabilities(
                    anyString(), anyString(), anyString(), any(), any(), anyString()))
                    .thenReturn(response);
            doThrow(new BadRequestException("Instance type not available"))
                    .when(capabilityValidator).validate(any());

            assertThrows(BadRequestException.class,
                    () -> underTest.validateIfPresent(sdxDbRequest, null, env, Architecture.X86_64, USER_CRN));
        });
    }

    @Test
    void exceptionFromEndpointShouldBeSwallowed() {
        SdxDatabaseRequest sdxDbRequest = new SdxDatabaseRequest();
        sdxDbRequest.setDatabaseInstanceType("db.m5.large");
        DetailedEnvironmentResponse env = createEnv();

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            when(environmentPlatformResourceEndpoint.getDatabaseCapabilities(
                    anyString(), anyString(), anyString(), any(), any(), anyString()))
                    .thenThrow(new RuntimeException("Connection timeout"));

            assertDoesNotThrow(() -> underTest.validateIfPresent(sdxDbRequest, null, env, Architecture.X86_64, USER_CRN));
        });
    }

    private DetailedEnvironmentResponse createEnv() {
        DetailedEnvironmentResponse env = new DetailedEnvironmentResponse();
        env.setCrn("env-crn");
        env.setCloudPlatform("AWS");
        LocationResponse location = new LocationResponse();
        location.setName("us-east-1");
        env.setLocation(location);
        return env;
    }

    private PlatformDatabaseCapabilitiesResponse createCapabilitiesResponse() {
        DatabaseVmTypeMetaJson meta = new DatabaseVmTypeMetaJson();
        meta.setProperties(Map.of("Cpu", 4, "Memory", 16.0f, "Architecture", "x86_64"));
        DatabaseVmTypeResponse vmType = new DatabaseVmTypeResponse("db.m5.xlarge", meta);

        DatabaseVmTypeMetaJson defaultMeta = new DatabaseVmTypeMetaJson();
        defaultMeta.setProperties(Map.of("Cpu", 2, "Memory", 8.0f, "Architecture", "x86_64"));
        DatabaseVmTypeResponse defaultVmType = new DatabaseVmTypeResponse("db.m5.large", defaultMeta);

        return new PlatformDatabaseCapabilitiesResponse(
                Map.of(),
                Map.of("us-east-1", "db.m5.large"),
                Map.of(),
                Map.of(),
                "14",
                Map.of("us-east-1", Set.of(vmType, defaultVmType)));
    }
}
