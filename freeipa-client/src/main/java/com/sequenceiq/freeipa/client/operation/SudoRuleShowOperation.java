package com.sequenceiq.freeipa.client.operation;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.FreeIpaClientExceptionUtil;
import com.sequenceiq.freeipa.client.model.SudoRule;

public class SudoRuleShowOperation extends AbstractFreeipaOperation<SudoRule> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SudoRuleShowOperation.class);

    private final String ruleName;

    private SudoRuleShowOperation(String ruleName) {
        this.ruleName = ruleName;
    }

    public static SudoRuleShowOperation create(String ruleName) {
        return new SudoRuleShowOperation(ruleName);
    }

    @Override
    public String getOperationName() {
        return "sudorule_show";
    }

    @Override
    protected List<Object> getFlags() {
        return List.of(ruleName);
    }

    @Override
    public Optional<SudoRule> invoke(FreeIpaClient freeipaClient) throws FreeIpaClientException {
        try {
            LOGGER.debug("Show '{}'", ruleName);
            SudoRule sudoRule = invoke(freeipaClient, SudoRule.class);
            LOGGER.debug("Success: '{}'", sudoRule);
            return Optional.ofNullable(sudoRule);
        } catch (FreeIpaClientException ex) {
            if (FreeIpaClientExceptionUtil.isNotFoundException(ex)) {
                LOGGER.debug("Not found '{}'", ruleName);
                return Optional.empty();
            }
            LOGGER.error("Failed to show '{}': ", ruleName, ex);
            throw ex;
        }
    }
}
