package com.sequenceiq.cloudbreak.controller;

import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v1.RecipeEndpoint;
import com.sequenceiq.cloudbreak.api.model.RecipeRequest;
import com.sequenceiq.cloudbreak.api.model.RecipeResponse;
import com.sequenceiq.cloudbreak.common.type.ResourceEvent;
import com.sequenceiq.cloudbreak.domain.Recipe;
import com.sequenceiq.cloudbreak.domain.organization.Organization;
import com.sequenceiq.cloudbreak.domain.organization.User;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.organization.OrganizationService;
import com.sequenceiq.cloudbreak.service.recipe.RecipeService;
import com.sequenceiq.cloudbreak.service.user.UserService;

@Component
@Transactional(TxType.NEVER)
public class RecipeController extends NotificationController implements RecipeEndpoint {

    @Inject
    private RecipeService recipeService;

    @Inject
    @Named("conversionService")
    private ConversionService conversionService;

    @Inject
    private OrganizationService organizationService;

    @Inject
    private UserService userService;

    @Inject
    private RestRequestThreadLocalService restRequestThreadLocalService;

    @Override
    public RecipeResponse get(Long id) {
        return conversionService.convert(recipeService.get(id), RecipeResponse.class);
    }

    @Override
    public void delete(Long id) {
        Recipe deleted = recipeService.delete(id);
        notify(ResourceEvent.RECIPE_DELETED);
        conversionService.convert(deleted, RecipeResponse.class);
    }

    @Override
    public RecipeRequest getRequestfromName(String name) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getIdentityUser());
        Organization organization = organizationService.get(restRequestThreadLocalService.getRequestedOrgId(), user);
        Recipe recipe = recipeService.getByNameForOrganization(name, organization);
        return conversionService.convert(recipe, RecipeRequest.class);
    }

    @Override
    public RecipeResponse postPublic(RecipeRequest recipeRequest) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getIdentityUser());
        return createInDefaultOrganization(recipeRequest, user);
    }

    @Override
    public RecipeResponse postPrivate(RecipeRequest recipeRequest) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getIdentityUser());
        return createInDefaultOrganization(recipeRequest, user);
    }

    @Override
    public Set<RecipeResponse> getPrivates() {
        User user = userService.getOrCreate(restRequestThreadLocalService.getIdentityUser());
        return listForUsersDefaultOrganization(user);
    }

    @Override
    public Set<RecipeResponse> getPublics() {
        User user = userService.getOrCreate(restRequestThreadLocalService.getIdentityUser());
        return listForUsersDefaultOrganization(user);
    }

    @Override
    public RecipeResponse getPrivate(String name) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getIdentityUser());
        return getRecipeResponse(name, user);
    }

    @Override
    public RecipeResponse getPublic(String name) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getIdentityUser());
        return getRecipeResponse(name, user);
    }

    @Override
    public void deletePublic(String name) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getIdentityUser());
        deleteInDefaultOrganization(name, user);
    }

    @Override
    public void deletePrivate(String name) {
        User user = userService.getOrCreate(restRequestThreadLocalService.getIdentityUser());
        deleteInDefaultOrganization(name, user);
    }

    private RecipeResponse getRecipeResponse(String name, User user) {
        Organization organization = organizationService.get(restRequestThreadLocalService.getRequestedOrgId(), user);
        return conversionService.convert(recipeService.getByNameForOrganization(name, organization), RecipeResponse.class);
    }

    private Set<RecipeResponse> listForUsersDefaultOrganization(User user) {
        Organization organization = organizationService.get(restRequestThreadLocalService.getRequestedOrgId(), user);
        return recipeService.findAllByOrganization(organization).stream()
                .map(recipe -> conversionService.convert(recipe, RecipeResponse.class))
                .collect(Collectors.toSet());
    }

    private void deleteInDefaultOrganization(String name, User user) {
        Organization organization = organizationService.get(restRequestThreadLocalService.getRequestedOrgId(), user);
        executeAndNotify(identityUser -> recipeService.deleteByNameFromOrganization(name, organization.getId()), ResourceEvent.RECIPE_DELETED);
    }

    private RecipeResponse createInDefaultOrganization(RecipeRequest request, User user) {
        Recipe recipe = conversionService.convert(request, Recipe.class);
        Organization organization = organizationService.get(restRequestThreadLocalService.getRequestedOrgId(), user);
        recipe = recipeService.create(recipe, organization, user);
        return notifyAndReturn(recipe, ResourceEvent.RECIPE_CREATED);
    }

    private RecipeResponse notifyAndReturn(Recipe recipe, ResourceEvent resourceEvent) {
        notify(resourceEvent);
        return conversionService.convert(recipe, RecipeResponse.class);
    }
}
