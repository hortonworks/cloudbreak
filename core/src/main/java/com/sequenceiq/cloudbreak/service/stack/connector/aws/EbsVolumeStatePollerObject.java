package com.sequenceiq.cloudbreak.service.stack.connector.aws;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.CreateVolumeResult;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.StackDependentPollerObject;

public class EbsVolumeStatePollerObject extends StackDependentPollerObject {

    private CreateVolumeResult volumeResult;
    private String volumeId;
    private AmazonEC2Client client;


    public EbsVolumeStatePollerObject(Stack stack, CreateVolumeResult volumeResult, String volumeId, AmazonEC2Client client) {
        super(stack);
        this.volumeResult = volumeResult;
        this.volumeId = volumeId;
        this.client = client;
    }

    public CreateVolumeResult getVolumeResult() {
        return volumeResult;
    }

    public String getVolumeId() {
        return volumeId;
    }

    public AmazonEC2Client getClient() {
        return client;
    }
}
