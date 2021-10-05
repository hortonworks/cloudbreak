package com.sequenceiq.datalake.controller.sdx;

import javax.inject.Inject;
import javax.validation.Valid;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceName;
import com.sequenceiq.authorization.annotation.ResourceCrn;
import com.sequenceiq.authorization.annotation.ResourceName;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.recipe.AttachRecipeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.recipe.DetachRecipeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.recipe.UpdateRecipesV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.recipe.AttachRecipeV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.recipe.DetachRecipeV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.recipe.UpdateRecipesV4Response;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.security.internal.TenantAwareParam;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.structuredevent.rest.annotation.AccountEntityType;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.sdx.api.endpoint.SdxRecipeEndpoint;

@Controller
@AccountEntityType(SdxCluster.class)
public class SdxRecipeController implements SdxRecipeEndpoint {

    @Inject
    private SdxService sdxService;

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.REFRESH_RECIPES_DATALAKE)
    public UpdateRecipesV4Response refreshRecipesByCrn(@TenantAwareParam @ResourceCrn String crn, @Valid UpdateRecipesV4Request request) {
        SdxCluster sdxCluster = getSdxClusterByCrn(crn);
        return sdxService.refreshRecipes(sdxCluster, request);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.REFRESH_RECIPES_DATALAKE)
    public UpdateRecipesV4Response refreshRecipesByName(@ResourceName String name, @Valid UpdateRecipesV4Request request) {
        SdxCluster sdxCluster = getSdxClusterByName(name);
        return sdxService.refreshRecipes(sdxCluster, request);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.REFRESH_RECIPES_DATALAKE)
    public AttachRecipeV4Response attachRecipeByCrn(@ResourceCrn @TenantAwareParam String crn, @Valid AttachRecipeV4Request request) {
        SdxCluster sdxCluster = getSdxClusterByCrn(crn);
        return sdxService.attachRecipe(sdxCluster, request);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.REFRESH_RECIPES_DATALAKE)
    public AttachRecipeV4Response attachRecipeByName(@ResourceName String name, @Valid AttachRecipeV4Request request) {
        SdxCluster sdxCluster = getSdxClusterByName(name);
        return sdxService.attachRecipe(sdxCluster, request);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.REFRESH_RECIPES_DATALAKE)
    public DetachRecipeV4Response detachRecipeByCrn(@ResourceCrn @TenantAwareParam String crn, @Valid DetachRecipeV4Request request) {
        SdxCluster sdxCluster = getSdxClusterByCrn(crn);
        return sdxService.detachRecipe(sdxCluster, request);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.REFRESH_RECIPES_DATALAKE)
    public DetachRecipeV4Response detachRecipeByName(@ResourceName String name, @Valid DetachRecipeV4Request request) {
        SdxCluster sdxCluster = getSdxClusterByName(name);
        return sdxService.detachRecipe(sdxCluster, request);
    }

    private SdxCluster getSdxClusterByCrn(String crn) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        SdxCluster sdxCluster = sdxService.getByCrn(userCrn, crn);
        MDCBuilder.buildMdcContext(sdxCluster);
        return sdxCluster;
    }

    private SdxCluster getSdxClusterByName(String name) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        SdxCluster sdxCluster = sdxService.getByNameInAccount(userCrn, name);
        MDCBuilder.buildMdcContext(sdxCluster);
        return sdxCluster;
    }
}
