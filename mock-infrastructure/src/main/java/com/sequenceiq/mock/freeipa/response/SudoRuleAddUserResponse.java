package com.sequenceiq.mock.freeipa.response;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.freeipa.client.model.SudoRule;

@Component
public class SudoRuleAddUserResponse extends AbstractFreeIpaResponse<SudoRule> {

    @Override
    public String method() {
        return "sudorule_add_user";
    }

    @Override
    protected SudoRule handleInternal(List<CloudVmMetaDataStatus> metadatas, String body) {
        SudoRule sudoRule = new SudoRule();
        sudoRule.setHostCategory("all");
        return sudoRule;
    }
}
