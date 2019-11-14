package com.sequenceiq.it.cloudbreak.dto.distrox;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.distrox.api.v1.distrox.model.AwsDistroXV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.AzureDistroXV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.InstanceGroupV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.sharedservice.SdxV1Request;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractCloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.cluster.DistroXClusterTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.image.DistroXImageTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.instancegroup.DistroXInstanceGroupTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.instancegroup.DistroXNetworkTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.imagecatalog.ImageCatalogTestDto;
import com.sequenceiq.it.util.TagAdderUtil;
import com.sequenceiq.it.util.TestNameExtractorUtil;
import com.sequenceiq.sdx.api.model.SdxInternalClusterRequest;

public class DistroXTestDtoBase<T extends DistroXTestDtoBase> extends AbstractCloudbreakTestDto<DistroXV1Request, StackV4Response, T> {

    @Inject
    private TestNameExtractorUtil testNameExtractorUtil;

    @Inject
    private TagAdderUtil tagAdderUtil;

    protected DistroXTestDtoBase(DistroXV1Request request, TestContext testContext) {
        super(request, testContext);
    }

    public DistroXTestDtoBase<T> valid() {
        String name = getResourcePropertyProvider().getName();
        withName(name)
                .withTestNameAsTag()
                .withInstanceGroupsEntity(DistroXInstanceGroupTestDto.defaultHostGroup(getTestContext()))
                .withCluster(getTestContext().given(DistroXClusterTestDto.class))
                .withImageSettings(getTestContext().given(DistroXImageTestDto.class));
        return getCloudProvider().distrox(this);
    }

    public DistroXTestDtoBase<T> withEnvironmentName(String environmentName) {
        getRequest().setEnvironmentName(environmentName);
        return this;
    }

    public DistroXTestDtoBase<T> withGatewayPort(Integer port) {
        getRequest().setGatewayPort(port);
        return this;
    }

    public DistroXTestDtoBase<T> withEnvironment(Class<EnvironmentTestDto> environmentKey) {
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

    public DistroXTestDtoBase<T> withInternalSdx(String key) {
        SdxInternalClusterRequest sdxInternalClusterRequest = getTestContext().get(key);

        SdxV1Request sdxRequest = new SdxV1Request();
        sdxRequest.setName(sdxInternalClusterRequest.getStackV4Request().getName());
        return withSdx(sdxRequest);
    }

    public DistroXTestDtoBase<T> withInstanceGroupsEntity(Collection<DistroXInstanceGroupTestDto> instanceGroups) {
        getRequest().setInstanceGroups(instanceGroups.stream()
                .map(DistroXInstanceGroupTestDto::getRequest)
                .collect(Collectors.toSet()));
        return this;
    }

    public DistroXTestDtoBase<T> withDefaultInstanceGroups() {
        return withInstanceGroups(DistroXInstanceGroupTestDto.class.getSimpleName());
    }

    public DistroXTestDtoBase<T> replaceInstanceGroups(String... keys) {
        Stream.of(keys)
                .map(this::getInstanceGroupV1Request)
                .forEach(ig -> {
                    List<InstanceGroupV1Request> instanceGroups = new ArrayList<>(getRequest().getInstanceGroups());
                    getRequest().setInstanceGroups(new HashSet<>());
                    for (int i = 0; i < instanceGroups.size(); i++) {
                        InstanceGroupV1Request old = instanceGroups.get(i);
                        if (old.getName().equals(ig.getName())) {
                            getRequest().getInstanceGroups().add(ig);
                        } else {
                            getRequest().getInstanceGroups().add(old);
                        }
                    }

                });
        return this;
    }

    private DistroXTestDtoBase<T> withTestNameAsTag() {
        String callingMethodName = testNameExtractorUtil.getExecutingTestName();
        tagAdderUtil.addTestNameTag(getRequest().initAndGetTags().getUserDefined(), callingMethodName);
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
}
