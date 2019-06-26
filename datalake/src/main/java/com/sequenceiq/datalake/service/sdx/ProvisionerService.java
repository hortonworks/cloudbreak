package com.sequenceiq.datalake.service.sdx;

import static com.sequenceiq.cloudbreak.exception.NotFoundException.notFound;
import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNull;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;

import javax.inject.Inject;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.dyngr.Polling;
import com.dyngr.core.AttemptResults;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.AwsNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.AzureNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.authentication.StackAuthenticationV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.environment.placement.PlacementSettingsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.network.NetworkV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.tags.TagsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ClusterV4Response;
import com.sequenceiq.cloudbreak.client.CloudbreakUserCrnClient;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.cloudbreak.util.PasswordUtil;
import com.sequenceiq.datalake.controller.exception.BadRequestException;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxClusterStatus;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;

@Service
public class ProvisionerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProvisionerService.class);

    @Inject
    private CloudbreakUserCrnClient cloudbreakClient;

    @Inject
    private SdxClusterRepository sdxClusterRepository;

    @Inject
    private Clock clock;

    @Value("${sdx.cluster.definition}")
    private String clusterDefinition;

    public void startStackDeletion(Long id) {
        sdxClusterRepository.findById(id).ifPresentOrElse(sdxCluster -> {
            try {
                cloudbreakClient.withCrn(sdxCluster.getInitiatorUserCrn())
                        .stackV4Endpoint()
                        .delete(0L, sdxCluster.getClusterName(), false, false);
            } catch (NotFoundException e) {
                LOGGER.info("Can not find stack on cloudbreak side {}", sdxCluster.getClusterName());
            } catch (ClientErrorException e) {
                LOGGER.info("Can not delete stack from cloudbreak: {}", sdxCluster.getClusterName());
                throw new RuntimeException("Can not delete stack, client error happened", e);
            }
        }, () -> {
            throw notFound("SDX cluster", id).get();
        });
    }

    public void waitCloudbreakClusterDeletion(Long id, PollingConfig pollingConfig) {
        sdxClusterRepository.findById(id).ifPresentOrElse(sdxCluster -> {
            Polling.waitPeriodly(pollingConfig.getSleepTime(), pollingConfig.getSleepTimeUnit())
                    .stopIfException(false)
                    .stopAfterDelay(pollingConfig.getDuration(), pollingConfig.getDurationTimeUnit())
                    .run(() -> {
                        LOGGER.info("Deletion polling cloudbreak for stack status: '{}' in '{}' env", sdxCluster.getClusterName(), sdxCluster.getEnvName());
                        try {
                            StackV4Response stackV4Response = cloudbreakClient.withCrn(sdxCluster.getInitiatorUserCrn())
                                    .stackV4Endpoint()
                                    .get(0L, sdxCluster.getClusterName(), Collections.emptySet());
                            LOGGER.info("Response from cloudbreak: {}", JsonUtil.writeValueAsString(stackV4Response));
                            if (Status.DELETE_FAILED.equals(stackV4Response.getStatus())) {
                                return AttemptResults.breakFor("Stack deletion failed " + sdxCluster.getClusterName());
                            } else {
                                return AttemptResults.justContinue();
                            }
                        } catch (NotFoundException e) {
                            return AttemptResults.finishWith(null);
                        }
                    });
            sdxCluster.setStatus(SdxClusterStatus.DELETED);
            sdxCluster.setDeleted(clock.getCurrentTimeMillis());
            sdxClusterRepository.save(sdxCluster);
        }, () -> {
            throw notFound("SDX cluster", id).get();
        });
    }

    public void startStackProvisioning(Long id, DetailedEnvironmentResponse environment) {
        sdxClusterRepository.findById(id).ifPresentOrElse(sdxCluster -> {
            StackV4Request stackV4Request = setupStackRequestForCloudbreak(sdxCluster, environment);

            LOGGER.info("Call cloudbreak with stackrequest");
            try {
                sdxCluster.setStackRequestToCloudbreak(JsonUtil.writeValueAsString(stackV4Request));
                StackV4Response stackV4Response = cloudbreakClient.withCrn(sdxCluster.getInitiatorUserCrn())
                        .stackV4Endpoint()
                        .post(0L, stackV4Request);
                sdxCluster.setStackId(stackV4Response.getId());
                sdxCluster.setStatus(SdxClusterStatus.REQUESTED_FROM_CLOUDBREAK);
                sdxClusterRepository.save(sdxCluster);
                LOGGER.info("Sdx cluster updated");
            } catch (ClientErrorException e) {
                LOGGER.info("Can not start provisioning", e);
                throw new RuntimeException("Can not start provisioning, client error happened", e);
            } catch (JsonProcessingException e) {
                LOGGER.info("Can not write stackrequest to json");
                throw new RuntimeException("Can not write stackrequest to json", e);
            }
        }, () -> {
            throw notFound("SDX cluster", id).get();
        });
    }

    public void waitCloudbreakClusterCreation(Long id, PollingConfig pollingConfig) {
        sdxClusterRepository.findById(id).ifPresentOrElse(sdxCluster -> {
            Polling.waitPeriodly(pollingConfig.getSleepTime(), pollingConfig.getSleepTimeUnit())
                    .stopIfException(false)
                    .stopAfterDelay(pollingConfig.getDuration(), pollingConfig.getDurationTimeUnit())
                    .run(() -> {
                        LOGGER.info("Polling cloudbreak for stack status: '{}' in '{}' env", sdxCluster.getClusterName(), sdxCluster.getEnvName());
                        try {
                            StackV4Response stackV4Response = cloudbreakClient.withCrn(sdxCluster.getInitiatorUserCrn())
                                    .stackV4Endpoint()
                                    .get(0L, sdxCluster.getClusterName(), Collections.emptySet());
                            LOGGER.info("Response from cloudbreak: {}", JsonUtil.writeValueAsString(stackV4Response));
                            ClusterV4Response cluster = stackV4Response.getCluster();
                            if (stackV4Response.getStatus().isAvailable()
                                    && cluster != null
                                    && cluster.getStatus() != null
                                    && cluster.getStatus().isAvailable()) {
                                return AttemptResults.finishWith(stackV4Response);
                            } else {
                                if (Status.CREATE_FAILED.equals(stackV4Response.getStatus())) {
                                    LOGGER.info("Stack creation failed {}, status reason is: {}",
                                            sdxCluster.getClusterName(), stackV4Response.getStatusReason());
                                    return AttemptResults.breakFor("Stack creation failed " + sdxCluster.getClusterName());
                                } else {
                                    return AttemptResults.justContinue();
                                }
                            }
                        } catch (NotFoundException e) {
                            LOGGER.debug("Stack not found on CB side " + sdxCluster.getClusterName(), e);
                            return AttemptResults.breakFor("Stack not found on CB side " + sdxCluster.getClusterName());
                        }
                    });
            sdxCluster.setStatus(SdxClusterStatus.RUNNING);
            sdxClusterRepository.save(sdxCluster);
        }, () -> {
            throw notFound("SDX cluster", id).get();
        });
    }

    private StackV4Request setupStackRequestForCloudbreak(SdxCluster sdxCluster, DetailedEnvironmentResponse environment) {
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
            setupAuthentication(environment, stackRequest);
            setupClusterRequest(stackRequest);
            return stackRequest;
        } catch (IOException e) {
            LOGGER.error("Can not parse json to stack request");
            throw new IllegalStateException("Can not parse json to stack request", e);
        }
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
}
