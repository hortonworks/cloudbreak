package com.sequenceiq.cloudbreak.converter.v2.cli;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.model.SharedServiceRequest;
import com.sequenceiq.cloudbreak.api.model.stack.StackAuthenticationRequest;
import com.sequenceiq.cloudbreak.api.model.v2.ClusterV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.CustomDomainSettings;
import com.sequenceiq.cloudbreak.api.model.v2.GeneralSettings;
import com.sequenceiq.cloudbreak.api.model.v2.ImageSettings;
import com.sequenceiq.cloudbreak.api.model.v2.InstanceGroupV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.NetworkV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.PlacementSettings;
import com.sequenceiq.cloudbreak.api.model.v2.StackV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.Tags;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.StackInputs;
import com.sequenceiq.cloudbreak.cloud.model.StackTags;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.Recipe;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.service.ComponentConfigProvider;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Component
public class StackToStackV2RequestConverter extends AbstractConversionServiceAwareConverter<Stack, StackV2Request> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackToStackV2RequestConverter.class);

    @Inject
    private ComponentConfigProvider componentConfigProvider;

    @Inject
    private StackService stackService;

    @Override
    public StackV2Request convert(Stack source) {
        StackV2Request stackV2Request = new StackV2Request();
        stackV2Request.setGeneral(getGeneral(source));
        stackV2Request.setPlacement(getPlacementSettings(source.getRegion(), source.getAvailabilityZone()));
        stackV2Request.setCustomDomain(getCustomDomainSettings(source));
        stackV2Request.setFlexId(source.getFlexSubscription() == null ? null : source.getFlexSubscription().getId());
        stackV2Request.setParameters(source.getParameters());
        stackV2Request.setStackAuthentication(getConversionService().convert(source.getStackAuthentication(), StackAuthenticationRequest.class));
        stackV2Request.setNetwork(getConversionService().convert(source.getNetwork(), NetworkV2Request.class));
        stackV2Request.setCluster(getConversionService().convert(source.getCluster(), ClusterV2Request.class));
        stackV2Request.setInstanceGroups(getInstanceGroups(source));
        prepareImage(source, stackV2Request);
        prepareTags(source, stackV2Request);
        prepareInputs(source, stackV2Request);
        prepareDatalakeRequest(source, stackV2Request);
        return stackV2Request;
    }

    private void prepareDatalakeRequest(Stack source, StackV2Request stackV2Request) {
        if (source.getDatalakeId() != null) {
            SharedServiceRequest sharedServiceRequest = new SharedServiceRequest();
            sharedServiceRequest.setSharedCluster(stackService.get(source.getDatalakeId()).getName());
            stackV2Request.getCluster().setSharedService(sharedServiceRequest);
        }
    }

    private PlacementSettings getPlacementSettings(String region, String availabilityZone) {
        PlacementSettings ps = new PlacementSettings();
        ps.setRegion(region);
        ps.setAvailabilityZone(availabilityZone);
        return ps;
    }

    private CustomDomainSettings getCustomDomainSettings(Stack stack) {
        CustomDomainSettings cd = new CustomDomainSettings();
        cd.setCustomDomain(stack.getCustomDomain());
        cd.setCustomHostname(stack.getCustomHostname());
        cd.setHostgroupNameAsHostname(stack.isHostgroupNameAsHostname());
        cd.setClusterNameAsSubdomain(stack.isClusterNameAsSubdomain());
        return cd;
    }

    private GeneralSettings getGeneral(Stack source) {
        GeneralSettings generalSettings = new GeneralSettings();
        generalSettings.setName("");
        if (source.getEnvironment() != null) {
            generalSettings.setEnvironmentName(source.getEnvironment().getName());
        }
        if (source.getCredential() != null) {
            generalSettings.setCredentialName(source.getCredential().getName());
        }
        return generalSettings;
    }

    private void prepareImage(Stack source, StackV2Request stackV2Request) {
        try {
            Image image = componentConfigProvider.getImage(source.getId());
            ImageSettings is = new ImageSettings();
            is.setImageId(Strings.isNullOrEmpty(image.getImageId()) ? "" : image.getImageId());
            is.setImageCatalog(Strings.isNullOrEmpty(image.getImageCatalogName()) ? "" : image.getImageCatalogName());
            stackV2Request.setImageSettings(is);
        } catch (CloudbreakImageNotFoundException e) {
            LOGGER.info(e.toString());
        }
    }

    private List<InstanceGroupV2Request> getInstanceGroups(Stack stack) {
        List<InstanceGroupV2Request> ret = new ArrayList<>();
        for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {
            InstanceGroupV2Request instanceGroupV2Request = getConversionService().convert(instanceGroup, InstanceGroupV2Request.class);
            collectInformationsFromActualHostgroup(stack.getCluster(), instanceGroup, instanceGroupV2Request);
            ret.add(instanceGroupV2Request);
        }
        return ret;
    }

    private void collectInformationsFromActualHostgroup(Cluster cluster, InstanceGroup instanceGroup, InstanceGroupV2Request instanceGroupV2Request) {
        if (cluster != null && cluster.getHostGroups() != null) {
            cluster.getHostGroups().stream()
                    .filter(hostGroup -> hostGroup.getName().equals(instanceGroup.getGroupName()))
                    .findFirst()
                    .ifPresent(hostGroup -> {
                        Set<String> recipeNames = hostGroup.getRecipes().stream().map(Recipe::getName).collect(Collectors.toSet());
                        instanceGroupV2Request.setRecipeNames(recipeNames);
                        instanceGroupV2Request.setRecoveryMode(hostGroup.getRecoveryMode());
                    });
        }
    }

    private void prepareTags(Stack source, StackV2Request stackV2Request) {
        try {
            if (source.getTags() != null && source.getTags().getValue() != null) {
                StackTags stackTags = source.getTags().get(StackTags.class);
                if (stackTags.getUserDefinedTags() != null) {
                    Tags tags = new Tags();
                    tags.setApplicationTags(null);
                    tags.setDefaultTags(null);
                    tags.setUserDefinedTags(stackTags.getUserDefinedTags());
                    stackV2Request.setTags(tags);
                }
            }
        } catch (IOException e) {
            stackV2Request.setTags(null);
        }
    }

    private void prepareInputs(Stack source, StackV2Request stackV2Request) {
        try {

            StackInputs stackInputs = Strings.isNullOrEmpty(source.getInputs().getValue())
                    ? new StackInputs(new HashMap<>(), new HashMap<>(), new HashMap<>()) : source.getInputs().get(StackInputs.class);
            if (stackInputs.getCustomInputs() != null) {
                stackV2Request.setInputs(stackInputs.getCustomInputs());
            }
        } catch (IOException e) {
            stackV2Request.setInputs(null);
        }
    }

}
