package com.sequenceiq.datalake.service.sdx;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.ws.rs.ClientErrorException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.dyngr.Polling;
import com.dyngr.core.AttemptResults;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.RecoveryMode;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.authentication.StackAuthenticationV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.environment.EnvironmentSettingsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.environment.placement.PlacementSettingsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.image.ImageSettingsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.securitygroup.SecurityGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.InstanceTemplateV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.volume.RootVolumeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.volume.VolumeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.tags.TagsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.requests.SecurityRuleV4Request;
import com.sequenceiq.cloudbreak.client.CloudbreakUserCrnClient;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.type.InstanceGroupType;
import com.sequenceiq.datalake.controller.exception.BadRequestException;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxClusterStatus;
import com.sequenceiq.datalake.repository.SdxClusterRepository;

@Service
public class ProvisionerService {

    public static final String CLUSTER_DEFINITION = "CDP 1.0 - SDX: Hive Metastore";

    private static final Logger LOGGER = LoggerFactory.getLogger(ProvisionerService.class);

    private static final int DISK_SIZE = 100;

    private static final int DISK_COUNT = 2;

    private static final int WOKER_NODE_COUNT = 3;

    private static final int MASTER_NODE_COUNT = 1;

    private String dummySshKey = "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQC0Rfl2G2vDs6yc19RxCqReunFgpYj+ucyLobpTCBtfDwzIbJot2Fmife6M42mBtiTmAK6x8kc"
            + "UEeab6CB4MUzsqF7vGTFUjwWirG/XU5pYXFUBhi8xzey+KS9KVrQ+UuKJh/AN9iSQeMV+rgT1yF5+etVH+bK1/37QCKp3+mCqjFzPyQOrvkGZv4sYyRwX7BKBLleQmIVWpofpj"
            + "T7BfcCxH877RzC5YMIi65aBc82Dl6tH6OEiP7mzByU52yvH6JFuwZ/9fWj1vXCWJzxx2w0F1OU8Zwg8gNNzL+SVb9+xfBE7xBHMpYFg72hBWPh862Ce36F4NZd3MpWMSjMmpDPh"
            + "centos";

    @Inject
    private CloudbreakUserCrnClient cloudbreakClient;

    @Inject
    private SdxClusterRepository sdxClusterRepository;

    public void startProvisioning(Long id) {
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
                        LOGGER.info("Polling cloudbreak for stack status: '{}' in '{}' env", sdxCluster.getClusterName(), sdxCluster.getEnvName());
                        StackV4Response stackV4Response = cloudbreakClient.withCrn(sdxCluster.getInitiatorUserCrn())
                                .stackV4Endpoint()
                                .get(0L, sdxCluster.getClusterName(), Collections.emptySet());
                        LOGGER.info("Response from cloudbreak: {}", JsonUtil.writeValueAsString(stackV4Response));
                        if (stackV4Response.getStatus().isAvailable()) {
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
        StackV4Request stackV4Request = new StackV4Request();
        stackV4Request.setName(sdxCluster.getClusterName());
        TagsV4Request tags = new TagsV4Request();
        try {
            tags.setUserDefined(sdxCluster.getTags().get(HashMap.class));
        } catch (IOException e) {
            throw new BadRequestException("can not convert from json to tags");
        }
        stackV4Request.setTags(tags);

        EnvironmentSettingsV4Request environment = new EnvironmentSettingsV4Request();
        environment.setName(sdxCluster.getEnvName());
        stackV4Request.setEnvironment(environment);

        PlacementSettingsV4Request placementSettingsV4Request = new PlacementSettingsV4Request();
        placementSettingsV4Request.setRegion("eu-west-1");
        placementSettingsV4Request.setAvailabilityZone("eu-west-1b");
        stackV4Request.setPlacement(placementSettingsV4Request);

        StackAuthenticationV4Request stackAuthenticationV4Request = new StackAuthenticationV4Request();
        stackAuthenticationV4Request.setPublicKey(dummySshKey);
        stackV4Request.setAuthentication(stackAuthenticationV4Request);

        ClusterV4Request clusterV4Request = new ClusterV4Request();
        clusterV4Request.setBlueprintName(CLUSTER_DEFINITION);
        clusterV4Request.setUserName("admin");
        clusterV4Request.setPassword("admin123");
        clusterV4Request.setValidateBlueprint(false);
        stackV4Request.setCluster(clusterV4Request);

        ImageSettingsV4Request image = new ImageSettingsV4Request();
        image.setOs("redhat7");
        stackV4Request.setImage(image);

        InstanceGroupV4Request masterInstanceGroupV4Request = new InstanceGroupV4Request();
        masterInstanceGroupV4Request.setName("master");

        InstanceTemplateV4Request template = new InstanceTemplateV4Request();
        Set<VolumeV4Request> attachedVolumes = new HashSet<>();
        VolumeV4Request volumeV4Request = new VolumeV4Request();
        volumeV4Request.setSize(DISK_SIZE);
        volumeV4Request.setCount(DISK_COUNT);
        volumeV4Request.setType("standard");
        attachedVolumes.add(volumeV4Request);
        template.setAttachedVolumes(attachedVolumes);
        template.setInstanceType("m5.2xlarge");
        RootVolumeV4Request rootVolume = new RootVolumeV4Request();
        rootVolume.setSize(DISK_SIZE);
        template.setRootVolume(rootVolume);

        masterInstanceGroupV4Request.setTemplate(template);
        masterInstanceGroupV4Request.setNodeCount(1);
        masterInstanceGroupV4Request.setType(InstanceGroupType.GATEWAY);
        masterInstanceGroupV4Request.setRecoveryMode(RecoveryMode.MANUAL);

        SecurityGroupV4Request securityGroup = new SecurityGroupV4Request();
        ArrayList<SecurityRuleV4Request> securityRules = new ArrayList<>();
        SecurityRuleV4Request securityRule9443 = new SecurityRuleV4Request();
        securityRule9443.setSubnet(sdxCluster.getAccessCidr());
        ArrayList<String> ports = new ArrayList<>();
        ports.add("443");
        ports.add("9443");
        ports.add("22");
        securityRule9443.setPorts(ports);
        securityRule9443.setProtocol("tcp");
        securityRules.add(securityRule9443);
        securityGroup.setSecurityRules(securityRules);

        masterInstanceGroupV4Request.setSecurityGroup(securityGroup);

        ArrayList<InstanceGroupV4Request> instanceGroups = new ArrayList<>();
        instanceGroups.add(masterInstanceGroupV4Request);
        stackV4Request.setInstanceGroups(instanceGroups);

        return stackV4Request;
    }
}
