package com.sequenceiq.cloudbreak.usage.service;

import java.util.Arrays;
import java.util.Optional;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.usage.UsageReporter;

@Service
public class RecipeUsageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecipeUsageService.class);

    @Inject
    private UsageReporter usageReporter;

    public void sendCreatedUsageReport(String recipeName, String recipeCrn, String recipeType) {
        try {
            usageReporter.cdpRecipeEvent(getRecipeEventBuilder(recipeName, recipeCrn, recipeType)
                    .setStatus(UsageProto.CDPRecipeStatus.Value.CREATED)
                    .build());
        } catch (Exception e) {
            LOGGER.error(String.format("Couldn't send usage report about recipe [name: %s] creation because: ", recipeName), e);
        }
    }

    public void sendDeletedUsageReport(String recipeName, String recipeCrn, String recipeType) {
        try {
            usageReporter.cdpRecipeEvent(getRecipeEventBuilder(recipeName, recipeCrn, recipeType)
                    .setStatus(UsageProto.CDPRecipeStatus.Value.DELETED)
                    .build());
        } catch (Exception e) {
            LOGGER.error(String.format("Couldn't send usage report about recipe [name: %s] deletion because: ", recipeName), e);
        }
    }

    public void sendAttachedUsageReport(String recipeName, Optional<String> recipeCrn, Optional<String> recipeType,
            String stackCrn, Optional<String> hostGroupName) {
        try {
            usageReporter.cdpRecipeEvent(getRecipeEventBuilder(recipeName, recipeCrn, recipeType, stackCrn, hostGroupName)
                    .setStatus(UsageProto.CDPRecipeStatus.Value.ATTACHED)
                    .build());
        } catch (Exception e) {
            LOGGER.error(String.format("Couldn't send usage report about recipe [name: %s] attachment because: ", recipeName), e);
        }
    }

    public void sendDetachedUsageReport(String recipeName, Optional<String> recipeCrn, Optional<String> recipeType,
            String stackCrn, Optional<String> hostGroupName) {
        try {
            usageReporter.cdpRecipeEvent(getRecipeEventBuilder(recipeName, recipeCrn, recipeType, stackCrn, hostGroupName)
                    .setStatus(UsageProto.CDPRecipeStatus.Value.DETACHED)
                    .build());
        } catch (Exception e) {
            LOGGER.error(String.format("Couldn't send usage report about recipe [name: %s] detachment because: ", recipeName), e);
        }
    }

    public void sendClusterCreationRecipeUsageReport(String stackCrn, int recipeCount, Optional<String> typeDetails, Optional<String> hostGroupDetails) {
        try {
            usageReporter.cdpClusterCreationRecipeEvent(UsageProto.CDPClusterCreationRecipeEvent.newBuilder()
                    .setStackCrn(stackCrn)
                    .setRecipeCount(recipeCount)
                    .setHostGroupDetails(hostGroupDetails.orElse(""))
                    .setTypeDetails(typeDetails.orElse(""))
                    .setAccountId(ThreadBasedUserCrnProvider.getAccountId())
                    .build());
        } catch (Exception e) {
            LOGGER.error(String.format("Couldn't send recipe related usage report about cluster creation [stack CRN: %s], because: ", stackCrn), e);
        }
    }

    private static UsageProto.CDPRecipeType.Value getRecipeType(String recipeType) {
        return Arrays.stream(UsageProto.CDPRecipeType.Value.values())
                .filter(value -> StringUtils.equals(value.name(), recipeType))
                .findFirst()
                .orElse(UsageProto.CDPRecipeType.Value.UNKNOWN);
    }

    private static UsageProto.CDPRecipeEvent.Builder getRecipeEventBuilder(String recipeName, String recipeCrn, String recipeType) {
        return UsageProto.CDPRecipeEvent.newBuilder()
                .setAccountId(ThreadBasedUserCrnProvider.getAccountId())
                .setName(recipeName)
                .setRecipeCrn(recipeCrn)
                .setType(getRecipeType(recipeType));
    }

    private static UsageProto.CDPRecipeEvent.Builder getRecipeEventBuilder(String recipeName, Optional<String> recipeCrn,
            Optional<String> recipeType, String stackCrn, Optional<String> hostGroupName) {
        return UsageProto.CDPRecipeEvent.newBuilder()
                .setAccountId(ThreadBasedUserCrnProvider.getAccountId())
                .setName(recipeName)
                .setRecipeCrn(recipeCrn.orElse(""))
                .setType(getRecipeType(recipeType.orElse("")))
                .setStackCrn(stackCrn)
                .setHostGroup(hostGroupName.orElse(""));
    }
}
