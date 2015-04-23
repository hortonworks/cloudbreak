package com.sequenceiq.cloudbreak.converter;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.sequenceiq.cloudbreak.controller.json.HostGroupJson;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.Recipe;

@Component
public class HostGroupToJsonConverter extends AbstractConversionServiceAwareConverter<HostGroup, HostGroupJson> {
    @Override
    public HostGroupJson convert(HostGroup source) {
        HostGroupJson hostGroupJson = new HostGroupJson();
        hostGroupJson.setName(source.getName());
        hostGroupJson.setInstanceGroupName(source.getInstanceGroup().getGroupName());
        hostGroupJson.setRecipeIds(getRecipeIds(source.getRecipes()));
        return hostGroupJson;
    }

    private Set<Long> getRecipeIds(Set<Recipe> recipes) {
        return FluentIterable.from(recipes).transform(new Function<Recipe, Long>() {
            @Override
            public Long apply(Recipe recipe) {
                return recipe.getId();
            }
        }).toSet();
    }
}
