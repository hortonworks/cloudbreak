package com.sequenceiq.environment.environment.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.credential.CredentialDetails;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.environment.EnvironmentDeletionType;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.domain.EnvironmentTags;
import com.sequenceiq.environment.environment.domain.ExperimentalFeatures;
import com.sequenceiq.environment.environment.domain.Region;
import com.sequenceiq.environment.environment.dto.dataservices.EnvironmentDataServices;
import com.sequenceiq.environment.environment.dto.telemetry.EnvironmentTelemetry;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.environment.parameter.dto.ParametersDto;
import com.sequenceiq.environment.proxy.domain.ProxyConfig;

class EnvironmentDtoTest {

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
        Credential credential = new Credential();
        ProxyConfig proxyConfig = new ProxyConfig();
        CredentialDetails credentialDetails = CredentialDetails.builder().build();

        EnvironmentDto environmentDto = EnvironmentDto.builder()
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
                .withCredential(credential)
                .withProxyConfig(proxyConfig)
                .withCredentialDetails(credentialDetails)
                .build();

        assertThat(environmentDto).isNotNull();
        assertThat(environmentDto.getId()).isEqualTo(456L);
        assertThat(environmentDto.getLocation()).isSameAs(locationDto);
        assertThat(environmentDto.getName()).isEqualTo("name");
        assertThat(environmentDto.getOriginalName()).isEqualTo("originalName");
        assertThat(environmentDto.getDescription()).isEqualTo("description");
        assertThat(environmentDto.getCloudPlatform()).isEqualTo("cloudPlatform");
        assertThat(environmentDto.getRegions()).isSameAs(regions);
        assertThat(environmentDto.isArchived()).isTrue();
        assertThat(environmentDto.getDeletionTimestamp()).isEqualTo(789L);
        assertThat(environmentDto.getNetwork()).isSameAs(network);
        assertThat(environmentDto.getAccountId()).isEqualTo("accountId");
        assertThat(environmentDto.getResourceCrn()).isEqualTo("resourceCrn");
        assertThat(environmentDto.getStatus()).isEqualTo(EnvironmentStatus.AVAILABLE);
        assertThat(environmentDto.getFreeIpaCreation()).isSameAs(freeIpaCreation);
        assertThat(environmentDto.getTelemetry()).isSameAs(telemetry);
        assertThat(environmentDto.getBackup()).isSameAs(backup);
        assertThat(environmentDto.getAuthentication()).isSameAs(authentication);
        assertThat(environmentDto.getStatusReason()).isEqualTo("statusReason");
        assertThat(environmentDto.getCreated()).isEqualTo(123L);
        assertThat(environmentDto.getSecurityAccess()).isSameAs(securityAccess);
        assertThat(environmentDto.getAdminGroupName()).isEqualTo("adminGroupName");
        assertThat(environmentDto.getParameters()).isSameAs(parameters);
        assertThat(environmentDto.getExperimentalFeatures()).isSameAs(experimentalFeatures);
        assertThat(environmentDto.getTags()).isSameAs(tags);
        assertThat(environmentDto.getParentEnvironmentCrn()).isEqualTo("parentEnvironmentCrn");
        assertThat(environmentDto.getParentEnvironmentName()).isEqualTo("parentEnvironmentName");
        assertThat(environmentDto.getParentEnvironmentCloudPlatform()).isEqualTo("parentEnvironmentCloudPlatform");
        assertThat(environmentDto.getEnvironmentServiceVersion()).isEqualTo("environmentServiceVersion");
        assertThat(environmentDto.getDeletionType()).isEqualTo(EnvironmentDeletionType.NONE);
        assertThat(environmentDto.getDomain()).isEqualTo("environmentDomain");
        assertThat(environmentDto.getDataServices()).isSameAs(dataServices);
        assertThat(environmentDto.isEnableSecretEncryption()).isTrue();
        assertThat(environmentDto.getCredential()).isSameAs(credential);
        assertThat(environmentDto.getProxyConfig()).isSameAs(proxyConfig);
        assertThat(environmentDto.getCredentialDetails()).isSameAs(credentialDetails);
    }

}