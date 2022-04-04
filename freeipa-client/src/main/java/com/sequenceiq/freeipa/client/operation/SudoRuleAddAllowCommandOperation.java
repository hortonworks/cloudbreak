package com.sequenceiq.freeipa.client.operation;

import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.model.SudoRule;

public class SudoRuleAddAllowCommandOperation extends AbstractFreeIpaAddOperation<SudoRule> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SudoRuleAddAllowCommandOperation.class);

    private final String command;

    private SudoRuleAddAllowCommandOperation(String name, String command) {
        super(name);
        this.command = command;
    }

    public static SudoRuleAddAllowCommandOperation create(String name, String command) {
        return new SudoRuleAddAllowCommandOperation(name, command);
    }

    @Override
    public String getOperationName() {
        return "sudorule_add_allow_command";
    }

    @Override
    protected Map<String, Object> getParams() {
        return Map.of("sudocmd", command);
    }

    @Override
    public Optional<SudoRule> invoke(FreeIpaClient freeIpaClient) throws FreeIpaClientException {
        return invokeAdd(freeIpaClient, SudoRule.class);
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }
}
