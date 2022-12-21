package com.sequenceiq.freeipa.client.operation;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.freeipa.client.model.SudoRule;

public class SudoRuleAddAllowCommandOperation extends AbstractFreeIpaAddOperation<SudoRule> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SudoRuleAddAllowCommandOperation.class);

    private final String command;

    private SudoRuleAddAllowCommandOperation(String name, String command) {
        super(name, SudoRule.class);
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
    protected Logger getLogger() {
        return LOGGER;
    }
}
