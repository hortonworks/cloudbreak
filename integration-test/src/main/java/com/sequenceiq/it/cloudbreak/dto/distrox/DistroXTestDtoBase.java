package com.sequenceiq.it.cloudbreak.dto.distrox;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.distrox.api.v1.distrox.model.AwsDistroXV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.AzureDistroXV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.database.DistroXDatabaseRequest;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.InstanceGroupV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.network.InstanceGroupNetworkV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.network.aws.InstanceGroupAwsNetworkV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.sharedservice.SdxV1Request;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractCloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.cluster.DistroXClusterTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.externaldatabase.DistroXExternalDatabaseTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.image.DistroXImageTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.instancegroup.DistroXInstanceGroupTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.instancegroup.DistroXNetworkTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.imagecatalog.ImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.microservice.EnvironmentClient;

public class DistroXTestDtoBase<T extends DistroXTestDtoBase> extends AbstractCloudbreakTestDto<DistroXV1Request, StackV4Response, T> {

    public static final String DISTROX_RESOURCE_NAME = "distroxName";

    protected DistroXTestDtoBase(DistroXV1Request request, TestContext testContext) {
        super(request, testContext);
    }

    public DistroXTestDtoBase<T> valid() {
        String name = getResourcePropertyProvider().getName(15, getCloudPlatform());
        withName(name)
                .withInstanceGroupsEntity(DistroXInstanceGroupTestDto.dataEngHostGroups(getTestContext(), getCloudPlatform()))
                .withCluster(getTestContext().given(DistroXClusterTestDto.class, getCloudPlatform()))
                .withImageSettings(getTestContext().given(DistroXImageTestDto.class, getCloudPlatform()))
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
        if (env == null) {
            throw new IllegalArgumentException("Env is null with given key: " + environmentKey);
        }
        if (env.getResponse() == null) {
            throw new IllegalArgumentException("Env response is null with given key: " + environmentKey);
        }
        return withEnvironmentName(env.getResponse().getName());
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

    public DistroXTestDtoBase<T> withResourceEncryption() {
        return getCloudProvider().withResourceEncryption(this);
    }

    public DistroXTestDtoBase<T> withAzure(AzureDistroXV1Parameters azure) {
        getRequest().setAzure(azure);
        return this;
    }

    public DistroXTestDtoBase<T> withAws(AwsDistroXV1Parameters aws) {
        getRequest().setAws(aws);
        return this;
    }

    public DistroXTestDtoBase<T> withImageSettings() {
        return withImageSettings(DistroXImageTestDto.class.getSimpleName());
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

    public DistroXTestDtoBase<T> withExternalDatabase(DistroXDatabaseRequest database) {
        getRequest().setExternalDatabase(database);
        return this;
    }

    public DistroXTestDtoBase<T> withPreferredSubnetsForInstanceNetworkIfMultiAzEnabledOrJustFirst() {
        if (StringUtils.isEmpty(getRequest().getEnvironmentName())) {
            throw new TestFailException("Cannot fetch the preferred subnet without env name, please add it");
        }
        try {
            EnvironmentClient envClient = getTestContext().getMicroserviceClient(EnvironmentClient.class);
            DetailedEnvironmentResponse envResponse = envClient.getDefaultClient(getTestContext())
                    .environmentV1Endpoint()
                    .getByName(getRequest().getEnvironmentName());
            InstanceGroupNetworkV1Request instanceGroupNetworkV1Request = new InstanceGroupNetworkV1Request();
            InstanceGroupAwsNetworkV1Parameters awsNetworkV1Parameters = new InstanceGroupAwsNetworkV1Parameters();
            if ("AWS_NATIVE".equals(getRequest().getVariant())) {
                awsNetworkV1Parameters.setSubnetIds(new ArrayList<>(envResponse.getNetwork().getPreferedSubnetIds()));
            } else {
                envResponse.getNetwork().getPreferedSubnetIds().stream()
                        .filter(s -> !s.equals(envResponse.getNetwork().getPreferedSubnetId()))
                        .findFirst().ifPresent(s -> awsNetworkV1Parameters.setSubnetIds(List.of(s)));
            }
            instanceGroupNetworkV1Request.setAws(awsNetworkV1Parameters);
            getRequest().getInstanceGroups().forEach(s -> s.setNetwork(instanceGroupNetworkV1Request));
        } catch (Exception e) {
            String message = "Cannot fetch preferred subnets from " + getRequest().getEnvironmentName();
            throw new TestFailException(message, e);
        }
        return this;
    }

    public DistroXTestDtoBase<T> withLoadBalancer() {
        getRequest().setEnableLoadBalancer(true);
        return this;
    }

    public DistroXTestDtoBase<T> addTags(Map<String, String> tags) {
        tags.forEach((key, value) -> getRequest().addTag(key, value));
        return this;
    }

    public String getVariant() {
        return getResponse().getVariant();
    }

    public DistroXTestDtoBase<T> addApplicationTags(Map<String, String> tags) {
        tags.forEach((key, value) -> getRequest().initAndGetTags().getApplication().put(key, value));
        return this;
    }
}