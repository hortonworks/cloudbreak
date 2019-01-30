package com.sequenceiq.cloudbreak.converter.v4.stacks.instancegroup;

import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.responses.RecipeV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.Recipe;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;

@Component
public class HostGroupToInstanceGroupV4ResponseConverter extends AbstractConversionServiceAwareConverter<HostGroup, InstanceGroupV4Response> {

    @Inject
    private ConverterUtil converterUtil;

    @Override
    public InstanceGroupV4Response convert(HostGroup source) {

        InstanceGroupV4Response response = new InstanceGroupV4Response();

        Set<String> recipeNames = source.getRecipes().stream().map(Recipe::getName).collect(Collectors.toSet());
        response.setRecipes(converterUtil.convertAll(source.getRecipes(), RecipeV4Response.class));
        response.setRecoveryMode(source.getRecoveryMode());

        response.getMetadata()
                .forEach(imd -> source.getHostMetadata().stream()
                        .filter(s -> s.getHostName().equals(imd.getDiscoveryFQDN()))
                        .findFirst()
                        .ifPresent(hmd -> imd.setState(hmd.getHostMetadataState().name()))
                );
        return response;
    }
}
