package com.sequenceiq.freeipa.client.operation;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.freeipa.client.model.SudoRule;

public class SudoRuleAddOperation extends AbstractFreeIpaAddOperation<SudoRule> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SudoRuleAddDenyCommandOperation.class);

    private static final Map<String, Object> HOST_CATEGORY_ALL = Map.of("hostcategory", "all");

    private final boolean hostCategoryAll;

    private SudoRuleAddOperation(String name, boolean hostCategoryAll, AbstractFreeipaOperation<SudoRule> getOperation) {
        super(name, getOperation, SudoRule.class);
        this.hostCategoryAll = hostCategoryAll;
    }

    public static SudoRuleAddOperation create(String name, boolean hostCategoryAll, AbstractFreeipaOperation<SudoRule> getOperation) {
        return new SudoRuleAddOperation(name, hostCategoryAll, getOperation);
    }

    @Override
    public String getOperationName() {
        return "sudorule_add";
    }

    @Override
    protected Map<String, Object> getParams() {
        return hostCategoryAll ? HOST_CATEGORY_ALL : Map.of();
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }
}
