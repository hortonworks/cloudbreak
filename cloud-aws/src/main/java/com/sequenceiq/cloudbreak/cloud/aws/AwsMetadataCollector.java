package com.sequenceiq.cloudbreak.cloud.aws;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.Tag;
import com.sequenceiq.cloudbreak.cloud.MetadataCollector;
import com.sequenceiq.cloudbreak.cloud.aws.task.AwsPollTaskFactory;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstanceMetaData;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;

@Service
public class AwsMetadataCollector implements MetadataCollector {
    private static final Logger LOGGER = LoggerFactory.getLogger(AwsMetadataCollector.class);
    private static final String CBNAME = "cbname";

    @Inject
    private AwsClient awsClient;
    @Inject
    private CloudFormationStackUtil cloudFormationStackUtil;
    @Inject
    private SyncPollingScheduler<Boolean> syncPollingScheduler;
    @Inject
    private AwsPollTaskFactory awsPollTaskFactory;

    @Override
    public List<CloudVmInstanceStatus> collect(AuthenticatedContext ac, List<CloudResource> resources, List<InstanceTemplate> vms) {
        List<CloudInstance> cloudInstances = new ArrayList<>();
        try {
            AmazonCloudFormationClient amazonCFClient =
                    awsClient.createCloudFormationClient(new AwsCredentialView(ac.getCloudCredential()), ac.getCloudContext().getRegion());
            AmazonAutoScalingClient amazonASClient =
                    awsClient.createAutoScalingClient(new AwsCredentialView(ac.getCloudCredential()), ac.getCloudContext().getRegion());
            AmazonEC2Client amazonEC2Client =
                    awsClient.createAccess(new AwsCredentialView(ac.getCloudCredential()), ac.getCloudContext().getRegion());
            Map<String, List<InstanceTemplate>> instanceIds1 = getInstanceIds(vms);


            scheduleStatusChecks(ac, amazonCFClient, instanceIds1);

            for (Map.Entry<String, List<InstanceTemplate>> stringListEntry : instanceIds1.entrySet()) {
                List<String> instanceIds = new ArrayList<>();
                String asGroupName = cloudFormationStackUtil.getAutoscalingGroupName(ac, amazonCFClient, stringListEntry.getKey());
                instanceIds.addAll(cloudFormationStackUtil.getInstanceIds(ac, amazonASClient, amazonCFClient, asGroupName));
                DescribeInstancesRequest instancesRequest = new DescribeInstancesRequest().withInstanceIds(instanceIds);
                DescribeInstancesResult instancesResult = amazonEC2Client.describeInstances(instancesRequest);

                List<String> visibleTags = getVisibleTagsFromInstanceResult(instancesResult);

                List<InstanceTemplate> remainingTemplates = getRemainingTemplates(stringListEntry, visibleTags);

                for (Reservation reservation : instancesResult.getReservations()) {
                    LOGGER.info("Number of instances found in reservation: {}", reservation.getInstances().size());
                    for (com.amazonaws.services.ec2.model.Instance instance : reservation.getInstances()) {
                        boolean tagWasCreated = false;
                        String tagName = null;
                        for (Tag tag : instance.getTags()) {
                            if (CBNAME.equals(tag.getKey())) {
                                tagWasCreated = true;
                                tagName = tag.getValue();
                                break;
                            }
                        }
                        if (!tagWasCreated) {
                            InstanceTemplate instanceTemplate = remainingTemplates.get(0);
                            tagName = awsClient.getCbName(stringListEntry.getKey(), instanceTemplate.getPrivateId());
                            Tag t = new Tag();
                            t.setKey(CBNAME);
                            t.setValue(tagName);
                            CreateTagsRequest ctr = new CreateTagsRequest();
                            ctr.setTags(Arrays.asList(t));
                            ctr.withResources(instance.getInstanceId());
                            amazonEC2Client.createTags(ctr);
                            remainingTemplates.remove(instanceTemplate);
                        }
                        CloudInstanceMetaData md = new CloudInstanceMetaData(instance.getPrivateIpAddress(), instance.getPublicIpAddress());
                        InstanceTemplate instanceTemplate = getInstanceTemplate(stringListEntry.getKey(), stringListEntry.getValue(), tagName);
                        if (instanceTemplate != null) {
                            InstanceTemplate template = stringListEntry.getValue().get(reservation.getInstances().indexOf(instance));
                            cloudInstances.add(new CloudInstance(instance.getInstanceId(), md, template));
                        }
                    }
                }
            }

            return createInstanceStatusFromInstances(cloudInstances);
        } catch (Exception e) {
            throw new CloudConnectorException(e.getMessage(), e);
        }
    }

    private List<InstanceTemplate> getRemainingTemplates(Map.Entry<String, List<InstanceTemplate>> stringListEntry, List<String> visibleTags) {
        List<InstanceTemplate> remainingTemplates = new ArrayList<>();
        for (InstanceTemplate instanceTemplate : stringListEntry.getValue()) {
            String cbName = awsClient.getCbName(instanceTemplate.getGroupName(), instanceTemplate.getPrivateId());
            if (!visibleTags.contains(cbName)) {
                remainingTemplates.add(instanceTemplate);
            }
        }
        return remainingTemplates;
    }

    private List<String> getVisibleTagsFromInstanceResult(DescribeInstancesResult instancesResult) {
        List<String> visibleTags = new ArrayList<>();
        for (Reservation reservation : instancesResult.getReservations()) {
            for (com.amazonaws.services.ec2.model.Instance instance : reservation.getInstances()) {
                for (Tag tag : instance.getTags()) {
                    if (CBNAME.equals(tag.getKey())) {
                        visibleTags.add(tag.getValue());
                        break;
                    }
                }
            }
        }
        return visibleTags;
    }

    private void scheduleStatusChecks(AuthenticatedContext ac, AmazonCloudFormationClient amazonCFClient, Map<String, List<InstanceTemplate>> instanceIds1) {
        for (Map.Entry<String, List<InstanceTemplate>> stringListEntry : instanceIds1.entrySet()) {
            if (stringListEntry.getValue().get(0).getParameter("spotPrice", Long.class) == null) {
                String asGroupName = cloudFormationStackUtil.getAutoscalingGroupName(ac, amazonCFClient, stringListEntry.getKey());
                PollTask<Boolean> task = awsPollTaskFactory.newASGroupStatusCheckerTask(ac, asGroupName, stringListEntry.getValue().size(),
                        awsClient, cloudFormationStackUtil);
                try {
                    Boolean statePollerResult = task.call();
                    if (!task.completed(statePollerResult)) {
                        syncPollingScheduler.schedule(task);
                    }
                } catch (Exception e) {
                    throw new CloudConnectorException(e.getMessage(), e);
                }
            }
        }
    }

    private List<CloudVmInstanceStatus> createInstanceStatusFromInstances(List<CloudInstance> cloudInstances) {
        List<CloudVmInstanceStatus> results = new ArrayList<>();
        for (CloudInstance cloudInstance : cloudInstances) {
            results.add(new CloudVmInstanceStatus(cloudInstance, InstanceStatus.CREATED));
        }
        return results;
    }

    private InstanceTemplate getInstanceTemplate(String group, List<InstanceTemplate> instanceTemplates, String search) {
        for (InstanceTemplate instanceTemplate : instanceTemplates) {
            if (awsClient.getCbName(group, instanceTemplate.getPrivateId()).equals(search)) {
                return instanceTemplate;
            }
        }
        return null;
    }


    private Map<String, List<InstanceTemplate>> getInstanceIds(List<InstanceTemplate> vms) {
        Map<String, List<InstanceTemplate>> result = new HashMap<>();
        for (InstanceTemplate vm : vms) {
            if (result.keySet().contains(vm.getGroupName())) {
                result.get(vm.getGroupName()).add(vm);
            } else {
                List<InstanceTemplate> tmp = new ArrayList<>();
                tmp.add(vm);
                result.put(vm.getGroupName(), tmp);
            }
        }
        return result;
    }

}
