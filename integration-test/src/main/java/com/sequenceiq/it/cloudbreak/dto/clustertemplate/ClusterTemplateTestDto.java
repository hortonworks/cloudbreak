package com.sequenceiq.it.cloudbreak.dto.clustertemplate;

import java.util.Collection;

import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.ClusterTemplateV4Type;
import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.requests.ClusterTemplateV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.responses.ClusterTemplateV4Response;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXV1Request;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.DeletableTestDto;
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
        return withName(getResourceProperyProvider().getName())
                .withDistroXTemplate(getTestContext().init(DistroXTemplateTestDto.class).getRequest());
    }

    private ClusterTemplateTestDto withDistroXTemplate(DistroXV1Request distroXV1Request) {
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
