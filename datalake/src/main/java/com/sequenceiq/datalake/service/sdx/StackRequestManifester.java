package com.sequenceiq.datalake.service.sdx;

import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNull;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.responses.FileSystemParameterV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.responses.FileSystemParameterV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.AwsNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.AzureNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.authentication.StackAuthenticationV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.storage.CloudStorageV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.storage.location.StorageLocationV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.environment.placement.PlacementSettingsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.network.NetworkV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.tags.TagsV4Request;
import com.sequenceiq.cloudbreak.client.CloudbreakUserCrnClient;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.cloudbreak.util.PasswordUtil;
import com.sequenceiq.datalake.controller.exception.BadRequestException;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;
import com.sequenceiq.sdx.api.model.SdxCloudStorageRequest;
import com.sequenceiq.sdx.api.model.SdxClusterRequest;

@Service
public class StackRequestManifester {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackRequestManifester.class);

    @Inject
    private CloudbreakUserCrnClient cloudbreakClient;

    @Value("${sdx.cluster.definition}")
    private String clusterDefinition;

    public void configureStackForSdxCluster(StackV4Request internalRequest, SdxClusterRequest sdxClusterRequest,
            SdxCluster sdxCluster, DetailedEnvironmentResponse environment) {
        if (internalRequest != null) {
            addStackV4RequestAsString(sdxCluster, internalRequest);
        } else {
            //TODO: get detailed env service
            StackV4Request generatedStackV4Request = setupStackRequestForCloudbreak(sdxCluster, sdxClusterRequest, environment);
            addStackV4RequestAsString(sdxCluster, generatedStackV4Request);
        }
    }

    public void addStackV4RequestAsString(SdxCluster sdxCluster, StackV4Request internalRequest) {
        try {
            LOGGER.info("Forming request from Internal Request");
            sdxCluster.setStackRequestToCloudbreak(JsonUtil.writeValueAsString(internalRequest));
        } catch (JsonProcessingException e) {
            LOGGER.info("Can not parse stack request");
            throw new BadRequestException("Can not parse stack request", e);
        }
    }

    public StackV4Request setupStackRequestForCloudbreak(SdxCluster sdxCluster, SdxClusterRequest clusterRequest, DetailedEnvironmentResponse environment) {

        String clusterTemplateJson;
        if (sdxCluster.getStackRequest() == null) {
            clusterTemplateJson = FileReaderUtils.readFileFromClasspathQuietly("sdx/" + "cluster-template.json");
        } else {
            clusterTemplateJson = sdxCluster.getStackRequest();
        }
        try {
            StackV4Request stackRequest = JsonUtil.readValue(clusterTemplateJson, StackV4Request.class);
            stackRequest.setName(sdxCluster.getClusterName());
            TagsV4Request tags = new TagsV4Request();
            try {
                tags.setUserDefined(sdxCluster.getTags().get(HashMap.class));
            } catch (IOException e) {
                throw new BadRequestException("can not convert from json to tags");
            }
            stackRequest.setTags(tags);
            stackRequest.setEnvironmentCrn(sdxCluster.getEnvCrn());

            if (!CloudPlatform.YARN.name().equals(environment.getCloudPlatform())
                    && environment.getNetwork() != null
                    && environment.getNetwork().getSubnetMetas() != null
                    && !environment.getNetwork().getSubnetMetas().isEmpty()) {
                setupPlacement(environment, stackRequest);
                setupNetwork(environment, stackRequest);
            }
            if (isCloudStorageConfigured(clusterRequest)) {
                configureCloudStorage(environment.getCloudPlatform(), sdxCluster, clusterRequest, stackRequest);
            }
            setupAuthentication(environment, stackRequest);
            setupClusterRequest(stackRequest);
            return stackRequest;
        } catch (IOException e) {
            LOGGER.error("Can not parse json to stack request");
            throw new IllegalStateException("Can not parse json to stack request", e);
        }
    }

    private boolean isCloudStorageConfigured(SdxClusterRequest clusterRequest) {
        return clusterRequest.getCloudStorage() != null
                && StringUtils.isNotEmpty(clusterRequest.getCloudStorage().getBaseLocation());
    }

    private void setupPlacement(DetailedEnvironmentResponse environment, StackV4Request stackRequest) {
        String subnetId = environment.getNetwork().getSubnetMetas().keySet().iterator().next();
        CloudSubnet cloudSubnet = environment.getNetwork().getSubnetMetas().get(subnetId);

        PlacementSettingsV4Request placementSettingsV4Request = new PlacementSettingsV4Request();
        placementSettingsV4Request.setAvailabilityZone(cloudSubnet.getAvailabilityZone());
        placementSettingsV4Request.setRegion(environment.getRegions().getNames().iterator().next());
        stackRequest.setPlacement(placementSettingsV4Request);
    }

    private void setupNetwork(DetailedEnvironmentResponse environmentResponse, StackV4Request stackRequest) {
        stackRequest.setNetwork(convertNetwork(environmentResponse.getNetwork()));
    }

    public NetworkV4Request convertNetwork(EnvironmentNetworkResponse network) {
        NetworkV4Request response = new NetworkV4Request();
        response.setAws(getIfNotNull(network.getAws(), aws -> convertToAwsNetwork(network)));
        response.setAzure(getIfNotNull(network.getAzure(), azure -> convertToAzureNetwork(network)));
        return response;
    }

    private AzureNetworkV4Parameters convertToAzureNetwork(EnvironmentNetworkResponse source) {
        AzureNetworkV4Parameters response = new AzureNetworkV4Parameters();
        response.setNetworkId(source.getAzure().getNetworkId());
        response.setNoFirewallRules(source.getAzure().getNoFirewallRules());
        response.setNoPublicIp(source.getAzure().getNoPublicIp());
        response.setResourceGroupName(source.getAzure().getResourceGroupName());
        response.setSubnetId(source.getSubnetIds().stream().findFirst().orElseThrow(()
                -> new com.sequenceiq.cloudbreak.exception.BadRequestException("No subnet id for this environment")));
        return response;
    }

    private AwsNetworkV4Parameters convertToAwsNetwork(EnvironmentNetworkResponse source) {
        AwsNetworkV4Parameters response = new AwsNetworkV4Parameters();
        response.setSubnetId(source.getSubnetIds().stream().findFirst().orElseThrow(()
                -> new com.sequenceiq.cloudbreak.exception.BadRequestException("No subnet id for this environment")));
        response.setVpcId(source.getAws().getVpcId());
        return response;
    }

    private void setupAuthentication(DetailedEnvironmentResponse environment, StackV4Request stackRequest) {
        if (stackRequest.getAuthentication() == null) {
            StackAuthenticationV4Request stackAuthenticationV4Request = new StackAuthenticationV4Request();
            stackAuthenticationV4Request.setPublicKey(environment.getAuthentication().getPublicKey());
            stackAuthenticationV4Request.setPublicKeyId(environment.getAuthentication().getPublicKeyId());
            stackRequest.setAuthentication(stackAuthenticationV4Request);
        }
    }

    private void setupClusterRequest(StackV4Request stackRequest) {
        ClusterV4Request cluster = stackRequest.getCluster();
        if (cluster != null && cluster.getBlueprintName() == null) {
            cluster.setBlueprintName(clusterDefinition);
        }
        if (cluster != null && cluster.getUserName() == null) {
            cluster.setUserName("admin");
        }
        if (cluster != null && cluster.getPassword() == null) {
            cluster.setPassword(PasswordUtil.generatePassword());
        }
    }

    private void configureCloudStorage(String cloudPlatform, SdxCluster sdxCluster, SdxClusterRequest clusterRequest, StackV4Request stackRequest) {
        SdxCloudStorageRequest cloudStorage = clusterRequest.getCloudStorage();
        validateCloudStorage(cloudPlatform, cloudStorage);

        FileSystemParameterV4Responses fileSystemRecommendations = getFileSystemRecommendations(sdxCluster.getInitiatorUserCrn(),
                sdxCluster.getClusterName(),
                cloudStorage);

        LOGGER.info("File recommendations {}", fileSystemRecommendations);

        CloudStorageV4Request cloudStorageV4Request = new CloudStorageV4Request();
        Set<StorageLocationV4Request> locations = new HashSet<>();
        for (FileSystemParameterV4Response response : fileSystemRecommendations.getResponses()) {
            StorageLocationV4Request sl = new StorageLocationV4Request();
            sl.setPropertyName(response.getPropertyName());
            sl.setPropertyFile(response.getPropertyFile());
            sl.setValue(response.getDefaultPath());
            locations.add(sl);
        }
        cloudStorageV4Request.setLocations(locations);
        cloudStorageV4Request.setS3(cloudStorage.getS3());
        stackRequest.getCluster().setCloudStorage(cloudStorageV4Request);
    }

    private void validateCloudStorage(String cloudPlatform, SdxCloudStorageRequest cloudStorage) {
        if (CloudPlatform.AWS.name().equalsIgnoreCase(cloudPlatform)) {
            if (cloudStorage.getS3() == null || StringUtils.isEmpty(cloudStorage.getS3().getInstanceProfile())) {
                throw new BadRequestException("instance profile must be defined for S3");
            }
        }
    }

    private FileSystemParameterV4Responses getFileSystemRecommendations(String userCrn, String clusterName, SdxCloudStorageRequest cloudStorageRequest) {
        return cloudbreakClient.withCrn(userCrn).filesystemV4Endpoint()
                .getFileSystemParameters(0L,
                        clusterDefinition,
                        clusterName,
                        "",
                        cloudStorageRequest.getBaseLocation(),
                        cloudStorageRequest.getFileSystemType().toString(),
                        false,
                        false);
    }
}
