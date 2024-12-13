package com.sequenceiq.cloudbreak.cloud.template.init;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;

@Service
public class SshKeyNameGenerator {

    public String generateKeyPairName(AuthenticatedContext ac) {
        CloudCredential cloudCredential = ac.getCloudCredential();
        CloudContext cloudContext = ac.getCloudContext();

        return String.format("%s%s%s%s",
                cloudCredential.getName(),
                cloudCredential.getId(),
                cloudContext.getName(),
                cloudContext.getId());
    }

    public boolean hasPublicKeyId(CloudStack stack) {
        return StringUtils.isNotBlank(stack.getInstanceAuthentication().getPublicKeyId());
    }

    public boolean mustUploadPublicKey(CloudStack stack) {
        return !hasPublicKeyId(stack);
    }

    public String getKeyPairName(AuthenticatedContext ac, CloudStack stack) {
        return hasPublicKeyId(stack)
                ? stack.getInstanceAuthentication().getPublicKeyId()
                : generateKeyPairName(ac);
    }
}
