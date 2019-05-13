package com.sequenceiq.environment.credential;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.credential.InitCodeGrantFlowRequest;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;

@Component
public class RequestProvider {

    public InitCodeGrantFlowRequest getInitCodeGrantFlowRequest(CloudContext cloudContext, CloudCredential cloudCredential) {
        return new InitCodeGrantFlowRequest(cloudContext, cloudCredential);
    }

}
