package com.sequenceiq.redbeams.service.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.PlatformResources;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseVmType;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.PlatformDatabaseCapabilities;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.redbeams.converter.cloud.CredentialToExtendedCloudCredentialConverter;
import com.sequenceiq.redbeams.dto.Credential;
import com.sequenceiq.redbeams.service.CredentialService;

@ExtendWith(MockitoExtension.class)
class DatabaseInstanceTypeValidatorTest {

    private static final String ENVIRONMENT_CRN = "crn:cdp:environments:us-west-1:cloudera:environment:test-env";

    private static final String CLOUD_PLATFORM = "AWS";

    private static final String REGION = "us-east-1";

    private static final String PRIMARY_TYPE = "db.m5.large";

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock
    private CredentialService credentialService;

    @Mock
    private CredentialToExtendedCloudCredentialConverter extendedCredentialConverter;

    @Mock
    private CloudConnector cloudConnector;

    @Mock
    private PlatformResources platformResources;

    @Mock
    private Credential credential;

    @Mock
    private ExtendedCloudCredential extendedCloudCredential;

    @InjectMocks
    private DatabaseInstanceTypeValidator underTest;

    private void setupCloudMocks() {
        when(cloudPlatformConnectors.get(any())).thenReturn(cloudConnector);
        when(cloudConnector.platformResources()).thenReturn(platformResources);
        when(credentialService.getCredentialByEnvCrn(ENVIRONMENT_CRN)).thenReturn(credential);
        when(extendedCredentialConverter.convert(credential, CLOUD_PLATFORM)).thenReturn(extendedCloudCredential);
    }

    @Test
    void emptyFallbackListPassesWithoutValidation() {
        Optional<String> result = underTest.validate(PRIMARY_TYPE, Collections.emptyList(), ENVIRONMENT_CRN, CLOUD_PLATFORM, REGION);
        assertThat(result).isEmpty();
    }

    @Test
    void nullFallbackListPassesWithoutValidation() {
        Optional<String> result = underTest.validate(PRIMARY_TYPE, null, ENVIRONMENT_CRN, CLOUD_PLATFORM, REGION);
        assertThat(result).isEmpty();
    }

    @Test
    void fallbacksProvidedButPrimaryIsBlankThrows() {
        List<String> fallbacks = List.of("db.m6i.large");

        assertThatThrownBy(() -> underTest.validate("", fallbacks, ENVIRONMENT_CRN, CLOUD_PLATFORM, REGION))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Primary instance type must be specified");
    }

    @Test
    void fallbacksProvidedButPrimaryIsNullThrows() {
        List<String> fallbacks = List.of("db.m6i.large");

        assertThatThrownBy(() -> underTest.validate(null, fallbacks, ENVIRONMENT_CRN, CLOUD_PLATFORM, REGION))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Primary instance type must be specified");
    }

    @Test
    void blankEntryInFallbackListThrows() {
        List<String> fallbacks = new ArrayList<>();
        fallbacks.add("db.m6i.large");
        fallbacks.add("  ");

        assertThatThrownBy(() -> underTest.validate(PRIMARY_TYPE, fallbacks, ENVIRONMENT_CRN, CLOUD_PLATFORM, REGION))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("position 2 is blank");
    }

    @Test
    void nullEntryInFallbackListThrows() {
        List<String> fallbacks = new ArrayList<>();
        fallbacks.add("db.m6i.large");
        fallbacks.add(null);

        assertThatThrownBy(() -> underTest.validate(PRIMARY_TYPE, fallbacks, ENVIRONMENT_CRN, CLOUD_PLATFORM, REGION))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("position 2 is blank");
    }

    @Test
    void duplicateEntriesInFallbackListThrows() {
        List<String> fallbacks = List.of("db.m6i.large", "db.m7i.large", "db.m6i.large");

        assertThatThrownBy(() -> underTest.validate(PRIMARY_TYPE, fallbacks, ENVIRONMENT_CRN, CLOUD_PLATFORM, REGION))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Duplicate fallback instance type: 'db.m6i.large'");
    }

    @Test
    void primaryRepeatedInFallbacksThrows() {
        List<String> fallbacks = List.of(PRIMARY_TYPE, "db.m6i.large");

        assertThatThrownBy(() -> underTest.validate(PRIMARY_TYPE, fallbacks, ENVIRONMENT_CRN, CLOUD_PLATFORM, REGION))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Primary instance type 'db.m5.large' must not be repeated");
    }

    @Test
    void allTypesAvailableInRegionPasses() {
        setupCloudMocks();
        List<String> fallbacks = List.of("db.m6i.large", "db.m7i.large");
        setupAvailableTypes(Set.of(PRIMARY_TYPE, "db.m6i.large", "db.m7i.large", "db.r5.large"));

        Optional<String> result = underTest.validate(PRIMARY_TYPE, fallbacks, ENVIRONMENT_CRN, CLOUD_PLATFORM, REGION);
        assertThat(result).isEmpty();
    }

    @Test
    void fallbackTypeNotAvailableInRegionThrows() {
        setupCloudMocks();
        List<String> fallbacks = List.of("db.m6i.large", "db.m7i.large");
        setupAvailableTypes(Set.of(PRIMARY_TYPE, "db.m6i.large", "db.r5.large"));

        assertThatThrownBy(() -> underTest.validate(PRIMARY_TYPE, fallbacks, ENVIRONMENT_CRN, CLOUD_PLATFORM, REGION))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("db.m7i.large")
                .hasMessageContaining("not available in region");
    }

    @Test
    void primaryTypeNotAvailableInRegionThrows() {
        setupCloudMocks();
        List<String> fallbacks = List.of("db.m6i.large");
        setupAvailableTypes(Set.of("db.m6i.large", "db.r5.large"));

        assertThatThrownBy(() -> underTest.validate(PRIMARY_TYPE, fallbacks, ENVIRONMENT_CRN, CLOUD_PLATFORM, REGION))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining(PRIMARY_TYPE)
                .hasMessageContaining("not available in region");
    }

    @Test
    void emptyCapabilitiesReturnsWarning() {
        setupCloudMocks();
        List<String> fallbacks = List.of("db.m6i.large");
        PlatformDatabaseCapabilities capabilities = new PlatformDatabaseCapabilities(
                Map.of(), Map.of(), Map.of(), null, Map.of());
        when(platformResources.databaseCapabilities(any(), any(), anyMap())).thenReturn(capabilities);

        Optional<String> result = underTest.validate(PRIMARY_TYPE, fallbacks, ENVIRONMENT_CRN, CLOUD_PLATFORM, REGION);
        assertThat(result).isPresent();
        assertThat(result.get()).contains("Could not validate database instance type availability");
    }

    @Test
    void unsupportedOperationExceptionReturnsWarning() {
        setupCloudMocks();
        List<String> fallbacks = List.of("db.m6i.large");
        when(platformResources.databaseCapabilities(any(), any(), anyMap())).thenThrow(new UnsupportedOperationException("not supported"));

        Optional<String> result = underTest.validate(PRIMARY_TYPE, fallbacks, ENVIRONMENT_CRN, CLOUD_PLATFORM, REGION);
        assertThat(result).isPresent();
        assertThat(result.get()).contains("Could not validate database instance type availability");
    }

    @Test
    void runtimeExceptionDuringCapabilitiesFetchReturnsWarning() {
        setupCloudMocks();
        List<String> fallbacks = List.of("db.m6i.large");
        when(platformResources.databaseCapabilities(any(), any(), anyMap())).thenThrow(new RuntimeException("connection timeout"));

        Optional<String> result = underTest.validate(PRIMARY_TYPE, fallbacks, ENVIRONMENT_CRN, CLOUD_PLATFORM, REGION);
        assertThat(result).isPresent();
        assertThat(result.get()).contains("Could not validate database instance type availability");
    }

    private void setupAvailableTypes(Set<String> types) {
        Region region = Region.region(REGION);
        Set<DatabaseVmType> vmTypes = new java.util.HashSet<>();
        for (String type : types) {
            vmTypes.add(DatabaseVmType.databaseVmType(type, null));
        }
        PlatformDatabaseCapabilities capabilities = new PlatformDatabaseCapabilities(
                Map.of(), Map.of(), Map.of(), null, Map.of(region, vmTypes));
        when(platformResources.databaseCapabilities(any(), any(), anyMap())).thenReturn(capabilities);
    }
}
