package com.sequenceiq.cloudbreak.converter.v2.cli;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.model.OrchestratorRequest;
import com.sequenceiq.cloudbreak.api.model.StackAuthenticationRequest;
import com.sequenceiq.cloudbreak.api.model.v2.ClusterV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.InstanceGroupV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.NetworkV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.StackV2Request;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.StackTags;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.Recipe;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.ComponentConfigProvider;

@Component
public class StackToStackV2RequestConverter extends AbstractConversionServiceAwareConverter<Stack, StackV2Request> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackToStackV2RequestConverter.class);

    @Inject
    private ComponentConfigProvider componentConfigProvider;

    @Override
    public StackV2Request convert(Stack source) {
        StackV2Request stackV2Request = new StackV2Request();

        stackV2Request.setName("");
        stackV2Request.setRegion(source.getRegion());
        stackV2Request.setAvailabilityZone(source.getAvailabilityZone());
        stackV2Request.setCredentialName(source.getCredential().getName());
        stackV2Request.setOnFailureAction(null);
        stackV2Request.setClusterNameAsSubdomain(source.isClusterNameAsSubdomain());
        stackV2Request.setCustomHostname(source.getCustomHostname());
        stackV2Request.setCustomDomain(source.getCustomDomain());
        stackV2Request.setFlexId(source.getFlexSubscription() == null ? null : source.getFlexSubscription().getId());
        stackV2Request.setHostgroupNameAsHostname(source.isHostgroupNameAsHostname());
        stackV2Request.setParameters(source.getParameters());
        stackV2Request.setInstanceGroups(new ArrayList<>());
        stackV2Request.setOrchestrator(getConversionService().convert(source.getOrchestrator(), OrchestratorRequest.class));
        stackV2Request.setStackAuthentication(getConversionService().convert(source.getStackAuthentication(), StackAuthenticationRequest.class));
        stackV2Request.setNetwork(getConversionService().convert(source.getNetwork(), NetworkV2Request.class));
        stackV2Request.setClusterRequest(getConversionService().convert(source.getCluster(), ClusterV2Request.class));
        for (InstanceGroup instanceGroup : source.getInstanceGroups()) {
            InstanceGroupV2Request instanceGroupV2Request = getConversionService().convert(instanceGroup, InstanceGroupV2Request.class);
            instanceGroupV2Request = collectInformationsFromActualHostgroup(source, instanceGroup, instanceGroupV2Request);
            stackV2Request.getInstanceGroups().add(instanceGroupV2Request);
        }
        prepareImage(source, stackV2Request);
        prepareTags(source, stackV2Request);
        return stackV2Request;
    }

    private void prepareImage(Stack source, StackV2Request stackV2Request) {
        try {
            Image image = componentConfigProvider.getImage(source.getId());
            stackV2Request.setImageId(Strings.isNullOrEmpty(image.getImageId()) ? "" : image.getImageId());
            stackV2Request.setImageCatalog(Strings.isNullOrEmpty(image.getImageCatalogName()) ? "" : image.getImageCatalogName());
        } catch (CloudbreakImageNotFoundException e) {
            LOGGER.error(e.toString());
        }
    }

    private InstanceGroupV2Request collectInformationsFromActualHostgroup(Stack source, InstanceGroup instanceGroup,
            InstanceGroupV2Request instanceGroupV2Request) {
        HostGroup actualHostgroup = null;
        for (HostGroup hostGroup : source.getCluster().getHostGroups()) {
            if (hostGroup.getName().equals(instanceGroup.getGroupName())) {
                actualHostgroup = hostGroup;
            }
        }
        if (actualHostgroup != null) {
            instanceGroupV2Request.setRecoveryMode(actualHostgroup.getRecoveryMode());
            instanceGroupV2Request.setRecipeNames(new HashSet<>());
            for (Recipe recipe : actualHostgroup.getRecipes()) {
                instanceGroupV2Request.getRecipeNames().add(recipe.getName());
            }
        }
        return instanceGroupV2Request;
    }

    private void prepareTags(Stack source, StackV2Request stackV2Request) {
        try {
            StackTags stackTags = source.getTags().get(StackTags.class);
            stackV2Request.setApplicationTags(null);
            stackV2Request.setDefaultTags(null);
            stackV2Request.setUserDefinedTags(stackTags.getUserDefinedTags());
        } catch (IOException e) {
            stackV2Request.setApplicationTags(new HashMap<>());
            stackV2Request.setDefaultTags(new HashMap<>());
            stackV2Request.setUserDefinedTags(new HashMap<>());
        }
    }

}
