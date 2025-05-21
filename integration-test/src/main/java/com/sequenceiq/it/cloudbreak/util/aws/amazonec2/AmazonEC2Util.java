package com.sequenceiq.it.cloudbreak.util.aws.amazonec2;

import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.it.cloudbreak.util.aws.amazonec2.action.EC2ClientActions;

import software.amazon.awssdk.services.cloudformation.model.Stack;

@Component
public class AmazonEC2Util {
    @Inject
    private EC2ClientActions ec2ClientActions;

    private AmazonEC2Util() {
    }

    public List<String> listInstancesVolumeIds(List<String> instanceIds) {
        return ec2ClientActions.getSelectedInstancesVolumeIds(instanceIds, false);
    }

    public Map<String, Set<String>> listInstanceVolumeIds(String instanceId) {
        return ec2ClientActions.getInstanceVolumeIds(instanceId, false);
    }

    public List<String> listInstanceTypes(List<String> instanceIds) {
        return ec2ClientActions.listInstanceTypes(instanceIds);
    }

    public List<String> listVolumeKmsKeyIds(List<String> instanceIds) {
        return ec2ClientActions.getRootVolumesKmsKeys(instanceIds);
    }

    public Map<String, Map<String, String>> listTagsByInstanceId(List<String> instanceIds) {
        return ec2ClientActions.listTagsByInstanceId(instanceIds);
    }

    public void deleteHostGroupInstances(List<String> instanceIds) {
        ec2ClientActions.deleteHostGroupInstances(instanceIds);
    }

    public void stopHostGroupInstances(List<String> instanceIds) {
        ec2ClientActions.stopHostGroupInstances(instanceIds);
    }

    public Map<String, Boolean> enaSupport(List<String> instanceIds) {
        return ec2ClientActions.enaSupport(instanceIds);
    }

    public Map<String, String> instanceSubnet(List<String> instanceIds) {
        return ec2ClientActions.instanceSubnet(instanceIds);
    }

    public Map<String, String> listLaunchTemplatesUserData(String name) {
        return ec2ClientActions.listLaunchTemplatesUserData(name);
    }

    public Boolean isCloudFormationExistForStack(String name) {
        return ec2ClientActions.isCloudFormationExistForStack(name);
    }

    public List<Stack> listStacksByEnvironmentCrn(String crn) {
        return ec2ClientActions.listCfStacksByEnvironment(crn);
    }

    public List<com.sequenceiq.cloudbreak.cloud.model.Volume> describeVolumes(List<String> volumeIds) {
        return ec2ClientActions.describeVolumes(volumeIds);
    }

    public List<String> listInstancesRootVolumeIds(List<String> instanceIds) {
        return ec2ClientActions.getSelectedInstancesVolumeIds(instanceIds, true);
    }

}
