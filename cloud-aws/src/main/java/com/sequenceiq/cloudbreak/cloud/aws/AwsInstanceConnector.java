package com.sequenceiq.cloudbreak.cloud.aws;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.AmazonEC2Exception;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.GetConsoleOutputRequest;
import com.amazonaws.services.ec2.model.GetConsoleOutputResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.StartInstancesRequest;
import com.amazonaws.services.ec2.model.StopInstancesRequest;
import com.sequenceiq.cloudbreak.cloud.InstanceConnector;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudOperationNotSupportedException;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;

@Service
public class AwsInstanceConnector implements InstanceConnector {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsInstanceConnector.class);

    private static final String INSTANCE_NOT_FOUND_ERROR_CODE = "InvalidInstanceID.NotFound";

    @Inject
    private AwsClient awsClient;

    @Value("${cb.aws.hostkey.verify:}")
    private boolean verifyHostKey;

    @Override
    public String getConsoleOutput(AuthenticatedContext authenticatedContext, CloudInstance vm) {
        if (!verifyHostKey) {
            throw new CloudOperationNotSupportedException("Host key verification is disabled on AWS");
        }
        AmazonEC2Client amazonEC2Client = awsClient.createAccess(new AwsCredentialView(authenticatedContext.getCloudCredential()),
                authenticatedContext.getCloudContext().getLocation().getRegion().value());
        GetConsoleOutputRequest getConsoleOutputRequest = new GetConsoleOutputRequest().withInstanceId(vm.getInstanceId());
        GetConsoleOutputResult getConsoleOutputResult = amazonEC2Client.getConsoleOutput(getConsoleOutputRequest);
        try {
            return getConsoleOutputResult.getOutput() == null ? "" : getConsoleOutputResult.getDecodedOutput();
        } catch (Exception ex) {
            LOGGER.debug(ex.getMessage(), ex);
            return "";
        }
    }

    @Override
    public List<CloudVmInstanceStatus> start(AuthenticatedContext ac, List<CloudResource> resources, List<CloudInstance> vms) {
        List<CloudVmInstanceStatus> statuses = new ArrayList<>();
        AmazonEC2Client amazonEC2Client = awsClient.createAccess(new AwsCredentialView(ac.getCloudCredential()),
                ac.getCloudContext().getLocation().getRegion().value());

        for (String group : getGroups(vms)) {
            Collection<String> instances = new ArrayList<>();
            Collection<CloudInstance> cloudInstances = new ArrayList<>();

            for (CloudInstance vm : vms) {
                if (vm.getTemplate().getGroupName().equals(group)) {
                    instances.add(vm.getInstanceId());
                    cloudInstances.add(vm);
                }
            }
            try {
                instances = removeInstanceIdsWhichAreNotInCorrectState(instances, amazonEC2Client, "Running");
                if (!instances.isEmpty()) {
                    amazonEC2Client.startInstances(new StartInstancesRequest().withInstanceIds(instances));
                }
                for (CloudInstance cloudInstance : cloudInstances) {
                    statuses.add(new CloudVmInstanceStatus(cloudInstance, InstanceStatus.IN_PROGRESS));
                }
            } catch (RuntimeException e) {
                LOGGER.error("Start instances failed on AWS", e);
                String message = e instanceof AmazonEC2Exception ? ((AmazonEC2Exception) e).getErrorCode() : e.getMessage();
                for (CloudInstance cloudInstance : cloudInstances) {
                    statuses.add(new CloudVmInstanceStatus(cloudInstance, InstanceStatus.FAILED, message));
                }
            }
        }
        return statuses;
    }

    @Override
    public List<CloudVmInstanceStatus> stop(AuthenticatedContext ac, List<CloudResource> resources, List<CloudInstance> vms) {
        List<CloudVmInstanceStatus> statuses = new ArrayList<>();
        AmazonEC2Client amazonEC2Client = awsClient.createAccess(new AwsCredentialView(ac.getCloudCredential()),
                ac.getCloudContext().getLocation().getRegion().value());

        for (String group : getGroups(vms)) {
            Collection<String> instances = new ArrayList<>();
            Collection<CloudInstance> cloudInstances = new ArrayList<>();

            for (CloudInstance vm : vms) {
                if (vm.getTemplate().getGroupName().equals(group)) {
                    instances.add(vm.getInstanceId());
                    cloudInstances.add(vm);
                }
            }
            try {
                instances = removeInstanceIdsWhichAreNotInCorrectState(instances, amazonEC2Client, "Stopped");
                if (!instances.isEmpty()) {
                    amazonEC2Client.stopInstances(new StopInstancesRequest().withInstanceIds(instances));
                }
                for (CloudInstance cloudInstance : cloudInstances) {
                    statuses.add(new CloudVmInstanceStatus(cloudInstance, InstanceStatus.IN_PROGRESS));
                }
            } catch (RuntimeException e) {
                LOGGER.error("Stop instances failed on AWS", e);
                String message = e instanceof AmazonEC2Exception ? ((AmazonEC2Exception) e).getErrorCode() : e.getMessage();
                for (CloudInstance cloudInstance : cloudInstances) {
                    statuses.add(new CloudVmInstanceStatus(cloudInstance, InstanceStatus.FAILED, message));
                }
            }
        }
        return statuses;
    }

    @Override
    public List<CloudVmInstanceStatus> check(AuthenticatedContext ac, List<CloudInstance> vms) {
        List<CloudVmInstanceStatus> cloudVmInstanceStatuses = new ArrayList<>();
        for (CloudInstance vm : vms) {
            String instanceId = vm.getInstanceId();
            try {
                String region = ac.getCloudContext().getLocation().getRegion().value();
                DescribeInstancesResult result = awsClient.createAccess(new AwsCredentialView(ac.getCloudCredential()),
                        ac.getCloudContext().getLocation().getRegion().value())
                        .describeInstances(new DescribeInstancesRequest().withInstanceIds(instanceId));
                for (Reservation reservation : result.getReservations()) {
                    for (Instance instance : reservation.getInstances()) {
                        if ("Stopped".equalsIgnoreCase(instance.getState().getName())) {
                            LOGGER.info("AWS instance [{}] is in {} state, region: {}, stack: {}",
                                    instance.getInstanceId(), instance.getState().getName(), region, ac.getCloudContext().getId());
                            cloudVmInstanceStatuses.add(new CloudVmInstanceStatus(vm, InstanceStatus.STOPPED));
                        } else if ("Running".equalsIgnoreCase(instance.getState().getName())) {
                            LOGGER.info("AWS instance [{}] is in {} state, region: {}, stack: {}",
                                    instance.getInstanceId(), instance.getState().getName(), region, ac.getCloudContext().getId());
                            cloudVmInstanceStatuses.add(new CloudVmInstanceStatus(vm, InstanceStatus.STARTED));
                        } else if ("Terminated".equalsIgnoreCase(instance.getState().getName())) {
                            LOGGER.info("AWS instance [{}] is in {} state, region: {}, stack: {}",
                                    instance.getInstanceId(), instance.getState().getName(), region, ac.getCloudContext().getId());
                            cloudVmInstanceStatuses.add(new CloudVmInstanceStatus(vm, InstanceStatus.TERMINATED));
                        } else {
                            LOGGER.info("AWS instance [{}] is in {} state, region: {}, stack: {}",
                                    instance.getInstanceId(), instance.getState().getName(), region, ac.getCloudContext().getId());
                            cloudVmInstanceStatuses.add(new CloudVmInstanceStatus(vm, InstanceStatus.IN_PROGRESS));
                        }
                    }
                }
            } catch (AmazonEC2Exception e) {
                if (e.getErrorCode().equalsIgnoreCase(INSTANCE_NOT_FOUND_ERROR_CODE)) {
                    LOGGER.info("Instance does not exist with this id: {}, mark as terminated.", instanceId);
                    cloudVmInstanceStatuses.add(new CloudVmInstanceStatus(vm, InstanceStatus.TERMINATED));
                } else {
                    LOGGER.warn("Instance could not be described with this id: {}, original message: {}", instanceId, e.getMessage());
                }
            }
        }
        return cloudVmInstanceStatuses;
    }

    private Collection<String> removeInstanceIdsWhichAreNotInCorrectState(Collection<String> instances, AmazonEC2 amazonEC2Client, String state) {
        DescribeInstancesResult describeInstances = amazonEC2Client.describeInstances(
                new DescribeInstancesRequest().withInstanceIds(instances));
        for (Reservation reservation : describeInstances.getReservations()) {
            for (Instance instance : reservation.getInstances()) {
                if (state.equalsIgnoreCase(instance.getState().getName())) {
                    instances.remove(instance.getInstanceId());
                }
            }
        }
        return instances;
    }

    private Collection<String> getGroups(Iterable<CloudInstance> vms) {
        Collection<String> groups = new HashSet<>();
        for (CloudInstance vm : vms) {
            if (!groups.contains(vm.getTemplate().getGroupName())) {
                groups.add(vm.getTemplate().getGroupName());
            }
        }
        return groups;
    }

}
