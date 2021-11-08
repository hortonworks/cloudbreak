package com.sequenceiq.it.cloudbreak.dto.distrox;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.distrox.api.v1.distrox.model.AwsDistroXV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.AzureDistroXV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.InstanceGroupV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.sharedservice.SdxV1Request;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractCloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.cluster.DistroXClusterTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.externaldatabase.DistroXExternalDatabaseTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.image.DistroXImageTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.instancegroup.DistroXInstanceGroupTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.instancegroup.DistroXNetworkTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.imagecatalog.ImageCatalogTestDto;

public class DistroXTestDtoBase<T extends DistroXTestDtoBase> extends AbstractCloudbreakTestDto<DistroXV1Request, StackV4Response, T> {

    private static final String DISTROX_RESOURCE_NAME = "distroxName";

    protected DistroXTestDtoBase(DistroXV1Request request, TestContext testContext) {
        super(request, testContext);
    }

    public DistroXTestDtoBase<T> valid() {
        String name = getResourcePropertyProvider().getName(15, getCloudPlatform());
        withName(name)
                .withInstanceGroupsEntity(DistroXInstanceGroupTestDto.defaultHostGroup(getTestContext()))
                .withCluster(getTestContext().given(DistroXClusterTestDto.class))
                .withImageSettings(getTestContext().given(DistroXImageTestDto.class))
                .withVariant(getCloudProvider().getVariant());
        return getCloudProvider().distrox(this);
    }

    public DistroXTestDtoBase<T> withEnvironmentName(String environmentName) {
        getRequest().setEnvironmentName(environmentName);
        return this;
    }

    public DistroXTestDtoBase<T> withEnvironment() {
        return withEnvironmentKey(EnvironmentTestDto.class.getSimpleName());
    }

    public DistroXTestDtoBase<T> withEnvironmentKey(String environmentKey) {
        EnvironmentTestDto env = getTestContext().get(environmentKey);
        DistroXTestDtoBase<T> ret = this;
        if (env != null && env.getResponse() != null) {
            ret = withEnvironmentName(env.getResponse().getName());
        }
        return ret;
    }

    public DistroXTestDtoBase<T> withName(String name) {
        getRequest().setName(name);
        setName(name);
        return this;
    }

    public DistroXTestDtoBase<T> withVariant(String variant) {
        getRequest().setVariant(variant);
        return this;
    }

    @Override
    public String getResourceNameType() {
        return DISTROX_RESOURCE_NAME;
    }

    public DistroXTestDtoBase<T> withCluster() {
        return withCluster(DistroXClusterTestDto.class.getSimpleName());
    }

    public DistroXTestDtoBase<T> withCluster(String key) {
        DistroXClusterTestDto clusterEntity = getTestContext().get(key);
        return withCluster(clusterEntity);
    }

    public DistroXTestDtoBase<T> withCluster(DistroXClusterTestDto cluster) {
        getRequest().setCluster(cluster.getRequest());
        return this;
    }

    public DistroXTestDtoBase<T> withAzure(AzureDistroXV1Parameters azure) {
        getRequest().setAzure(azure);
        return this;
    }

    public DistroXTestDtoBase<T> withAws(AwsDistroXV1Parameters aws) {
        getRequest().setAws(aws);
        return this;
    }

    public DistroXTestDtoBase<T> withImageSettings(String key) {
        DistroXImageTestDto imageSettingsEntity = getTestContext().get(key);
        getRequest().setImage(imageSettingsEntity.getRequest());
        return this;
    }

    public DistroXTestDtoBase<T> withImageSettingsIf(boolean condition, String key) {
        if (condition) {
            return withImageSettings(key);
        }
        return this;
    }

    public DistroXTestDtoBase<T> withImageSettings(DistroXImageTestDto imageSettings) {
        getRequest().setImage(imageSettings.getRequest());
        ImageCatalogTestDto imageCatalogTestDto = getTestContext().get(ImageCatalogTestDto.class);
        if (imageCatalogTestDto != null) {
            getRequest().getImage().setCatalog(imageCatalogTestDto.getName());
        }
        return this;
    }

    public DistroXTestDtoBase<T> withNetwork(String key) {
        DistroXNetworkTestDto network = getTestContext().get(key);
        return withNetwork(network);
    }

    public DistroXTestDtoBase<T> withNetwork(DistroXNetworkTestDto network) {
        getRequest().setNetwork(network.getRequest());
        return this;
    }

    public DistroXTestDtoBase<T> withSdx(SdxV1Request sdxV1Request) {
        getRequest().setSdx(sdxV1Request);
        return this;
    }

    public DistroXTestDtoBase<T> withInstanceGroupsEntity(Collection<DistroXInstanceGroupTestDto> instanceGroups) {
        getRequest().setInstanceGroups(instanceGroups.stream()
                .map(DistroXInstanceGroupTestDto::getRequest)
                .collect(Collectors.toSet()));
        return this;
    }

    private InstanceGroupV1Request getInstanceGroupV1Request(String key) {
        DistroXInstanceGroupTestDto instanceGroupTestDto = getTestContext().get(key);
        if (instanceGroupTestDto == null) {
            throw new IllegalStateException("Given key is not exists in the test context: " + key);
        }
        return instanceGroupTestDto.getRequest();
    }

    public DistroXTestDtoBase<T> withInstanceGroups(String... keys) {
        getRequest().setInstanceGroups(Stream.of(keys)
                .map(this::getInstanceGroupV1Request)
                .collect(Collectors.toSet()));
        return this;
    }

    public DistroXTestDtoBase<T> withExternalDatabase(String key) {
        DistroXExternalDatabaseTestDto externalDatabaseDto = getTestContext().get(key);
        getRequest().setExternalDatabase(externalDatabaseDto.getRequest());
        return this;
    }
}
