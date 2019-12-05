package com.sequenceiq.it.cloudbreak.testcase.e2e.aws;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.it.cloudbreak.testcase.e2e.CloudFunctionality;
import com.sequenceiq.it.cloudbreak.util.aws.amazonec2.AmazonEC2Util;

@Component
public class AwsCloudFunctionality implements CloudFunctionality {

    @Inject
    private AmazonEC2Util amazonEC2Util;

    @Override
    public List<String> listInstanceVolumeIds(List<String> instanceIds) {
        return amazonEC2Util.listInstanceVolumeIds(instanceIds);
    }

    @Override
    public void deleteInstances(List<String> instanceIds) {
        amazonEC2Util.deleteHostGroupInstances(instanceIds);
    }

    @Override
    public void stopInstances(List<String> instanceIds) {
        amazonEC2Util.stopHostGroupInstances(instanceIds);
    }
}
