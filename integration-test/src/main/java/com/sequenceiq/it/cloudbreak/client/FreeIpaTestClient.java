package com.sequenceiq.it.cloudbreak.client;

import java.util.List;
import java.util.Set;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetadataType;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.action.freeipa.FreeIpaAttachChildEnvironmentAction;
import com.sequenceiq.it.cloudbreak.action.freeipa.FreeIpaAttachRecipeAction;
import com.sequenceiq.it.cloudbreak.action.freeipa.FreeIpaChangeImageCatalogAction;
import com.sequenceiq.it.cloudbreak.action.freeipa.FreeIpaCheckVariantAction;
import com.sequenceiq.it.cloudbreak.action.freeipa.FreeIpaCollectDiagnosticsAction;
import com.sequenceiq.it.cloudbreak.action.freeipa.FreeIpaCreateAction;
import com.sequenceiq.it.cloudbreak.action.freeipa.FreeIpaDeleteAction;
import com.sequenceiq.it.cloudbreak.action.freeipa.FreeIpaDescribeAction;
import com.sequenceiq.it.cloudbreak.action.freeipa.FreeIpaDetachChildEnvironmentAction;
import com.sequenceiq.it.cloudbreak.action.freeipa.FreeIpaDetachRecipeAction;
import com.sequenceiq.it.cloudbreak.action.freeipa.FreeIpaDiskUpdateAction;
import com.sequenceiq.it.cloudbreak.action.freeipa.FreeIpaDownscaleAction;
import com.sequenceiq.it.cloudbreak.action.freeipa.FreeIpaFindGroupsAction;
import com.sequenceiq.it.cloudbreak.action.freeipa.FreeIpaFindUsersAction;
import com.sequenceiq.it.cloudbreak.action.freeipa.FreeIpaFindUsersInGroupAction;
import com.sequenceiq.it.cloudbreak.action.freeipa.FreeIpaGetHealthDetailsAction;
import com.sequenceiq.it.cloudbreak.action.freeipa.FreeIpaGetLastSyncOperationStatus;
import com.sequenceiq.it.cloudbreak.action.freeipa.FreeIpaRebuildAction;
import com.sequenceiq.it.cloudbreak.action.freeipa.FreeIpaRebuildv2Action;
import com.sequenceiq.it.cloudbreak.action.freeipa.FreeIpaRefreshAction;
import com.sequenceiq.it.cloudbreak.action.freeipa.FreeIpaRepairAction;
import com.sequenceiq.it.cloudbreak.action.freeipa.FreeIpaRotateSecretAction;
import com.sequenceiq.it.cloudbreak.action.freeipa.FreeIpaRotateSecretInternalAction;
import com.sequenceiq.it.cloudbreak.action.freeipa.FreeIpaSetPasswordAction;
import com.sequenceiq.it.cloudbreak.action.freeipa.FreeIpaStartAction;
import com.sequenceiq.it.cloudbreak.action.freeipa.FreeIpaStopAction;
import com.sequenceiq.it.cloudbreak.action.freeipa.FreeIpaSynchronizeAllUsersAction;
import com.sequenceiq.it.cloudbreak.action.freeipa.FreeIpaSynchronizeAllUsersInternalAction;
import com.sequenceiq.it.cloudbreak.action.freeipa.FreeIpaTrustCleanupCommandsAction;
import com.sequenceiq.it.cloudbreak.action.freeipa.FreeIpaUpgradeAction;
import com.sequenceiq.it.cloudbreak.action.freeipa.FreeIpaUpscaleAction;
import com.sequenceiq.it.cloudbreak.action.freeipa.FreeipaInstanceMetadataUpdateAction;
import com.sequenceiq.it.cloudbreak.action.freeipa.FreeipaRotateSaltPasswordAction;
import com.sequenceiq.it.cloudbreak.action.freeipa.FreeipaUsedImagesAction;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaChildEnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaDiagnosticsTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaDownscaleTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaHealthDetailsDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaRotationTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTrustCommandsDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaUpscaleTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaUserSyncTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeipaChangeImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeipaUsedImagesTestDto;
import com.sequenceiq.it.cloudbreak.microservice.FreeIpaClient;

@Service
public class FreeIpaTestClient {

    @Inject
    private EnvironmentPublicApiTestClient environmentPublicApiTestClient;

    public Action<FreeIpaTestDto, FreeIpaClient> create() {
        return new FreeIpaCreateAction();
    }

    public Action<FreeIpaTestDto, FreeIpaClient> delete() {
        return new FreeIpaDeleteAction();
    }

    public Action<FreeIpaChildEnvironmentTestDto, FreeIpaClient> attachChildEnvironment() {
        return new FreeIpaAttachChildEnvironmentAction();
    }

    public Action<FreeIpaChildEnvironmentTestDto, FreeIpaClient> detachChildEnvironment() {
        return new FreeIpaDetachChildEnvironmentAction();
    }

    public Action<FreeIpaTestDto, FreeIpaClient> describe() {
        return new FreeIpaDescribeAction();
    }

    public Action<FreeIpaTestDto, FreeIpaClient> refresh() {
        return new FreeIpaRefreshAction();
    }

    public Action<FreeIpaTestDto, FreeIpaClient> start() {
        return new FreeIpaStartAction();
    }

    public Action<FreeIpaTestDto, FreeIpaClient> stop() {
        return new FreeIpaStopAction();
    }

    public Action<FreeIpaTestDto, FreeIpaClient> checkVariant(String variant) {
        return new FreeIpaCheckVariantAction(variant);
    }

    public Action<FreeIpaUserSyncTestDto, FreeIpaClient> syncAllInternal() {
        return new FreeIpaSynchronizeAllUsersInternalAction();
    }

    public Action<FreeIpaUserSyncTestDto, FreeIpaClient> getLastSyncOperationStatus() {
        return new FreeIpaGetLastSyncOperationStatus(environmentPublicApiTestClient);
    }

    public Action<FreeIpaUserSyncTestDto, FreeIpaClient> setPassword(Set<String> environmentCrns, String newPassword) {
        return new FreeIpaSetPasswordAction(environmentCrns, newPassword);
    }

    public Action<FreeIpaUserSyncTestDto, FreeIpaClient> syncAll() {
        return new FreeIpaSynchronizeAllUsersAction();
    }

    public Action<FreeIpaTestDto, FreeIpaClient> repair(InstanceMetadataType instanceMetadataType) {
        return new FreeIpaRepairAction(instanceMetadataType);
    }

    public Action<FreeIpaTestDto, FreeIpaClient> attachRecipes(List<String> recipes) {
        return new FreeIpaAttachRecipeAction(recipes);
    }

    public Action<FreeIpaTestDto, FreeIpaClient> detachRecipes(List<String> recipes) {
        return new FreeIpaDetachRecipeAction(recipes);
    }

    public Action<FreeIpaTestDto, FreeIpaClient> rebuild() {
        return new FreeIpaRebuildAction();
    }

    public Action<FreeIpaTestDto, FreeIpaClient> rebuildv2() {
        return new FreeIpaRebuildv2Action();
    }

    public Action<FreeIpaDiagnosticsTestDto, FreeIpaClient> collectDiagnostics() {
        return new FreeIpaCollectDiagnosticsAction();
    }

    public Action<FreeipaUsedImagesTestDto, FreeIpaClient> usedImages() {
        return new FreeipaUsedImagesAction();
    }

    public Action<FreeIpaTestDto, FreeIpaClient> findUsers(Set<String> users, boolean expectedPresence) {
        return new FreeIpaFindUsersAction(users, expectedPresence);
    }

    public Action<FreeIpaTestDto, FreeIpaClient> findGroups(Set<String> groups) {
        return new FreeIpaFindGroupsAction(groups);
    }

    public Action<FreeIpaTestDto, FreeIpaClient> findUsersInGroup(Set<String> users, String group, boolean expectedPresence) {
        return new FreeIpaFindUsersInGroupAction(users, group, expectedPresence);
    }

    public Action<FreeIpaTestDto, FreeIpaClient> upgrade() {
        return new FreeIpaUpgradeAction();
    }

    public Action<FreeIpaTestDto, FreeIpaClient> upgrade(String imageCatalogUrl, String imageId) {
        return new FreeIpaUpgradeAction(imageCatalogUrl, imageId);
    }

    public Action<FreeipaChangeImageCatalogTestDto, FreeIpaClient> changeImageCatalog() {
        return new FreeIpaChangeImageCatalogAction();
    }

    public Action<FreeIpaUpscaleTestDto, FreeIpaClient> upscale() {
        return new FreeIpaUpscaleAction();
    }

    public Action<FreeIpaDownscaleTestDto, FreeIpaClient> downscale() {
        return new FreeIpaDownscaleAction();
    }

    public Action<FreeIpaTestDto, FreeIpaClient> rotateSaltPassword() {
        return new FreeipaRotateSaltPasswordAction();
    }

    public Action<FreeIpaRotationTestDto, FreeIpaClient> rotateSecret() {
        return new FreeIpaRotateSecretAction();
    }

    public Action<FreeIpaRotationTestDto, FreeIpaClient> rotateSecretInternal() {
        return new FreeIpaRotateSecretInternalAction();
    }

    public Action<FreeIpaHealthDetailsDto, FreeIpaClient> getHealthDetails() {
        return new FreeIpaGetHealthDetailsAction();
    }

    public Action<FreeIpaTestDto, FreeIpaClient> instanceMetadataUpdate() {
        return new FreeipaInstanceMetadataUpdateAction();
    }

    public Action<FreeIpaTestDto, FreeIpaClient> updateDisks(int size, String volumeType) {
        return new FreeIpaDiskUpdateAction(size, volumeType);
    }

    public Action<FreeIpaTrustCommandsDto, FreeIpaClient> trustCleanupCommands() {
        return new FreeIpaTrustCleanupCommandsAction();
    }
}
