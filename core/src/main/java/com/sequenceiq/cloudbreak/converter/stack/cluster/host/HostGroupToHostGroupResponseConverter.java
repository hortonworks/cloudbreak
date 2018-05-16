package com.sequenceiq.cloudbreak.converter.stack.cluster.host;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.ConstraintJson;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.host.HostGroupResponse;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.host.HostMetadataResponse;
import com.sequenceiq.cloudbreak.api.model.RecipeResponse;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostMetadata;
import com.sequenceiq.cloudbreak.domain.Recipe;

@Component
public class HostGroupToHostGroupResponseConverter extends AbstractConversionServiceAwareConverter<HostGroup, HostGroupResponse> {

    @Override
    public HostGroupResponse convert(HostGroup source) {
        HostGroupResponse hostGroupBase = new HostGroupResponse();
        hostGroupBase.setId(source.getId());
        hostGroupBase.setName(source.getName());
        hostGroupBase.setConstraint(getConversionService().convert(source.getConstraint(), ConstraintJson.class));
        hostGroupBase.setRecipeIds(getRecipeIds(source.getRecipes()));
        hostGroupBase.setMetadata(getHostMetadata(source.getHostMetadata()));
        hostGroupBase.setRecoveryMode(source.getRecoveryMode());
        hostGroupBase.setRecipes(getRecipes(source.getRecipes()));
        return hostGroupBase;
    }

    private Set<HostMetadataResponse> getHostMetadata(Iterable<HostMetadata> hostMetadataCollection) {
        Set<HostMetadataResponse> hostMetadataResponses = new HashSet<>();
        for (HostMetadata hostMetadata : hostMetadataCollection) {
            hostMetadataResponses.add(getConversionService().convert(hostMetadata, HostMetadataResponse.class));
        }
        return hostMetadataResponses;
    }

    private Set<Long> getRecipeIds(Collection<Recipe> recipes) {
        return recipes.stream().map(Recipe::getId).collect(Collectors.toSet());
    }

    private Set<RecipeResponse> getRecipes(Iterable<Recipe> recipes) {
        Set<RecipeResponse> recipeResponseSet = new HashSet<>();
        for (Recipe recipe : recipes) {
            recipeResponseSet.add(getConversionService().convert(recipe, RecipeResponse.class));
        }
        return recipeResponseSet;
    }
}
