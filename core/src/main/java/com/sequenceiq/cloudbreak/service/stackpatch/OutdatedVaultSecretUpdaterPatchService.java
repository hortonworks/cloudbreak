package com.sequenceiq.cloudbreak.service.stackpatch;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackPatchType;

@Component
public class OutdatedVaultSecretUpdaterPatchService extends ExistingStackPatchService {

    @Value("${existing-stack-patcher.patch-configs.update-outdated-vault-secrets.intervalMinutes}")
    private int intervalInMinutes;

    @Override
    public int getIntervalInMinutes() {
        return intervalInMinutes;
    }

    @Override
    public StackPatchType getStackPatchType() {
        return StackPatchType.UPDATE_OUTDATED_VAULT_SECRETS;
    }

    @Override
    public boolean isAffected(Stack stack) {
        return false;
    }

    @Override
    boolean doApply(Stack stack) throws ExistingStackPatchApplyException {
        return true;
    }
}
