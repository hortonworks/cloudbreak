package com.sequenceiq.freeipa.client.operation;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.model.SudoCommand;

public class SudoCommandAddOperation extends AbstractFreeIpaAddOperation<SudoCommand> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SudoCommandAddOperation.class);

    private SudoCommandAddOperation(String name) {
        super(name);
    }

    public static SudoCommandAddOperation create(String name) {
        return new SudoCommandAddOperation(name);
    }

    @Override
    public String getOperationName() {
        return "sudocmd_add";
    }

    @Override
    public Optional<SudoCommand> invoke(FreeIpaClient freeIpaClient) throws FreeIpaClientException {
        return invokeAdd(freeIpaClient, SudoCommand.class);
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }
}
