package com.sequenceiq.cloudbreak.converter.stack.cluster.host;

import static com.sequenceiq.cloudbreak.structuredevent.json.AnonymizerUtil.anonymize;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.RecipeResponse;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.host.HostGroupResponse;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.host.HostMetadataResponse;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.Recipe;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.GeneratedRecipe;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;

@Component
public class HostGroupToHostGroupResponseConverter extends AbstractConversionServiceAwareConverter<HostGroup, HostGroupResponse> {

    private static final Logger LOGGER = LoggerFactory.getLogger(HostGroupToHostGroupResponseConverter.class);

    @Override
    public HostGroupResponse convert(HostGroup source) {
        HostGroupResponse hostGroupBase = new HostGroupResponse();
        hostGroupBase.setId(source.getId());
        hostGroupBase.setName(source.getName());
        hostGroupBase.setInstanceGroupName(source.getInstanceGroup().getGroupName());
        hostGroupBase.setRecipeIds(getRecipeIds(source.getRecipes()));
        hostGroupBase.setRecoveryMode(source.getRecoveryMode());
        hostGroupBase.setRecipes(getRecipes(source.getRecipes()));
        hostGroupBase.setExtendedRecipes(getExtendedRecipes(source.getGeneratedRecipes()));
        hostGroupBase.setMetadata(getHostMetaDataResponses(source));
        return hostGroupBase;
    }

    private Set<HostMetadataResponse> getHostMetaDataResponses(HostGroup source) {
        Set<InstanceMetaData> attachedInstanceMetaDataSet = source.getInstanceGroup().getAttachedInstanceMetaDataSet();
        Set<HostMetadataResponse> hostMetadataResponseList = new HashSet<>();
        for (InstanceMetaData instanceMetaData : attachedInstanceMetaDataSet) {
            HostMetadataResponse hostMetadataResponse = new HostMetadataResponse();
            hostMetadataResponse.setId(instanceMetaData.getId());
            hostMetadataResponse.setName(instanceMetaData.getInstanceId());
            hostMetadataResponse.setGroupName(source.getName());
            String status;
            switch (instanceMetaData.getInstanceStatus()) {
                case SERVICES_HEALTHY:
                    status = "HEALTHY";
                    break;
                case SERVICES_UNHEALTHY:
                    status = "UNHEALTHY";
                    break;
                case WAITING_FOR_REPAIR:
                    status = "WAITING_FOR_REPAIR";
                    break;
                case SERVICES_RUNNING:
                    status = "RUNNING";
                    break;
                default:
                    status = "UNKNOWN";
                    break;
            }
            hostMetadataResponse.setState(status);
            hostMetadataResponseList.add(hostMetadataResponse);
        }
        return hostMetadataResponseList;
    }

    private Set<String> getExtendedRecipes(Set<GeneratedRecipe> generatedRecipes) {
        Set<String> extendedRecipes = new HashSet<>();
        for (GeneratedRecipe generatedRecipe : generatedRecipes) {
            if (generatedRecipe.getExtendedRecipe() != null) {
                String encodeRecipe = new String(Base64.encodeBase64(anonymize(generatedRecipe.getExtendedRecipe()).getBytes()));
                extendedRecipes.add(encodeRecipe);
            } else {
                LOGGER.warn("Generated recipe's extendedRecipe field is null");
            }
        }
        return extendedRecipes;
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
