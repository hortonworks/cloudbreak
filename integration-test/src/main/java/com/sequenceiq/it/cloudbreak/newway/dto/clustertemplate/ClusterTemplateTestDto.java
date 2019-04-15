package com.sequenceiq.it.cloudbreak.newway.dto.clustertemplate;

import static com.sequenceiq.it.cloudbreak.newway.util.ResponseUtil.getErrorMessage;

import java.util.Collection;

import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.ClusterTemplateV4Type;
import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.requests.ClusterTemplateV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.responses.ClusterTemplateV4Response;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.ClusterTemplateUtil;
import com.sequenceiq.it.cloudbreak.newway.Prototype;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.DeletableTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.stack.StackTemplateTestDto;

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
        return withName(resourceProperyProvider().getName())
                .withStackTemplate(getTestContext().init(StackTemplateTestDto.class));
    }

    public ClusterTemplateTestDto withName(String name) {
        getRequest().setName(name);
        setName(name);
        return this;
    }

    public ClusterTemplateTestDto withoutStackTemplate() {
        getRequest().setStackTemplate(null);
        return this;
    }

    public ClusterTemplateTestDto withStackTemplate(StackTemplateTestDto stackTemplate) {
        getRequest().setStackTemplate(stackTemplate.getRequest());
        return this;
    }

    public ClusterTemplateTestDto withStackTemplate(String key) {
        StackTemplateTestDto stackTemplate = getTestContext().get(key);
        getRequest().setStackTemplate(stackTemplate.getRequest());
        return this;
    }

    public ClusterTemplateTestDto withStackTemplate(Class<StackTemplateTestDto> key) {
        return withStackTemplate(key.getSimpleName());
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
            client.getCloudbreakClient().clusterTemplateV4EndPoint().delete(client.getWorkspaceId(), entity.getName());
        } catch (Exception e) {
            LOGGER.warn("Something went wrong on {} purge. {}", entity.getName(), getErrorMessage(e), e);
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
