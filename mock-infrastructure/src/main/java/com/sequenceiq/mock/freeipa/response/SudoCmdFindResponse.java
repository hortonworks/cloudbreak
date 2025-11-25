package com.sequenceiq.mock.freeipa.response;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.freeipa.client.model.SudoCommand;

@Component
public class SudoCmdFindResponse extends AbstractFreeIpaResponse<Set<SudoCommand>> {

    @Override
    public String method() {
        return "sudocmd_find";
    }

    @Override
    protected Set<SudoCommand> handleInternal(List<CloudVmMetaDataStatus> metadatas, String body) {
        SudoCommand sudoCommand = new SudoCommand();
        sudoCommand.setSudocmd("all");
        return Set.of(sudoCommand);
    }
}
