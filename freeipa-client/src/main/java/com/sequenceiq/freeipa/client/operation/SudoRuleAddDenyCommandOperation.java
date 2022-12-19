package com.sequenceiq.freeipa.client.operation;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.freeipa.client.model.SudoRule;

public class SudoRuleAddDenyCommandOperation  extends AbstractFreeIpaAddOperation<SudoRule> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SudoRuleAddDenyCommandOperation.class);

    private final String command;

    private SudoRuleAddDenyCommandOperation(String name, String command) {
        super(name, SudoRule.class);
        this.command = command;
    }

    public static SudoRuleAddDenyCommandOperation create(String name, String command) {
        return new SudoRuleAddDenyCommandOperation(name, command);
    }

    @Override
    public String getOperationName() {
        return "sudorule_add_deny_command";
    }

    @Override
    protected Map<String, Object> getParams() {
        return Map.of("sudocmd", command);
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }
}
