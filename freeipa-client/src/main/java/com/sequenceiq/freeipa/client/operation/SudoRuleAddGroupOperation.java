package com.sequenceiq.freeipa.client.operation;

import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.model.SudoRule;

public class SudoRuleAddGroupOperation extends AbstractFreeIpaAddOperation<SudoRule> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SudoRuleAddGroupOperation.class);

    private final String group;

    private SudoRuleAddGroupOperation(String name, String group) {
        super(name);
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
    public Optional<SudoRule> invoke(FreeIpaClient freeIpaClient) throws FreeIpaClientException {
        return invokeAdd(freeIpaClient, SudoRule.class);
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }
}
