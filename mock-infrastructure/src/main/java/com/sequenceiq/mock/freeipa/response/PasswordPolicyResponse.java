package com.sequenceiq.mock.freeipa.response;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.freeipa.client.model.PasswordPolicy;

@Component
public class PasswordPolicyResponse extends AbstractFreeIpaResponse<PasswordPolicy> {

    @Override
    public String method() {
        return "pwpolicy_show";
    }

    @Override
    protected PasswordPolicy handleInternal(List<CloudVmMetaDataStatus> metadatas, String body) {
        PasswordPolicy passwordPolicy = new PasswordPolicy();
        passwordPolicy.setKrbmaxpwdlife(1);
        passwordPolicy.setKrbpwdfailurecountinterval(1);
        passwordPolicy.setKrbpwdlockoutduration(1);
        passwordPolicy.setKrbpwdmaxfailure(1);
        passwordPolicy.setKrbpwdminlength(1);
        passwordPolicy.setKrbpwdmindiffchars(1);
        return passwordPolicy;
    }
}
