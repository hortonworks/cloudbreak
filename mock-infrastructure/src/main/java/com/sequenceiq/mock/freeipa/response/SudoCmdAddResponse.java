package com.sequenceiq.mock.freeipa.response;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.freeipa.client.model.SudoCommand;

@Component
public class SudoCmdAddResponse extends AbstractFreeIpaResponse<SudoCommand> {

    @Override
    public String method() {
        return "sudocmd_add";
    }

    @Override
    protected SudoCommand handleInternal(List<CloudVmMetaDataStatus> metadatas, String body) {
        SudoCommand sudoCommand = new SudoCommand();
        sudoCommand.setSudocmd("all");
        return sudoCommand;
    }
}
