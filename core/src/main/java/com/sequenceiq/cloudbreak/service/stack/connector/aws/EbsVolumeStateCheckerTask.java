package com.sequenceiq.cloudbreak.service.stack.connector.aws;

import org.springframework.stereotype.Component;

import com.amazonaws.services.ec2.model.DescribeVolumesRequest;
import com.amazonaws.services.ec2.model.DescribeVolumesResult;
import com.sequenceiq.cloudbreak.service.StackBasedStatusCheckerTask;

@Component
public class EbsVolumeStateCheckerTask extends StackBasedStatusCheckerTask<EbsVolumeStatePollerObject> {

    @Override
    public boolean checkStatus(EbsVolumeStatePollerObject object) {
        DescribeVolumesResult describeVolumesResult = object.getClient().describeVolumes(new DescribeVolumesRequest().withVolumeIds(object.getVolumeId()));
        if (describeVolumesResult.getVolumes() != null && !describeVolumesResult.getVolumes().isEmpty()) {
            return "available".equals(describeVolumesResult.getVolumes().get(0).getState()) ? true : false;
        }
        return false;
    }

    @Override
    public void handleTimeout(EbsVolumeStatePollerObject ebsVolumeStatePollerObject) {
        throw new AwsResourceException(String.format(
                "AWS Ebs volume creation didn't reach the desired state in the given time frame, stack id: %s, name %s",
                ebsVolumeStatePollerObject.getStack().getId(), ebsVolumeStatePollerObject.getStack().getName()));
    }

    @Override
    public String successMessage(EbsVolumeStatePollerObject ebsVolumeStatePollerObject) {
        return String.format("AWS Ebs Volume creation success on  stack(%s)", ebsVolumeStatePollerObject.getStack().getId());
    }
}