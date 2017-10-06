package com.sequenceiq.cloudbreak.cloud.aws.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeVolumesRequest;
import com.amazonaws.services.ec2.model.DescribeVolumesResult;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.task.PollBooleanStateTask;

@Component(EbsVolumeStatusCheckerTask.NAME)
@Scope(value = "prototype")
public class EbsVolumeStatusCheckerTask extends PollBooleanStateTask {
    public static final String NAME = "ebsVolumeStatusCheckerTask";

    private static final Logger LOGGER = LoggerFactory.getLogger(EbsVolumeStatusCheckerTask.class);

    private Group group;

    private String volumeId;

    private AmazonEC2Client amazonEC2Client;

    public EbsVolumeStatusCheckerTask(AuthenticatedContext authenticatedContext, Group group, AmazonEC2Client amazonEC2Client, String volumeId) {
        super(authenticatedContext, true);
        this.volumeId = volumeId;
        this.group = group;
        this.amazonEC2Client = amazonEC2Client;
    }

    @Override
    protected Boolean doCall() throws Exception {
        LOGGER.info("Checking if AWS EBS volume '{}' is created.", volumeId);
        DescribeVolumesResult describeVolumesResult = amazonEC2Client.describeVolumes(new DescribeVolumesRequest().withVolumeIds(volumeId));
        return describeVolumesResult.getVolumes() != null && !describeVolumesResult.getVolumes().isEmpty()
                && "available".equals(describeVolumesResult.getVolumes().get(0).getState());
    }
}