package com.sequenceiq.cloudbreak.cloud.aws.task;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeVolumesRequest;
import com.amazonaws.services.ec2.model.DescribeVolumesResult;
import com.amazonaws.services.ec2.model.Volume;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.task.PollBooleanStateTask;

@Component(EbsVolumeStatusCheckerTask.NAME)
@Scope("prototype")
public class EbsVolumeStatusCheckerTask extends PollBooleanStateTask {
    public static final String NAME = "ebsVolumeStatusCheckerTask";

    private static final Logger LOGGER = LoggerFactory.getLogger(EbsVolumeStatusCheckerTask.class);

    private final String volumeId;

    private final AmazonEC2Client amazonEC2Client;

    public EbsVolumeStatusCheckerTask(AuthenticatedContext authenticatedContext, AmazonEC2Client amazonEC2Client, String volumeId) {
        super(authenticatedContext, true);
        this.volumeId = volumeId;
        this.amazonEC2Client = amazonEC2Client;
    }

    @Override
    protected Boolean doCall() {
        LOGGER.debug("Checking if AWS EBS volume '{}' is created.", volumeId);
        DescribeVolumesResult describeVolumesResult = amazonEC2Client.describeVolumes(new DescribeVolumesRequest().withVolumeIds(volumeId));
        Optional<Volume> volume = describeVolumesResult.getVolumes().stream().findFirst();
        return volume.isPresent() && "available".equals(volume.get().getState());
    }
}