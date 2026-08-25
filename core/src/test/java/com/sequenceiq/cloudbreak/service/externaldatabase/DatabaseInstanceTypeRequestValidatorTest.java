package com.sequenceiq.cloudbreak.service.externaldatabase;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.service.database.DatabaseInstanceTypeCapabilityValidator;
import com.sequenceiq.cloudbreak.service.database.DatabaseInstanceTypeValidationInput;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.common.model.DatabaseCapabilityType;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.LocationResponse;
import com.sequenceiq.environment.api.v1.platformresource.EnvironmentPlatformResourceEndpoint;
import com.sequenceiq.environment.api.v1.platformresource.model.DatabaseVmTypeMetaJson;
import com.sequenceiq.environment.api.v1.platformresource.model.DatabaseVmTypeResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformDatabaseCapabilitiesResponse;

@ExtendWith(MockitoExtension.class)
class DatabaseInstanceTypeRequestValidatorTest {

    @InjectMocks
    private DatabaseInstanceTypeRequestValidator underTest;

    @Mock
    private EnvironmentPlatformResourceEndpoint environmentPlatformResourceEndpoint;

    @Mock
    private DatabaseInstanceTypeCapabilityValidator capabilityValidator;

    @Test
    void blankInstanceTypeShouldNotCallEnvironmentEndpoint() {
        DetailedEnvironmentResponse env = createEnv();
        DatabaseRequest dbRequest = new DatabaseRequest();
        dbRequest.setDatabaseInstanceType("");

        underTest.validateIfPresent("", dbRequest, env, Architecture.X86_64);

        verify(environmentPlatformResourceEndpoint, never()).getDatabaseCapabilities(
                anyString(), anyString(), anyString(), any(), any(), anyString());
    }

    @Test
    void nullInstanceTypeShouldNotCallEnvironmentEndpoint() {
        DetailedEnvironmentResponse env = createEnv();
        DatabaseRequest dbRequest = new DatabaseRequest();

        underTest.validateIfPresent(null, dbRequest, env, Architecture.X86_64);

        verify(environmentPlatformResourceEndpoint, never()).getDatabaseCapabilities(
                anyString(), anyString(), anyString(), any(), any(), anyString());
    }

    @Test
    void validTypeShouldCallCapabilityValidator() {
        DetailedEnvironmentResponse env = createEnv();
        DatabaseRequest dbRequest = new DatabaseRequest();
        PlatformDatabaseCapabilitiesResponse response = createCapabilitiesResponse();
        when(environmentPlatformResourceEndpoint.getDatabaseCapabilities(
                eq("env-crn"), eq("us-east-1"), eq("AWS"), any(), eq(DatabaseCapabilityType.DEFAULT), eq("x86_64")))
                .thenReturn(response);

        underTest.validateIfPresent("db.m5.xlarge", dbRequest, env, Architecture.X86_64);

        verify(capabilityValidator).validate(any(DatabaseInstanceTypeValidationInput.class));
    }

    @Test
    void badRequestExceptionShouldPropagate() {
        DetailedEnvironmentResponse env = createEnv();
        DatabaseRequest dbRequest = new DatabaseRequest();
        PlatformDatabaseCapabilitiesResponse response = createCapabilitiesResponse();
        when(environmentPlatformResourceEndpoint.getDatabaseCapabilities(
                anyString(), anyString(), anyString(), any(), any(), anyString()))
                .thenReturn(response);
        doThrow(new BadRequestException("Instance type not available"))
                .when(capabilityValidator).validate(any());

        assertThrows(BadRequestException.class,
                () -> underTest.validateIfPresent("db.invalid", dbRequest, env, Architecture.X86_64));
    }

    @Test
    void environmentEndpointExceptionShouldBeSwallowed() {
        DetailedEnvironmentResponse env = createEnv();
        DatabaseRequest dbRequest = new DatabaseRequest();
        when(environmentPlatformResourceEndpoint.getDatabaseCapabilities(
                anyString(), anyString(), anyString(), any(), any(), anyString()))
                .thenThrow(new RuntimeException("Connection timeout"));

        assertDoesNotThrow(() -> underTest.validateIfPresent("db.m5.large", dbRequest, env, Architecture.X86_64));
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
