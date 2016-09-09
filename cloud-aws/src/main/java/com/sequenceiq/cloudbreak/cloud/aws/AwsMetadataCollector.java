package com.sequenceiq.cloudbreak.cloud.aws;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.CreateTagsRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.Tag;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.cloud.MetadataCollector;
import com.sequenceiq.cloudbreak.cloud.aws.task.AwsPollTaskFactory;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstanceMetaData;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;

@Service
public class AwsMetadataCollector implements MetadataCollector {
    private static final Logger LOGGER = LoggerFactory.getLogger(AwsMetadataCollector.class);
    private static final String TAG_NAME = "cbname";

    @Inject
    private AwsClient awsClient;
    @Inject
    private CloudFormationStackUtil cloudFormationStackUtil;
    @Inject
    private SyncPollingScheduler<Boolean> syncPollingScheduler;
    @Inject
    private AwsPollTaskFactory awsPollTaskFactory;

    @Override
    public List<CloudVmMetaDataStatus> collect(AuthenticatedContext ac, List<CloudResource> resources, List<CloudInstance> vms) {
        List<CloudVmMetaDataStatus> cloudVmMetaDataStatuses = new ArrayList<>();
        try {
            String region = ac.getCloudContext().getLocation().getRegion().value();
            AmazonCloudFormationClient amazonCFClient =
                    awsClient.createCloudFormationClient(new AwsCredentialView(ac.getCloudCredential()), region);
            AmazonAutoScalingClient amazonASClient =
                    awsClient.createAutoScalingClient(new AwsCredentialView(ac.getCloudCredential()), region);
            AmazonEC2Client amazonEC2Client =
                    awsClient.createAccess(new AwsCredentialView(ac.getCloudCredential()), region);

            //contains all instances
            ListMultimap<String, CloudInstance> groupByInstanceGroup = groupByInstanceGroup(vms);

            for (String key : groupByInstanceGroup.keySet()) {
                List<CloudInstance> cloudInstances = groupByInstanceGroup.get(key);
                cloudVmMetaDataStatuses.addAll(collectGroupMetaData(ac, amazonASClient, amazonEC2Client, amazonCFClient, key, cloudInstances));
            }

            return cloudVmMetaDataStatuses;
        } catch (Exception e) {
            throw new CloudConnectorException(e.getMessage(), e);
        }
    }

    private List<CloudVmMetaDataStatus> collectGroupMetaData(AuthenticatedContext ac, AmazonAutoScalingClient amazonASClient,
            AmazonEC2Client amazonEC2Client, AmazonCloudFormationClient amazonCFClient, String groupName, List<CloudInstance> cloudInstances) {

        List<CloudVmMetaDataStatus> cloudVmMetaDataStatuses = new ArrayList<>();

        String asGroupName = cloudFormationStackUtil.getAutoscalingGroupName(ac, amazonCFClient, groupName);
        List<String> instanceIds = cloudFormationStackUtil.getInstanceIds(amazonASClient, asGroupName);

        DescribeInstancesRequest instancesRequest = cloudFormationStackUtil.createDescribeInstancesRequest(instanceIds);
        DescribeInstancesResult instancesResult = amazonEC2Client.describeInstances(instancesRequest);

        //contains instances with instanceId
        Map<String, CloudInstance> mapByInstanceId = mapByInstanceId(cloudInstances);

        //contains instances with privateId (without instanceId)
        Queue<CloudInstance> untrackedInstances = untrackedInstances(cloudInstances);

        for (Reservation reservation : instancesResult.getReservations()) {
            LOGGER.info("Number of instances found in reservation: {}", reservation.getInstances().size());
            for (Instance instance : reservation.getInstances()) {

                String instanceId = instance.getInstanceId();
                CloudInstance cloudInstance = ensureInstanceTag(mapByInstanceId, instance, instanceId, untrackedInstances, amazonEC2Client);
                if (cloudInstance != null) {
                    CloudInstanceMetaData md = new CloudInstanceMetaData(instance.getPrivateIpAddress(), instance.getPublicIpAddress());
                    CloudVmInstanceStatus cloudVmInstanceStatus = new CloudVmInstanceStatus(cloudInstance, InstanceStatus.CREATED);
                    CloudVmMetaDataStatus cloudVmMetaDataStatus = new CloudVmMetaDataStatus(cloudVmInstanceStatus, md);
                    cloudVmMetaDataStatuses.add(cloudVmMetaDataStatus);
                }
            }
        }

        return cloudVmMetaDataStatuses;
    }

    private CloudInstance ensureInstanceTag(Map<String, CloudInstance> mapByInstanceId, Instance instance, String instanceId, Queue<CloudInstance>
            untrackedInstances, AmazonEC2Client amazonEC2Client) {

        // we need to figure out whether it is already tracked or not, if it is already tracked then it has a tag
        String tag = getTag(instance);

        CloudInstance cloudInstance = mapByInstanceId.get(instanceId);
        if (cloudInstance == null) {
            if (tag == null) {
                // so it is not tracked at the moment, therefore it considered as a new instance, and we shall track it by tagging it, with the private id of
                // an untracked CloudInstance
                cloudInstance = untrackedInstances.remove();
                cloudInstance = new CloudInstance(instanceId, cloudInstance.getTemplate());
            }
        }

        if (cloudInstance != null && tag == null) {
            addTag(amazonEC2Client, cloudInstance, instance);
        }

        return cloudInstance;
    }

    private String getTag(Instance instance) {
        for (Tag tag : instance.getTags()) {
            if (TAG_NAME.equals(tag.getKey())) {
                String value = tag.getValue();
                LOGGER.info("Instance: {} was already tagged: {}", instance.getInstanceId(), value);
                return value;
            }
        }
        return null;
    }

    private void addTag(AmazonEC2Client amazonEC2Client, CloudInstance cloudInstance, Instance instance) {
        String tagName = awsClient.getCbName(cloudInstance.getTemplate().getGroupName(), cloudInstance.getTemplate().getPrivateId());
        Tag t = new Tag();
        t.setKey(TAG_NAME);
        t.setValue(tagName);
        CreateTagsRequest ctr = new CreateTagsRequest();
        ctr.setTags(Collections.singletonList(t));
        ctr.withResources(instance.getInstanceId());
        amazonEC2Client.createTags(ctr);
    }

    private ListMultimap<String, CloudInstance> groupByInstanceGroup(List<CloudInstance> vms) {
        ListMultimap<String, CloudInstance> groupByInstanceGroup = ArrayListMultimap.create();
        for (CloudInstance vm : vms) {
            String groupName = vm.getTemplate().getGroupName();
            groupByInstanceGroup.put(groupName, vm);
        }
        return groupByInstanceGroup;
    }

    private Map<String, CloudInstance> mapByInstanceId(List<CloudInstance> vms) {
        Map<String, CloudInstance> groupByInstanceId = Maps.newHashMap();
        for (CloudInstance vm : vms) {
            String instanceId = vm.getInstanceId();
            if (instanceId != null) {
                groupByInstanceId.put(instanceId, vm);
            }
        }
        return groupByInstanceId;
    }

    private Queue<CloudInstance> untrackedInstances(List<CloudInstance> vms) {
        Queue<CloudInstance> cloudInstances = Lists.newLinkedList();
        for (CloudInstance vm : vms) {
            if (vm.getInstanceId() == null) {
                cloudInstances.add(vm);
            }
        }
        return cloudInstances;
    }

}
