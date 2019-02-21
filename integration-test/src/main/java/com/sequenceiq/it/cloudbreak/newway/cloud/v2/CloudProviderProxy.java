package com.sequenceiq.it.cloudbreak.newway.cloud.v2;

import static com.sequenceiq.it.cloudbreak.newway.cloud.v2.parameter.CommonCloudParameters.CLOUD_PROVIDER;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable.CloudPlatform;
import com.sequenceiq.it.cloudbreak.newway.EnvironmentEntity;
import com.sequenceiq.it.cloudbreak.newway.ImageCatalogEntity;
import com.sequenceiq.it.cloudbreak.newway.entity.InstanceTemplateV4Entity;
import com.sequenceiq.it.cloudbreak.newway.entity.NetworkV2Entity;
import com.sequenceiq.it.cloudbreak.newway.entity.PlacementSettingsEntity;
import com.sequenceiq.it.cloudbreak.newway.entity.StackAuthenticationEntity;
import com.sequenceiq.it.cloudbreak.newway.entity.StackV4EntityBase;
import com.sequenceiq.it.cloudbreak.newway.entity.VolumeV4Entity;
import com.sequenceiq.it.cloudbreak.newway.entity.credential.CredentialTestDto;

@Component
public class CloudProviderProxy implements CloudProvider {

    private CloudProvider delegate;

    @Value("${" + CLOUD_PROVIDER + ":MOCK}")
    private CloudPlatform cloudPlatform;

    @Inject
    private List<CloudProvider> cloudProviders;

    private final Map<CloudPlatform, CloudProvider> cloudProviderMap = new HashMap<>();

    @PostConstruct
    private void init() {
        Map<CloudPlatform, CloudProvider> cloudProviderMap = new HashMap<>();
        cloudProviders.forEach(cloudProvider -> {
            cloudProviderMap.put(cloudProvider.getCloudPlatform(), cloudProvider);
        });
        delegate = cloudProviderMap.get(cloudPlatform);
    }

//    @Override
//    public String availabilityZone(TestContext testContext) {
//        return delegate.availabilityZone(testContext);
//    }

//    @Override
//    public String region(TestContext testContext) {
//        return delegate.region(testContext);
//    }

//    @Override
//    public InstanceTemplateV4Entity template(TestContext testContext) {
//        return delegate.template(testContext);
//    }

//    @Override
//    public NetworkV2Entity network(TestContext testContext) {
//        return delegate.network(testContext);
//    }

//    @Override
//    public String getSubnetCIDR(TestContext testContext) {
//        return delegate.getSubnetCIDR(testContext);
//    }

    @Override
    public ImageCatalogEntity imageCatalog(ImageCatalogEntity imageCatalog) {
        return delegate.imageCatalog(imageCatalog);
    }

    @Override
    public InstanceTemplateV4Entity template(InstanceTemplateV4Entity template) {
        return delegate.template(template);
    }

    @Override
    public VolumeV4Entity attachedVolume(VolumeV4Entity volume) {
        return delegate.attachedVolume(volume);
    }

    @Override
    public NetworkV2Entity network(NetworkV2Entity network) {
        return delegate.network(network);
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
    public EnvironmentEntity environment(EnvironmentEntity environment) {
        return delegate.environment(environment);
    }

    @Override
    public PlacementSettingsEntity placement(PlacementSettingsEntity placement) {
        return delegate.placement(placement);
    }

    @Override
    public StackAuthenticationEntity stackAuthentication(StackAuthenticationEntity stackAuthenticationEntity) {
        return delegate.stackAuthentication(stackAuthenticationEntity);
    }

    @Override
    public Integer gatewayPort(StackV4EntityBase stackEntity) {
        return delegate.gatewayPort(stackEntity);
    }
}
