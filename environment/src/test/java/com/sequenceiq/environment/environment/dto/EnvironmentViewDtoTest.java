package com.sequenceiq.environment.environment.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.sequenceiq.environment.credential.domain.CredentialView;
import com.sequenceiq.environment.environment.EnvironmentDeletionType;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.domain.EnvironmentTags;
import com.sequenceiq.environment.environment.domain.ExperimentalFeatures;
import com.sequenceiq.environment.environment.domain.Region;
import com.sequenceiq.environment.environment.dto.dataservices.EnvironmentDataServices;
import com.sequenceiq.environment.environment.dto.telemetry.EnvironmentTelemetry;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.environment.parameter.dto.ParametersDto;
import com.sequenceiq.environment.proxy.domain.ProxyConfigView;

class EnvironmentViewDtoTest {

    @Test
    void builderTest() {
        LocationDto locationDto = LocationDto.builder().build();
        Set<Region> regions = Set.of();
        NetworkDto network = NetworkDto.builder().build();
        FreeIpaCreationDto freeIpaCreation = FreeIpaCreationDto.builder(3).build();
        EnvironmentTelemetry telemetry = new EnvironmentTelemetry();
        EnvironmentBackup backup = new EnvironmentBackup();
        AuthenticationDto authentication = AuthenticationDto.builder().build();
        SecurityAccessDto securityAccess = SecurityAccessDto.builder().build();
        ParametersDto parameters = ParametersDto.builder().build();
        ExperimentalFeatures experimentalFeatures = ExperimentalFeatures.builder().build();
        EnvironmentTags tags = new EnvironmentTags(Map.of(), Map.of());
        EnvironmentDataServices dataServices = EnvironmentDataServices.builder().build();
        CredentialView credential = new CredentialView();
        ProxyConfigView proxyConfig = new ProxyConfigView();
        EncryptionProfileDto encryptionProfile = new EncryptionProfileDto();

        EnvironmentViewDto environmentViewDto = EnvironmentViewDto.builder()
                .withId(456L)
                .withLocationDto(locationDto)
                .withName("name")
                .withOriginalName("originalName")
                .withDescription("description")
                .withCloudPlatform("cloudPlatform")
                .withRegions(regions)
                .withArchived(true)
                .withDeletionTimestamp(789L)
                .withNetwork(network)
                .withAccountId("accountId")
                .withResourceCrn("resourceCrn")
                .withEnvironmentStatus(EnvironmentStatus.AVAILABLE)
                .withFreeIpaCreation(freeIpaCreation)
                .withTelemetry(telemetry)
                .withBackup(backup)
                .withAuthentication(authentication)
                .withStatusReason("statusReason")
                .withCreated(123L)
                .withSecurityAccess(securityAccess)
                .withAdminGroupName("adminGroupName")
                .withParameters(parameters)
                .withExperimentalFeatures(experimentalFeatures)
                .withTags(tags)
                .withParentEnvironmentCrn("parentEnvironmentCrn")
                .withParentEnvironmentName("parentEnvironmentName")
                .withParentEnvironmentCloudPlatform("parentEnvironmentCloudPlatform")
                .withEnvironmentServiceVersion("environmentServiceVersion")
                .withEnvironmentDeletionType(EnvironmentDeletionType.NONE)
                .withEnvironmentDomain("environmentDomain")
                .withDataServices(dataServices)
                .withEnableSecretEncryption(true)
                .withCredentialView(credential)
                .withProxyConfig(proxyConfig)
                .withEncryptionProfile(encryptionProfile)
                .build();

        assertThat(environmentViewDto).isNotNull();
        assertThat(environmentViewDto.getId()).isEqualTo(456L);
        assertThat(environmentViewDto.getLocation()).isSameAs(locationDto);
        assertThat(environmentViewDto.getName()).isEqualTo("name");
        assertThat(environmentViewDto.getOriginalName()).isEqualTo("originalName");
        assertThat(environmentViewDto.getDescription()).isEqualTo("description");
        assertThat(environmentViewDto.getCloudPlatform()).isEqualTo("cloudPlatform");
        assertThat(environmentViewDto.getRegions()).isSameAs(regions);
        assertThat(environmentViewDto.isArchived()).isTrue();
        assertThat(environmentViewDto.getDeletionTimestamp()).isEqualTo(789L);
        assertThat(environmentViewDto.getNetwork()).isSameAs(network);
        assertThat(environmentViewDto.getAccountId()).isEqualTo("accountId");
        assertThat(environmentViewDto.getResourceCrn()).isEqualTo("resourceCrn");
        assertThat(environmentViewDto.getStatus()).isEqualTo(EnvironmentStatus.AVAILABLE);
        assertThat(environmentViewDto.getFreeIpaCreation()).isSameAs(freeIpaCreation);
        assertThat(environmentViewDto.getTelemetry()).isSameAs(telemetry);
        assertThat(environmentViewDto.getBackup()).isSameAs(backup);
        assertThat(environmentViewDto.getAuthentication()).isSameAs(authentication);
        assertThat(environmentViewDto.getStatusReason()).isEqualTo("statusReason");
        assertThat(environmentViewDto.getCreated()).isEqualTo(123L);
        assertThat(environmentViewDto.getSecurityAccess()).isSameAs(securityAccess);
        assertThat(environmentViewDto.getAdminGroupName()).isEqualTo("adminGroupName");
        assertThat(environmentViewDto.getParameters()).isSameAs(parameters);
        assertThat(environmentViewDto.getExperimentalFeatures()).isSameAs(experimentalFeatures);
        assertThat(environmentViewDto.getTags()).isSameAs(tags);
        assertThat(environmentViewDto.getParentEnvironmentCrn()).isEqualTo("parentEnvironmentCrn");
        assertThat(environmentViewDto.getParentEnvironmentName()).isEqualTo("parentEnvironmentName");
        assertThat(environmentViewDto.getParentEnvironmentCloudPlatform()).isEqualTo("parentEnvironmentCloudPlatform");
        assertThat(environmentViewDto.getEnvironmentServiceVersion()).isEqualTo("environmentServiceVersion");
        assertThat(environmentViewDto.getDeletionType()).isEqualTo(EnvironmentDeletionType.NONE);
        assertThat(environmentViewDto.getDomain()).isEqualTo("environmentDomain");
        assertThat(environmentViewDto.getDataServices()).isSameAs(dataServices);
        assertThat(environmentViewDto.isEnableSecretEncryption()).isTrue();
        assertThat(environmentViewDto.getCredentialView()).isSameAs(credential);
        assertThat(environmentViewDto.getProxyConfig()).isSameAs(proxyConfig);
        assertThat(environmentViewDto.getEncryptionProfile()).isEqualTo(encryptionProfile);
    }

}