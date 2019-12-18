package com.sequenceiq.it.cloudbreak.testcase.e2e.aws;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.it.cloudbreak.testcase.e2e.CloudFunctionality;
import com.sequenceiq.it.cloudbreak.util.aws.amazonec2.AmazonEC2Util;
import com.sequenceiq.it.cloudbreak.util.aws.amazons3.AmazonS3Util;

@Component
public class AwsCloudFunctionality implements CloudFunctionality {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsCloudFunctionality.class);

    @Inject
    private AmazonEC2Util amazonEC2Util;

    @Inject
    private AmazonS3Util amazonS3Util;

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

    @Override
    public void cloudStorageInitialize() {
        LOGGER.debug("cloudStorageInitialize: nothing to do for AWS");
    }

    @Override
    public void cloudStorageListContainer(String baseLocation) {
        amazonS3Util.listBucket(baseLocation);
    }

    @Override
    public void cloudStorageDeleteContainer(String baseLocation) {
        amazonS3Util.deleteNonVersionedBucket(baseLocation);
    }
}
