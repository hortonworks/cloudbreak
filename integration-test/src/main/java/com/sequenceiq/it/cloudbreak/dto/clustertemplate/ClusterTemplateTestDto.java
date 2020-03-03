package com.sequenceiq.it.cloudbreak.dto.clustertemplate;

import java.util.Collection;

import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.ClusterTemplateV4Type;
import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.requests.ClusterTemplateV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.responses.ClusterTemplateV4Response;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXV1Request;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.DeletableTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.cluster.clouderamanager.DistroXClouderaManagerTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.image.DistroXImageTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.instancegroup.DistroXNetworkTestDto;
import com.sequenceiq.it.cloudbreak.util.ClusterTemplateUtil;
import com.sequenceiq.it.cloudbreak.util.ResponseUtil;

@Prototype
public class ClusterTemplateTestDto extends DeletableTestDto<ClusterTemplateV4Request, ClusterTemplateV4Response,
        ClusterTemplateTestDto, ClusterTemplateV4Response> {

    public ClusterTemplateTestDto(TestContext testContext) {
        super(new ClusterTemplateV4Request(), testContext);
    }

    public ClusterTemplateTestDto() {
        super(ClusterTemplateTestDto.class.getSimpleName().toUpperCase());
    }

    public ClusterTemplateTestDto valid() {
        return withName(getResourcePropertyProvider().getName(getCloudPlatform()))
                .withDistroXTemplate(getTestContext().init(DistroXTemplateTestDto.class, getCloudPlatform()).getRequest());
    }

    public ClusterTemplateTestDto withDistroXTemplate() {
        withDistroXTemplate(getTestContext().get(DistroXTemplateTestDto.class).getRequest());
        return this;
    }

    public ClusterTemplateTestDto withDistroXTemplateKey(String key) {
        withDistroXTemplate(((DistroXTemplateTestDto) getTestContext().get(key)).getRequest());
        return this;
    }

    public ClusterTemplateTestDto withDistroXTemplate(DistroXV1Request distroXV1Request) {
        getRequest().setDistroXTemplate(distroXV1Request);
        return this;
    }

    public ClusterTemplateTestDto withName(String name) {
        getRequest().setName(name);
        setName(name);
        return this;
    }

    public ClusterTemplateTestDto withDescription(String description) {
        getRequest().setDescription(description);
        return this;
    }

    public ClusterTemplateTestDto withType(ClusterTemplateV4Type type) {
        getRequest().setType(type);
        return this;
    }

    public ClusterTemplateTestDto withNetwork() {
        DistroXNetworkTestDto networkTestDto = getTestContext().get(DistroXNetworkTestDto.class);
        getRequest().getDistroXTemplate().setNetwork(networkTestDto.getRequest());
        return this;
    }

    public ClusterTemplateTestDto withImageSettings() {
        DistroXImageTestDto imageTestDto = getTestContext().get(DistroXImageTestDto.class);
        getRequest().getDistroXTemplate().setImage(imageTestDto.getRequest());
        return this;
    }

    public ClusterTemplateTestDto withMockedGatewayPort() {
        Integer gatewayPort = ((MockedTestContext) getTestContext()).getSparkServer().getPort();
        getRequest().getDistroXTemplate().setGatewayPort(gatewayPort);
        return this;
    }

    public ClusterTemplateTestDto withCM(String key) {
        DistroXClouderaManagerTestDto clouderaManagerTestDto = getTestContext().get(key);
        getRequest().getDistroXTemplate().getCluster().setCm(clouderaManagerTestDto.getRequest());
        return this;
    }

    @Override
    public Collection<ClusterTemplateV4Response> getAll(CloudbreakClient client) {
        return ClusterTemplateUtil.getResponseFromViews(client.getCloudbreakClient().clusterTemplateV4EndPoint().list(client.getWorkspaceId()).getResponses());
    }

    @Override
    protected String name(ClusterTemplateV4Response entity) {
        return entity.getName();
    }

    @Override
    public void delete(TestContext testContext, ClusterTemplateV4Response entity, CloudbreakClient client) {
        try {
            client.getCloudbreakClient().clusterTemplateV4EndPoint().deleteByName(client.getWorkspaceId(), entity.getName());
        } catch (Exception e) {
            LOGGER.warn("Something went wrong on {} purge. {}", entity.getName(), ResponseUtil.getErrorMessage(e), e);
        }
    }

    @Override
    public void cleanUp(TestContext context, CloudbreakClient cloudbreakClient) {
        delete(context, getResponse(), cloudbreakClient);
    }

    public Long count() {
        CloudbreakClient client = getTestContext().getCloudbreakClient();
        return (long) client.getCloudbreakClient()
                .clusterTemplateV4EndPoint()
                .list(client.getWorkspaceId()).getResponses().size();
    }
}
