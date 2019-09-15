package com.sequenceiq.it.cloudbreak.dto.distrox.cluster;

import static com.google.common.collect.Sets.newHashSet;

import java.util.List;
import java.util.Set;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ClusterV4Response;
import com.sequenceiq.distrox.api.v1.distrox.model.cluster.DistroXClusterV1Request;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractCloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.cluster.clouderamanager.DistroXClouderaManagerTestDto;

@Prototype
public class DistroXClusterTestDto extends AbstractCloudbreakTestDto<DistroXClusterV1Request, ClusterV4Response, DistroXClusterTestDto> {

    private static final String CLUSTER_REQUEST = "CLUSTER_REQUEST";

    public DistroXClusterTestDto(DistroXClusterV1Request request, TestContext testContex) {
        super(request, testContex);
    }

    public DistroXClusterTestDto(TestContext testContex) {
        super(new DistroXClusterV1Request(), testContex);
    }

    public DistroXClusterTestDto valid() {
        return withClouderaManager(getTestContext().init(DistroXClouderaManagerTestDto.class))
                .getCloudProvider().cluster(this)
                .withValidateBlueprint(false);
    }

    public DistroXClusterTestDto withUserName(String userName) {
        getRequest().setUserName(userName);
        return this;
    }

    public DistroXClusterTestDto withPassword(String password) {
        getRequest().setPassword(password);
        return this;
    }

    public DistroXClusterTestDto withClouderaManager() {
        DistroXClouderaManagerTestDto clouderaManager = getTestContext().get(DistroXClouderaManagerTestDto.class);
        return withClouderaManager(clouderaManager);
    }

    public DistroXClusterTestDto withClouderaManager(String key) {
        DistroXClouderaManagerTestDto clouderaManager = getTestContext().get(key);
        return withClouderaManager(clouderaManager);
    }

    public DistroXClusterTestDto withClouderaManager(DistroXClouderaManagerTestDto clouderaManagerTestDto) {
        getRequest().setCm(clouderaManagerTestDto.getRequest());
        return this;
    }

    public DistroXClusterTestDto withProxyName(String proxyName) {
        getRequest().setProxy(proxyName);
        return this;
    }

    public DistroXClusterTestDto withRdsConfigNames(String... names) {
        getRequest().setDatabases(newHashSet(names));
        return this;
    }

    public DistroXClusterTestDto withRdsConfigNames(Set<String> names) {
        getRequest().setDatabases(names);
        return this;
    }

    public DistroXClusterTestDto withExposedServices(List<String> exposedServices) {
        getRequest().setExposedServices(exposedServices);
        return this;
    }

    public DistroXClusterTestDto withDatabase(String databaseName) {
        getRequest().getDatabases().add(databaseName);
        return this;
    }

    public DistroXClusterTestDto withBlueprintName(String blueprintName) {
        getRequest().setBlueprintName(blueprintName);
        return this;
    }

    public DistroXClusterTestDto withValidateBlueprint(boolean validateBlueprint) {
        getRequest().setValidateBlueprint(validateBlueprint);
        return this;
    }

}

