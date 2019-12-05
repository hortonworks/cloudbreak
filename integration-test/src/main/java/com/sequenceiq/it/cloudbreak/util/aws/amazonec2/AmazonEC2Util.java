package com.sequenceiq.it.cloudbreak.util.aws.amazonec2;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.it.cloudbreak.util.aws.amazonec2.action.EC2ClientActions;

@Component
public class AmazonEC2Util {
    @Inject
    private EC2ClientActions ec2ClientActions;

    private AmazonEC2Util() {
    }

    public List<String> listInstanceVolumeIds(List<String> instanceIds) {
        return ec2ClientActions.getInstanceVolumeIds(instanceIds);
    }

    public void deleteHostGroupInstances(List<String> instanceIds) {
        ec2ClientActions.deleteHostGroupInstances(instanceIds);
    }

    public void stopHostGroupInstances(List<String> instanceIds) {
        ec2ClientActions.stopHostGroupInstances(instanceIds);
    }
}
