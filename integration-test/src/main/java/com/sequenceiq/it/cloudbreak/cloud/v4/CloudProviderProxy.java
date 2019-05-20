package com.sequenceiq.it.cloudbreak.cloud.v4;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.stack.StackV4ParameterBase;
import com.sequenceiq.it.cloudbreak.dto.ClusterTestDto;
import com.sequenceiq.it.cloudbreak.dto.ImageSettingsTestDto;
import com.sequenceiq.it.cloudbreak.dto.InstanceTemplateV4TestDto;
import com.sequenceiq.it.cloudbreak.dto.NetworkV4TestDto;
import com.sequenceiq.it.cloudbreak.dto.PlacementSettingsTestDto;
import com.sequenceiq.it.cloudbreak.dto.StackAuthenticationTestDto;
import com.sequenceiq.it.cloudbreak.dto.VolumeV4TestDto;
import com.sequenceiq.it.cloudbreak.dto.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.imagecatalog.ImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDtoBase;

@Component
public class CloudProviderProxy implements CloudProvider {

    private CloudProvider delegate;

    @Inject
    private CommonCloudProperties commonCloudProperties;

    @Inject
    private List<CloudProvider> cloudProviders;

    private final Map<CloudPlatform, CloudProvider> cloudProviderMap = new HashMap<>();

    @PostConstruct
    private void init() {
        Map<CloudPlatform, CloudProvider> cloudProviderMap = new HashMap<>();
        cloudProviders.forEach(cloudProvider -> {
            cloudProviderMap.put(cloudProvider.getCloudPlatform(), cloudProvider);
        });
        delegate = cloudProviderMap.get(CloudPlatform.valueOf(commonCloudProperties.getCloudProvider()));
    }

    @Override
    public String availabilityZone() {
        return delegate.availabilityZone();
    }

    @Override
    public String region() {
        return delegate.region();
    }

    @Override
    public String location() {
        return delegate.location();
    }

    @Override
    public ImageCatalogTestDto imageCatalog(ImageCatalogTestDto imageCatalog) {
        return delegate.imageCatalog(imageCatalog);
    }

    @Override
    public ImageSettingsTestDto imageSettings(ImageSettingsTestDto imageSettings) {
        return delegate.imageSettings(imageSettings);
    }

    @Override
    public InstanceTemplateV4TestDto template(InstanceTemplateV4TestDto template) {
        return delegate.template(template);
    }

    @Override
    public StackTestDtoBase stack(StackTestDtoBase stack) {
        return delegate.stack(stack);
    }

    @Override
    public ClusterTestDto cluster(ClusterTestDto cluster) {
        return delegate.cluster(cluster);
    }

    @Override
    public VolumeV4TestDto attachedVolume(VolumeV4TestDto volume) {
        return delegate.attachedVolume(volume);
    }

    @Override
    public NetworkV4TestDto network(NetworkV4TestDto network) {
        return delegate.network(network);
    }

    @Override
    public String getSubnetCIDR() {
        return delegate.getSubnetCIDR();
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return delegate.getCloudPlatform();
    }

    @Override
    public CredentialTestDto credential(CredentialTestDto credential) {
        return delegate.credential(credential);
    }

    @Override
    public EnvironmentTestDto environment(EnvironmentTestDto environment) {
        return delegate.environment(environment);
    }

    @Override
    public PlacementSettingsTestDto placement(PlacementSettingsTestDto placement) {
        return delegate.placement(placement);
    }

    @Override
    public StackAuthenticationTestDto stackAuthentication(StackAuthenticationTestDto stackAuthenticationEntity) {
        return delegate.stackAuthentication(stackAuthenticationEntity);
    }

    @Override
    public Integer gatewayPort(StackTestDtoBase stackEntity) {
        return delegate.gatewayPort(stackEntity);
    }

    @Override
    public String getBlueprintName() {
        return delegate.getBlueprintName();
    }

    @Override
    public StackV4ParameterBase stackParameters() {
        return delegate.stackParameters();
    }
}
