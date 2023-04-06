package com.sequenceiq.it.cloudbreak.util.aws.amazonec2.action;

import static java.lang.String.format;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.BooleanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.util.aws.amazoncf.action.CfClientActions;
import com.sequenceiq.it.cloudbreak.util.aws.amazonec2.client.EC2Client;

import software.amazon.awssdk.core.retry.backoff.FixedDelayBackoffStrategy;
import software.amazon.awssdk.core.waiters.WaiterOverrideConfiguration;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.services.cloudformation.model.Stack;
import software.amazon.awssdk.services.cloudformation.model.StackResourceSummary;
import software.amazon.awssdk.services.cloudformation.model.StackSummary;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.DescribeLaunchTemplateVersionsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeLaunchTemplateVersionsResponse;
import software.amazon.awssdk.services.ec2.model.DescribeLaunchTemplatesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeLaunchTemplatesResponse;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesResponse;
import software.amazon.awssdk.services.ec2.model.EbsInstanceBlockDevice;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.InstanceBlockDeviceMapping;
import software.amazon.awssdk.services.ec2.model.InstanceState;
import software.amazon.awssdk.services.ec2.model.InstanceStateName;
import software.amazon.awssdk.services.ec2.model.LaunchTemplate;
import software.amazon.awssdk.services.ec2.model.LaunchTemplateVersion;
import software.amazon.awssdk.services.ec2.model.Reservation;
import software.amazon.awssdk.services.ec2.model.StopInstancesRequest;
import software.amazon.awssdk.services.ec2.model.StopInstancesResponse;
import software.amazon.awssdk.services.ec2.model.Tag;
import software.amazon.awssdk.services.ec2.model.TerminateInstancesRequest;
import software.amazon.awssdk.services.ec2.model.TerminateInstancesResponse;
import software.amazon.awssdk.services.ec2.model.Volume;
import software.amazon.awssdk.services.lambda.model.Ec2UnexpectedException;

@Component
public class EC2ClientActions extends EC2Client {

    private static final Logger LOGGER = LoggerFactory.getLogger(EC2ClientActions.class);

    @Inject
    private CfClientActions cfClientActions;

    public List<String> getInstanceVolumeIds(List<String> instanceIds, boolean rootVolumes) {
        DescribeInstancesResponse describeInstancesResponse;
        try (Ec2Client ec2Client = buildEC2Client()) {
            describeInstancesResponse = ec2Client.describeInstances(DescribeInstancesRequest.builder().instanceIds(instanceIds).build());
        }

        Map<String, Set<String>> instanceIdVolumeIdMap = describeInstancesResponse.reservations()
                .stream()
                .map(Reservation::instances)
                .flatMap(Collection::stream)
                .collect(Collectors.toMap(Instance::instanceId,
                        instance -> instance.blockDeviceMappings()
                                .stream()
                                .filter(dev -> {
                                    if (BooleanUtils.isTrue(rootVolumes)) {
                                        return "/dev/xvda".equals(dev.deviceName());
                                    } else {
                                        return !"/dev/xvda".equals(dev.deviceName());
                                    }
                                })
                                .map(InstanceBlockDeviceMapping::ebs)
                                .map(EbsInstanceBlockDevice::volumeId)
                                .collect(Collectors.toSet())
                ));
        instanceIdVolumeIdMap.forEach((instanceId, volumeIds) -> Log.log(LOGGER, format(" Attached volume IDs are %s for [%s] EC2 instance ",
                volumeIds.toString(), instanceId)));
        return instanceIdVolumeIdMap
                .values()
                .stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    public List<String> listInstanceTypes(List<String> instanceIds) {
        try (Ec2Client ec2Client = buildEC2Client()) {
            DescribeInstancesResponse describeInstanceResponse = ec2Client.describeInstances(DescribeInstancesRequest.builder()
                    .instanceIds(instanceIds)
                    .build());
            return describeInstanceResponse.reservations().stream()
                    .flatMap(instances -> instances.instances().stream().map(instance -> instance.instanceType().toString()))
                    .collect(Collectors.toList());
        }
    }

    public void deleteHostGroupInstances(List<String> instanceIds) {
        try (Ec2Client ec2Client = buildEC2Client()) {
            TerminateInstancesResponse terminateInstancesResponse = ec2Client.terminateInstances(TerminateInstancesRequest.builder()
                    .instanceIds(instanceIds).build());
            for (String instanceId : instanceIds) {
                try {
                    Log.log(LOGGER, format(" EC2 instance [%s] state is [%s] ", instanceId,
                            Objects.requireNonNull(terminateInstancesResponse.terminatingInstances().stream()
                                    .filter(instance -> instance.instanceId().equals(instanceId))
                                    .findAny().orElse(null)
                            ).currentState().name()));

                    WaiterResponse<DescribeInstancesResponse> waiterResponse = ec2Client.waiter().waitUntilInstanceTerminated(
                            DescribeInstancesRequest.builder()
                                    .instanceIds(instanceId)
                                    .build(),
                            WaiterOverrideConfiguration.builder()
                                    .maxAttempts(80)
                                    .backoffStrategy(FixedDelayBackoffStrategy.create(Duration.ofSeconds(30))).build());

                    if (waiterResponse.matched().exception().isPresent()) {
                        throw waiterResponse.matched().exception().get();
                    }

                    DescribeInstancesResponse describeInstanceResponse = ec2Client.describeInstances(DescribeInstancesRequest.builder()
                            .instanceIds(instanceId)
                            .build());
                    InstanceState actualInstanceState = describeInstanceResponse.reservations().get(0).instances().get(0).state();
                    if (InstanceStateName.TERMINATED == actualInstanceState.name()) {
                        Log.log(LOGGER, format(" EC2 Instance: %s state is: %s ", instanceId, InstanceStateName.TERMINATED));
                    } else {
                        LOGGER.error("EC2 Instance: {} termination has not been successful. So the actual state is: {} ",
                                instanceId, actualInstanceState.name());
                        throw new TestFailException(" EC2 Instance: " + instanceId
                                + " termination has not been successful, because of the actual state is: "
                                + actualInstanceState.name());
                    }
                } catch (Ec2UnexpectedException e) {
                    LOGGER.error("EC2 Instance {} termination has not been successful, because of EC2UnexpectedException: {}", instanceId, e);
                } catch (Throwable e) {
                    LOGGER.error("EC2 Instance {} termination has not been successful, because of Exception: {}", instanceId, e);
                }
            }
        }
    }

    public void stopHostGroupInstances(List<String> instanceIds) {
        try (Ec2Client ec2Client = buildEC2Client()) {
            StopInstancesResponse stopInstancesResponse = ec2Client.stopInstances(StopInstancesRequest.builder().instanceIds(instanceIds).build());
            for (String instanceId : instanceIds) {
                try {
                    Log.log(LOGGER, format(" EC2 instance [%s] state is [%s] ", instanceId,
                            Objects.requireNonNull(stopInstancesResponse.stoppingInstances().stream()
                                    .filter(instance -> instance.instanceId().equals(instanceId))
                                    .findAny().orElse(null)
                            ).currentState().name()));

                    WaiterResponse<DescribeInstancesResponse> waiterResponse = ec2Client.waiter().waitUntilInstanceStopped(
                            DescribeInstancesRequest.builder()
                                    .instanceIds(instanceId)
                                    .build(),
                            WaiterOverrideConfiguration.builder()
                                    .maxAttempts(80)
                                    .backoffStrategy(FixedDelayBackoffStrategy.create(Duration.ofSeconds(30))).build());

                    if (waiterResponse.matched().exception().isPresent()) {
                        throw waiterResponse.matched().exception().get();
                    }

                    DescribeInstancesResponse describeInstanceResponse = ec2Client.describeInstances(DescribeInstancesRequest.builder()
                            .instanceIds(instanceId)
                            .build());
                    InstanceState actualInstanceState = describeInstanceResponse.reservations().get(0).instances().get(0).state();
                    if (InstanceStateName.STOPPED.equals(actualInstanceState.name())) {
                        Log.log(LOGGER, format(" EC2 Instance: %s state is: %s ", instanceId, InstanceStateName.STOPPED));
                    } else {
                        LOGGER.error("EC2 Instance: {} stop has not been successful. So the actual state is: {} ",
                                instanceId, actualInstanceState.name());
                        throw new TestFailException(" EC2 Instance: " + instanceId
                                + " stop has not been successful, because of the actual state is: "
                                + actualInstanceState.name());
                    }
                } catch (Ec2UnexpectedException e) {
                    LOGGER.error("EC2 Instance {} stop has not been successful, because of EC2UnexpectedException: {}", instanceId, e);
                } catch (Throwable e) {
                    LOGGER.error("EC2 Instance {} termination has not been successful, because of Exception: {}", instanceId, e);
                }
            }
        }
    }

    public Map<String, Boolean> enaSupport(List<String> instanceIds) {
        try (Ec2Client ec2Client = buildEC2Client()) {
            DescribeInstancesResponse describeInstanceResponse = ec2Client.describeInstances(DescribeInstancesRequest.builder()
                    .instanceIds(instanceIds).build());
            return describeInstanceResponse.reservations().stream().flatMap(it -> it.instances().stream())
                    .collect(Collectors.toMap(Instance::instanceId, Instance::enaSupport));
        }
    }

    public Map<String, String> instanceSubnet(List<String> instanceIds) {
        try (Ec2Client ec2Client = buildEC2Client()) {
            DescribeInstancesResponse response = ec2Client.describeInstances(DescribeInstancesRequest.builder()
                    .instanceIds(instanceIds).build());
            return response.reservations().stream().flatMap(it -> it.instances().stream())
                    .collect(Collectors.toMap(Instance::instanceId, Instance::subnetId));
        }
    }

    public Map<String, Map<String, String>> listTagsByInstanceId(List<String> instanceIds) {
        try (Ec2Client ec2Client = buildEC2Client()) {
            DescribeInstancesRequest describeInstancesRequest = DescribeInstancesRequest.builder().instanceIds(instanceIds).build();
            DescribeInstancesResponse describeInstancesResponse = ec2Client.describeInstances(describeInstancesRequest);
            return describeInstancesResponse.reservations().stream()
                    .flatMap(reservation -> reservation.instances().stream())
                    .collect(Collectors.toMap(Instance::instanceId, this::getTagsForInstance));
        }
    }

    private Map<String, String> getTagsForInstance(Instance instance) {
        return instance.tags().stream()
                .collect(Collectors.toMap(Tag::key, Tag::value));
    }

    public List<String> getRootVolumesKmsKeys(List<String> instanceIds) {
        List<String> volumeIds = getInstanceVolumeIds(instanceIds, true);
        DescribeVolumesResponse describeVolumesResponse;
        try (Ec2Client ec2Client = buildEC2Client()) {
            describeVolumesResponse = ec2Client.describeVolumes(DescribeVolumesRequest.builder().volumeIds(volumeIds).build());
        }
        Map<String, String> volumeIdKmsIdMap = describeVolumesResponse.volumes()
                .stream()
                .collect(Collectors.toMap(Volume::volumeId, Volume::kmsKeyId));
        volumeIdKmsIdMap.forEach((volumeId, kmsKeyId) -> Log.log(LOGGER, format(" Following KMS Key IDs are available: [%s] for '%s' EC2 volume. ",
                kmsKeyId, volumeId)));
        return new ArrayList<>(volumeIdKmsIdMap.values());
    }

    public Map<String, String> listLaunchTemplatesUserData(String stack) {
        List<StackResourceSummary> launchTemplateList = cfClientActions.getLaunchTemplatesToStack(stack);
        Map<String, String> result = new HashMap<>();
        try (Ec2Client client = buildEC2Client()) {
            List<String> launchTemplateIds = launchTemplateList.stream()
                    .map(StackResourceSummary::physicalResourceId)
                    .collect(Collectors.toList());
            DescribeLaunchTemplatesResponse launchTemplates = client.describeLaunchTemplates(
                    DescribeLaunchTemplatesRequest.builder().launchTemplateIds(launchTemplateIds).build());
            for (LaunchTemplate launchTemplate : launchTemplates.launchTemplates()) {
                DescribeLaunchTemplateVersionsResponse ver = client.describeLaunchTemplateVersions(DescribeLaunchTemplateVersionsRequest.builder()
                        .launchTemplateId(launchTemplate.launchTemplateId())
                        .versions(String.valueOf(launchTemplate.latestVersionNumber()))
                        .build());
                LaunchTemplateVersion launchTemplateVersion = ver.launchTemplateVersions().get(0);
                String userData = new String(Base64.getDecoder().decode(launchTemplateVersion.launchTemplateData().userData()));
                result.put(launchTemplate.launchTemplateId(), userData);
            }
        }

        return result;
    }

    public Boolean isCloudFormationExistForStack(String stack) {
        List<StackSummary> stackSummaries = cfClientActions.listCfStacksByName(stack);
        LOGGER.info("Stack summaries in AWS: {}", stackSummaries.toString());
        return !stackSummaries.isEmpty();
    }

    public List<Stack> listCfStacksByEnvironment(String crn) {
        return cfClientActions.listCfStacksByTagsEnvironmentCrn(crn);
    }
}
