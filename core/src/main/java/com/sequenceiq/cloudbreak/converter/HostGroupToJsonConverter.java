package com.sequenceiq.cloudbreak.converter;

import java.util.Set;

import javax.inject.Inject;

import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.sequenceiq.cloudbreak.api.model.HostGroupJson;
import com.sequenceiq.cloudbreak.api.model.HostMetadataJson;
import com.sequenceiq.cloudbreak.api.model.ConstraintJson;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.HostMetadata;
import com.sequenceiq.cloudbreak.domain.Recipe;

@Component
public class HostGroupToJsonConverter extends AbstractConversionServiceAwareConverter<HostGroup, HostGroupJson> {

    @Inject
    private ConversionService conversionService;

    @Override
    public HostGroupJson convert(HostGroup source) {
        HostGroupJson hostGroupJson = new HostGroupJson();
        hostGroupJson.setName(source.getName());
        hostGroupJson.setConstraint(conversionService.convert(source.getConstraint(), ConstraintJson.class));
        hostGroupJson.setRecipeIds(getRecipeIds(source.getRecipes()));
        hostGroupJson.setMetadata(getHostMetadata(source.getHostMetadata()));
        return hostGroupJson;
    }

    private Set<HostMetadataJson> getHostMetadata(final Set<HostMetadata> hostMetadata) {
        return FluentIterable.from(hostMetadata).transform(new Function<HostMetadata, HostMetadataJson>() {
            @Override
            public HostMetadataJson apply(HostMetadata metadata) {
                HostMetadataJson hostMetadataJson = new HostMetadataJson();
                hostMetadataJson.setId(metadata.getId());
                hostMetadataJson.setGroupName(metadata.getHostGroup().getName());
                hostMetadataJson.setName(metadata.getHostName());
                hostMetadataJson.setState(metadata.getHostMetadataState().name());
                return hostMetadataJson;
            }
        }).toSet();
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
