package com.sequenceiq.freeipa.controller;

import java.util.List;

import jakarta.inject.Inject;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.AccountIdNotNeeded;
import com.sequenceiq.authorization.annotation.InternalOnly;
import com.sequenceiq.cloudbreak.auth.security.internal.AccountId;
import com.sequenceiq.freeipa.api.v1.util.UtilV1Endpoint;
import com.sequenceiq.freeipa.api.v1.util.model.UsedImagesListV1Response;
import com.sequenceiq.freeipa.service.UsedImagesProvider;
import com.sequenceiq.freeipa.service.recipe.FreeIpaRecipeService;

@Controller
public class UtilV1Controller implements UtilV1Endpoint {

    @Inject
    private UsedImagesProvider usedImagesProvider;

    @Inject
    private FreeIpaRecipeService freeIpaRecipeService;

    @Override
    @InternalOnly
    @AccountIdNotNeeded
    public UsedImagesListV1Response usedImages(Integer thresholdInDays) {
        return usedImagesProvider.getUsedImages(thresholdInDays);
    }

    @Override
    @InternalOnly
    public List<String> usedRecipes(@AccountId String accountId) {
        return freeIpaRecipeService.getUsedRecipeNamesForAccount(accountId);
    }
}
