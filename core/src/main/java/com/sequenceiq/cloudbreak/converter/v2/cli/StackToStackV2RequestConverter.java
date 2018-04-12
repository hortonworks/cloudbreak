package com.sequenceiq.cloudbreak.converter.v2.cli;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.model.StackAuthenticationRequest;
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
        stackV2Request.setGeneral(getGeneralSettings("", source.getCredential().getName()));
        stackV2Request.setPlacement(getPlacementSettings(source.getRegion(), source.getAvailabilityZone()));
        stackV2Request.setCustomDomain(getCustomDomainSettings(source.getCustomDomain(), source.getCustomHostname(),
                source.isHostgroupNameAsHostname(), source.isClusterNameAsSubdomain()));
        stackV2Request.setFlexId(source.getFlexSubscription() == null ? null : source.getFlexSubscription().getId());
        stackV2Request.setParameters(source.getParameters());
        stackV2Request.setInstanceGroups(new ArrayList<>());
        stackV2Request.setStackAuthentication(getConversionService().convert(source.getStackAuthentication(), StackAuthenticationRequest.class));
        stackV2Request.setNetwork(getConversionService().convert(source.getNetwork(), NetworkV2Request.class));
        stackV2Request.setCluster(getConversionService().convert(source.getCluster(), ClusterV2Request.class));
        for (InstanceGroup instanceGroup : source.getInstanceGroups()) {
            InstanceGroupV2Request instanceGroupV2Request = getConversionService().convert(instanceGroup, InstanceGroupV2Request.class);
            instanceGroupV2Request = collectInformationsFromActualHostgroup(source, instanceGroup, instanceGroupV2Request);
            stackV2Request.getInstanceGroups().add(instanceGroupV2Request);
        }
        prepareImage(source, stackV2Request);
        prepareTags(source, stackV2Request);
        return stackV2Request;
    }

    private PlacementSettings getPlacementSettings(String region, String availabilityZone) {
        PlacementSettings ps = new PlacementSettings();
        ps.setRegion(region);
        ps.setAvailabilityZone(availabilityZone);
        return ps;
    }

    private CustomDomainSettings getCustomDomainSettings(String customDomain, String customHostname,
            boolean hostgroupNameAsHostname, boolean clusterNameAsSubdomain) {
        CustomDomainSettings cd = new CustomDomainSettings();
        cd.setCustomDomain(customDomain);
        cd.setCustomHostname(customHostname);
        cd.setHostgroupNameAsHostname(hostgroupNameAsHostname);
        cd.setClusterNameAsSubdomain(clusterNameAsSubdomain);
        return cd;
    }

    private GeneralSettings getGeneralSettings(String name, String credentialName) {
        GeneralSettings gs = new GeneralSettings();
        gs.setName(name);
        gs.setCredentialName(credentialName);
        return gs;
    }

    private void prepareImage(Stack source, StackV2Request stackV2Request) {
        try {
            Image image = componentConfigProvider.getImage(source.getId());
            ImageSettings is = new ImageSettings();
            is.setImageId(Strings.isNullOrEmpty(image.getImageId()) ? "" : image.getImageId());
            is.setImageCatalog(Strings.isNullOrEmpty(image.getImageCatalogName()) ? "" : image.getImageCatalogName());
            stackV2Request.setImageSettings(is);
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
            if (stackTags.getUserDefinedTags() != null) {
                Tags tags = new Tags();
                tags.setApplicationTags(null);
                tags.setDefaultTags(null);
                tags.setUserDefinedTags(stackTags.getUserDefinedTags());
                stackV2Request.setTags(tags);
            }
        } catch (IOException e) {
            stackV2Request.setTags(null);
        }
    }

}
