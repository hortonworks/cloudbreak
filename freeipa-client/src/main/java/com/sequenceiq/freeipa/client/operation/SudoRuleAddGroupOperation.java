package com.sequenceiq.freeipa.client.operation;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.freeipa.client.model.SudoRule;

public class SudoRuleAddGroupOperation extends AbstractFreeIpaAddOperation<SudoRule> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SudoRuleAddGroupOperation.class);

    private final String group;

    private SudoRuleAddGroupOperation(String name, String group) {
        super(name, SudoRule.class);
        this.group = group;
    }

    public static SudoRuleAddGroupOperation create(String name, String group) {
        return new SudoRuleAddGroupOperation(name, group);
    }

    @Override
    public String getOperationName() {
        return "sudorule_add_user";
    }

    @Override
    protected Map<String, Object> getParams() {
        return Map.of("group", group);
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }
}
