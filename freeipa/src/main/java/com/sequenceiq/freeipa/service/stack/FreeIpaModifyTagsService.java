package com.sequenceiq.freeipa.service.stack;

import java.util.Map;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.freeipa.entity.Stack;

@Service
public class FreeIpaModifyTagsService {

    @Inject
    private StackService stackService;

    @Inject
    private StackUpdater stackUpdater;

    public void modifyUserDefinedTags(String environmentCrn, Map<String, String> userDefinedTags, String accountId) {
        Stack stack = stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(environmentCrn, accountId);
        stackUpdater.updateUserDefinedTags(stack, userDefinedTags);
    }
}