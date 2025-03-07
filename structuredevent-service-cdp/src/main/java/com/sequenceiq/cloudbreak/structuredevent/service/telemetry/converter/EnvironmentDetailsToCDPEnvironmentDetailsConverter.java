package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPAzureComputeClusterDetails;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPComputeClusterDetails;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPCredentialDetails;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPCredentialType;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPEnvironmentAwsDetails;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPEnvironmentAzureDetails;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPEnvironmentDetails;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPEnvironmentGcpDetails;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPEnvironmentsEnvironmentType;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.EnvironmentDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.credential.CredentialDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.credential.CredentialType;
import com.sequenceiq.environment.environment.domain.Region;
import com.sequenceiq.environment.environment.dto.ExternalizedComputeClusterDto;
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

    private final Map<CredentialType, CDPCredentialType.Value> credentialTypeMap = Map.of(
            CredentialType.AWS_KEY_BASED, CDPCredentialType.Value.AWS_KEY_BASED,
            CredentialType.AWS_ROLE_BASED, CDPCredentialType.Value.AWS_ROLE_BASED,
            CredentialType.AZURE_CODEGRANTFLOW, CDPCredentialType.Value.AZURE_CODEGRANTFLOW,
            CredentialType.AZURE_APPBASED_SECRET, CDPCredentialType.Value.AZURE_APPBASED_SECRET,
            CredentialType.AZURE_APPBASED_CERTIFICATE, CDPCredentialType.Value.AZURE_APPBASED_CERTIFICATE,
            CredentialType.GCP_P12, CDPCredentialType.Value.GCP_P12,
            CredentialType.GCP_JSON, CDPCredentialType.Value.GCP_JSON,
            CredentialType.YARN, CDPCredentialType.Value.YARN,
            CredentialType.MOCK, CDPCredentialType.Value.MOCK,
            CredentialType.UNKNOWN, CDPCredentialType.Value.UNKNOWN
    );

    @Inject
    private EnvironmentDetailsToCDPNetworkDetailsConverter networkDetailsConverter;

    public CDPEnvironmentDetails convert(EnvironmentDetails srcEnvironmentDetails) {
        CDPEnvironmentDetails.Builder cdpEnvironmentDetails = CDPEnvironmentDetails.newBuilder();
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
                cdpEnvironmentDetails.setEnvironmentType(CDPEnvironmentsEnvironmentType
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
            cdpEnvironmentDetails.setComputeClusterDetails(convertComputeClusterDetails(srcEnvironmentDetails));
            cdpEnvironmentDetails.setEnvironmentDeletionType(
                    EnvironmentDeletionTypeToCDPEnvironmentDeletionType.convert(srcEnvironmentDetails.getEnvironmentDeletionTypeAsString()));
        }

        CDPEnvironmentDetails ret = cdpEnvironmentDetails.build();
        LOGGER.debug("Converted CDPEnvironmentDetails event: {}", ret);
        return ret;
    }

    private CDPComputeClusterDetails convertComputeClusterDetails(EnvironmentDetails environmentDetails) {
        CDPComputeClusterDetails.Builder cdpComputeClusterDetails = CDPComputeClusterDetails.newBuilder();
        ExternalizedComputeClusterDto externalizedComputeCluster = environmentDetails.getExternalizedComputeCluster();
        if (externalizedComputeCluster != null) {
            cdpComputeClusterDetails.setEnabled(externalizedComputeCluster.isCreate());
            cdpComputeClusterDetails.setPrivateCluster(externalizedComputeCluster.isPrivateCluster());
            cdpComputeClusterDetails.addAllKubeApiAuthorizedIpRanges(externalizedComputeCluster.getKubeApiAuthorizedIpRanges());
            cdpComputeClusterDetails.addAllWorkerNodeSubnetIds(externalizedComputeCluster.getWorkerNodeSubnetIds());
            if (environmentDetails.getCloudPlatform() != null && CloudPlatform.AZURE.equals(CloudPlatform.valueOf(environmentDetails.getCloudPlatform()))) {
                cdpComputeClusterDetails.setAzureComputeClusterDetails(CDPAzureComputeClusterDetails.newBuilder()
                        .setOutboundType(externalizedComputeCluster.getOutboundType())
                        .build());
            }
        } else {
            cdpComputeClusterDetails.setEnabled(false);
        }
        return cdpComputeClusterDetails.build();
    }

    private CDPCredentialDetails convertCredentialDetails(CredentialDetails credentialDetails) {
        CDPCredentialDetails.Builder cdpCredentialDetails = CDPCredentialDetails.newBuilder();
        if (credentialDetails != null && credentialDetails.getCredentialType() != null) {
            cdpCredentialDetails.setCredentialType(credentialTypeMap.getOrDefault(
                    credentialDetails.getCredentialType(), CDPCredentialType.Value.UNKNOWN));
        } else {
            cdpCredentialDetails.setCredentialType(CDPCredentialType.Value.UNKNOWN);
        }
        return cdpCredentialDetails.build();
    }

    private CDPEnvironmentAwsDetails convertAwsDetails(ParametersDto parametersDto) {
        CDPEnvironmentAwsDetails.Builder builder = CDPEnvironmentAwsDetails.newBuilder();
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

    private CDPEnvironmentAzureDetails convertAzureDetails(ParametersDto parametersDto) {
        CDPEnvironmentAzureDetails.Builder builder = CDPEnvironmentAzureDetails.newBuilder();
        if (parametersDto != null) {
            AzureParametersDto azureParametersDto = parametersDto.getAzureParametersDto();
            if (azureParametersDto != null) {
                builder.setSingleResourceGroup(
                        azureParametersDto.getAzureResourceGroupDto().getResourceGroupUsagePattern().isSingleResourceGroup());
                Optional<String> encryptionKeyUrl = Optional.of(azureParametersDto)
                        .map(AzureParametersDto::getAzureResourceEncryptionParametersDto)
                        .map(AzureResourceEncryptionParametersDto::getEncryptionKeyUrl);
                builder.setResourceEncryptionEnabled(encryptionKeyUrl.isPresent());
                Optional<String> encryptionManagedIdentity = Optional.of(azureParametersDto)
                        .map(AzureParametersDto::getAzureResourceEncryptionParametersDto)
                        .map(AzureResourceEncryptionParametersDto::getUserManagedIdentity);
                builder.setEncryptionManagedIdentity(encryptionManagedIdentity.orElse(""));
            }
        }
        return builder.build();
    }

    private CDPEnvironmentGcpDetails convertGcpDetails(ParametersDto parametersDto) {
        CDPEnvironmentGcpDetails.Builder builder = CDPEnvironmentGcpDetails.newBuilder();
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
