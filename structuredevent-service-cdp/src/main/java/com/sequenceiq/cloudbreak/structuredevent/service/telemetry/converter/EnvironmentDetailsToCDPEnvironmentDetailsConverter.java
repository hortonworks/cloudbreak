package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.EnvironmentDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.credential.CredentialDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.credential.CredentialType;
import com.sequenceiq.environment.environment.domain.Region;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.environment.parameter.dto.AwsDiskEncryptionParametersDto;
import com.sequenceiq.environment.parameter.dto.AwsParametersDto;
import com.sequenceiq.environment.parameter.dto.AzureParametersDto;
import com.sequenceiq.environment.parameter.dto.AzureResourceEncryptionParametersDto;
import com.sequenceiq.environment.parameter.dto.GcpParametersDto;
import com.sequenceiq.environment.parameter.dto.GcpResourceEncryptionParametersDto;
import com.sequenceiq.environment.parameter.dto.ParametersDto;

@Component
public class EnvironmentDetailsToCDPEnvironmentDetailsConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentDetailsToCDPEnvironmentDetailsConverter.class);

    private static final int DEFAULT_INTEGER_VALUE = -1;

    @Inject
    private EnvironmentDetailsToCDPNetworkDetailsConverter networkDetailsConverter;

    private final Map<CredentialType, UsageProto.CDPCredentialType.Value> credentialTypeMap = Map.of(
            CredentialType.AWS_KEY_BASED, UsageProto.CDPCredentialType.Value.AWS_KEY_BASED,
            CredentialType.AWS_ROLE_BASED, UsageProto.CDPCredentialType.Value.AWS_ROLE_BASED,
            CredentialType.AZURE_CODEGRANTFLOW, UsageProto.CDPCredentialType.Value.AZURE_CODEGRANTFLOW,
            CredentialType.AZURE_APPBASED_SECRET, UsageProto.CDPCredentialType.Value.AZURE_APPBASED_SECRET,
            CredentialType.AZURE_APPBASED_CERTIFICATE, UsageProto.CDPCredentialType.Value.AZURE_APPBASED_CERTIFICATE,
            CredentialType.GCP_P12, UsageProto.CDPCredentialType.Value.GCP_P12,
            CredentialType.GCP_JSON, UsageProto.CDPCredentialType.Value.GCP_JSON,
            CredentialType.YARN, UsageProto.CDPCredentialType.Value.YARN,
            CredentialType.MOCK, UsageProto.CDPCredentialType.Value.MOCK,
            CredentialType.UNKNOWN, UsageProto.CDPCredentialType.Value.UNKNOWN
    );

    public UsageProto.CDPEnvironmentDetails convert(EnvironmentDetails srcEnvironmentDetails) {
        UsageProto.CDPEnvironmentDetails.Builder cdpEnvironmentDetails = UsageProto.CDPEnvironmentDetails.newBuilder();
        cdpEnvironmentDetails.setNumberOfAvailabilityZones(DEFAULT_INTEGER_VALUE);

        if (srcEnvironmentDetails != null) {
            if (srcEnvironmentDetails.getRegions() != null) {
                cdpEnvironmentDetails.setRegion(srcEnvironmentDetails.getRegions().stream()
                        .map(Region::getName)
                        .filter(Objects::nonNull)
                        .map(region -> region.toLowerCase().replace(" ", ""))
                        .sorted()
                        .distinct()
                        .collect(Collectors.joining(",")));
            }

            if (srcEnvironmentDetails.getCloudPlatform() != null) {
                cdpEnvironmentDetails.setEnvironmentType(UsageProto.CDPEnvironmentsEnvironmentType
                        .Value.valueOf(srcEnvironmentDetails.getCloudPlatform()));
            }

            NetworkDto network = srcEnvironmentDetails.getNetwork();
            if (network != null && network.getSubnetMetas() != null) {
                if (network.getSubnetMetas().isEmpty()) {
                    cdpEnvironmentDetails.setNumberOfAvailabilityZones(0);
                } else {
                    List<String> availabilityZones = network.getSubnetMetas().values()
                            .stream()
                            .map(CloudSubnet::getAvailabilityZone)
                            .filter(Objects::nonNull)
                            .sorted()
                            .distinct()
                            .collect(Collectors.toUnmodifiableList());
                    if (!availabilityZones.isEmpty()) {
                        cdpEnvironmentDetails.setNumberOfAvailabilityZones(availabilityZones.size());
                        cdpEnvironmentDetails.setAvailabilityZones(String.join(",", availabilityZones));
                    }
                }
            }
            cdpEnvironmentDetails.setNetworkDetails(networkDetailsConverter.convert(srcEnvironmentDetails));

            cdpEnvironmentDetails.setAwsDetails(convertAwsDetails(srcEnvironmentDetails.getParameters()));
            cdpEnvironmentDetails.setAzureDetails(convertAzureDetails(srcEnvironmentDetails.getParameters()));
            cdpEnvironmentDetails.setGcpDetails(convertGcpDetails(srcEnvironmentDetails.getParameters()));

            Map<String, String> userTags = srcEnvironmentDetails.getUserDefinedTags();
            if (userTags != null && !userTags.isEmpty()) {
                cdpEnvironmentDetails.setUserTags(JsonUtil.writeValueAsStringSilentSafe(userTags));
            }
            cdpEnvironmentDetails.setCredentialDetails(convertCredentialDetails(srcEnvironmentDetails.getCredentialDetails()));

            cdpEnvironmentDetails.setSecretEncryptionEnabled(srcEnvironmentDetails.isEnableSecretEncryption());
            cdpEnvironmentDetails.setCreatorClient(srcEnvironmentDetails.creatorClient());
        }

        UsageProto.CDPEnvironmentDetails ret = cdpEnvironmentDetails.build();
        LOGGER.debug("Converted CDPEnvironmentDetails event: {}", ret);
        return ret;
    }

    private UsageProto.CDPCredentialDetails convertCredentialDetails(CredentialDetails credentialDetails) {
        UsageProto.CDPCredentialDetails.Builder cdpCredentialDetails = UsageProto.CDPCredentialDetails.newBuilder();
        if (credentialDetails != null && credentialDetails.getCredentialType() != null) {
            cdpCredentialDetails.setCredentialType(credentialTypeMap.getOrDefault(
                    credentialDetails.getCredentialType(), UsageProto.CDPCredentialType.Value.UNKNOWN));
        } else {
            cdpCredentialDetails.setCredentialType(UsageProto.CDPCredentialType.Value.UNKNOWN);
        }
        return cdpCredentialDetails.build();
    }

    private UsageProto.CDPEnvironmentAwsDetails convertAwsDetails(ParametersDto parametersDto) {
        UsageProto.CDPEnvironmentAwsDetails.Builder builder = UsageProto.CDPEnvironmentAwsDetails.newBuilder();
        if (parametersDto != null) {
            AwsParametersDto awsParametersDto = parametersDto.getAwsParametersDto();
            if (awsParametersDto != null) {
                Optional<String> encryptionKeyArn = Optional.of(awsParametersDto)
                        .map(AwsParametersDto::getAwsDiskEncryptionParametersDto)
                        .map(AwsDiskEncryptionParametersDto::getEncryptionKeyArn);
                builder.setResourceEncryptionEnabled(encryptionKeyArn.isPresent());
            }
        }
        return builder.build();
    }

    private UsageProto.CDPEnvironmentAzureDetails convertAzureDetails(ParametersDto parametersDto) {
        UsageProto.CDPEnvironmentAzureDetails.Builder builder = UsageProto.CDPEnvironmentAzureDetails.newBuilder();
        if (parametersDto != null) {
            AzureParametersDto azureParametersDto = parametersDto.getAzureParametersDto();
            if (azureParametersDto != null) {
                builder.setSingleResourceGroup(
                        azureParametersDto.getAzureResourceGroupDto().getResourceGroupUsagePattern().isSingleResourceGroup());
                Optional<String> encryptionKeyUrl = Optional.of(azureParametersDto)
                        .map(AzureParametersDto::getAzureResourceEncryptionParametersDto)
                        .map(AzureResourceEncryptionParametersDto::getEncryptionKeyUrl);
                builder.setResourceEncryptionEnabled(encryptionKeyUrl.isPresent());
            }
        }
        return builder.build();
    }

    private UsageProto.CDPEnvironmentGcpDetails convertGcpDetails(ParametersDto parametersDto) {
        UsageProto.CDPEnvironmentGcpDetails.Builder builder = UsageProto.CDPEnvironmentGcpDetails.newBuilder();
        if (parametersDto != null) {
            GcpParametersDto gcpParametersDto = parametersDto.getGcpParametersDto();
            if (gcpParametersDto != null) {
                Optional<String> encryptionKey = Optional.of(gcpParametersDto)
                        .map(GcpParametersDto::getGcpResourceEncryptionParametersDto)
                        .map(GcpResourceEncryptionParametersDto::getEncryptionKey);
                builder.setResourceEncryptionEnabled(encryptionKey.isPresent());
            }
        }
        return builder.build();
    }
}
