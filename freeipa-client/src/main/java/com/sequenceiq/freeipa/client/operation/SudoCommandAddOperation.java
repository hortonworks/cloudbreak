package com.sequenceiq.freeipa.client.operation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.freeipa.client.model.SudoCommand;

public class SudoCommandAddOperation extends AbstractFreeIpaAddOperation<SudoCommand> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SudoCommandAddOperation.class);

    private SudoCommandAddOperation(String name) {
        super(name, SudoCommand.class);
    }

    public static SudoCommandAddOperation create(String name) {
        return new SudoCommandAddOperation(name);
    }

    @Override
    public String getOperationName() {
        return "sudocmd_add";
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }
}
