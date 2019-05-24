package com.sequenceiq.datalake.service.sdx;

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
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.authentication.StackAuthenticationV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.environment.EnvironmentSettingsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.tags.TagsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ClusterV4Response;
import com.sequenceiq.cloudbreak.client.CloudbreakUserCrnClient;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.datalake.controller.exception.BadRequestException;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxClusterStatus;
import com.sequenceiq.datalake.repository.SdxClusterRepository;

@Service
public class ProvisionerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProvisionerService.class);

    private static final int DISK_SIZE = 100;

    private static final int DISK_COUNT = 2;

    private String dummySshKey = "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQC0Rfl2G2vDs6yc19RxCqReunFgpYj+ucyLobpTCBtfDwzIbJot2Fmife6M42mBtiTmAK6x8kc"
            + "UEeab6CB4MUzsqF7vGTFUjwWirG/XU5pYXFUBhi8xzey+KS9KVrQ+UuKJh/AN9iSQeMV+rgT1yF5+etVH+bK1/37QCKp3+mCqjFzPyQOrvkGZv4sYyRwX7BKBLleQmIVWpofpj"
            + "T7BfcCxH877RzC5YMIi65aBc82Dl6tH6OEiP7mzByU52yvH6JFuwZ/9fWj1vXCWJzxx2w0F1OU8Zwg8gNNzL+SVb9+xfBE7xBHMpYFg72hBWPh862Ce36F4NZd3MpWMSjMmpDPh"
            + "centos";

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
                LOGGER.info("Can not delete stack from cloudbreak: {}", sdxCluster.getClusterName(), e);
                throw new RuntimeException("Can not delete stack, client error happened");
            }
        }, () -> {
            throw new BadRequestException("Can not find SDX cluster by ID: " + id);
        });
    }

    public void waitCloudbreakClusterDeletion(Long id, PollingConfig pollingConfig) {
        sdxClusterRepository.findById(id).ifPresentOrElse(sdxCluster -> {
            Polling.waitPeriodly(pollingConfig.getSleepTime(), pollingConfig.getSleepTimeUnit())
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
            throw new BadRequestException("Can not find SDX cluster by ID: " + id);
        });
    }

    public void startStackProvisioning(Long id) {
        sdxClusterRepository.findById(id).ifPresentOrElse(sdxCluster -> {
            StackV4Request stackV4Request = setupStackRequestForCloudbreak(sdxCluster);

            LOGGER.info("Call cloudbreak with stackrequest");
            try {
                StackV4Response stackV4Response = cloudbreakClient.withCrn(sdxCluster.getInitiatorUserCrn())
                        .stackV4Endpoint()
                        .post(0L, stackV4Request);
                sdxCluster.setStackId(stackV4Response.getId());
                sdxCluster.setStatus(SdxClusterStatus.REQUESTED_FROM_CLOUDBREAK);
                sdxClusterRepository.save(sdxCluster);
                LOGGER.info("Sdx cluster updated");
            } catch (ClientErrorException e) {
                LOGGER.info("Can not start provisioning", e);
                throw new RuntimeException("Can not start provisioning, client error happened");
            }
        }, () -> {
            throw new BadRequestException("Can not find SDX cluster by ID: " + id);
        });
    }

    public void waitCloudbreakClusterCreation(Long id, PollingConfig pollingConfig) {
        sdxClusterRepository.findById(id).ifPresentOrElse(sdxCluster -> {
            Polling.waitPeriodly(pollingConfig.getSleepTime(), pollingConfig.getSleepTimeUnit())
                    .stopAfterDelay(pollingConfig.getDuration(), pollingConfig.getDurationTimeUnit())
                    .run(() -> {
                        LOGGER.info("Creation polling cloudbreak for stack status: '{}' in '{}' env", sdxCluster.getClusterName(), sdxCluster.getEnvName());
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
                                return AttemptResults.breakFor("Stack creation failed " + sdxCluster.getClusterName());
                            } else {
                                return AttemptResults.justContinue();
                            }
                        }
                    });
            sdxCluster.setStatus(SdxClusterStatus.RUNNING);
            sdxClusterRepository.save(sdxCluster);
        }, () -> {
            throw new BadRequestException("Can not find SDX cluster by ID: " + id);
        });
    }

    private StackV4Request setupStackRequestForCloudbreak(SdxCluster sdxCluster) {

        String clusterTemplateJson = FileReaderUtils.readFileFromClasspathQuietly("sdx/" + "cluster-template.json");
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
            EnvironmentSettingsV4Request environment = new EnvironmentSettingsV4Request();
            environment.setName(sdxCluster.getEnvName());
            stackRequest.setEnvironment(environment);
            StackAuthenticationV4Request stackAuthenticationV4Request = new StackAuthenticationV4Request();
            stackAuthenticationV4Request.setPublicKey(dummySshKey);
            stackRequest.setAuthentication(stackAuthenticationV4Request);
            ClusterV4Request cluster = stackRequest.getCluster();
            cluster.setBlueprintName(clusterDefinition);
            cluster.setUserName("admin");
            cluster.setPassword("admin123");
            return stackRequest;
        } catch (IOException e) {
            LOGGER.error("Can not parse json to stack request");
            throw new IllegalStateException("Can not parse json to stack request", e);
        }
    }
}
