package com.sequenceiq.it.cloudbreak.newway.entity;

import static com.sequenceiq.it.cloudbreak.newway.util.ResponseUtil.getErrorMessage;

import java.util.Collection;

import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.ClusterTemplateV4Type;
import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.requests.ClusterTemplateV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.responses.ClusterTemplateV4Response;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.ClusterTemplateUtil;
import com.sequenceiq.it.cloudbreak.newway.Prototype;
import com.sequenceiq.it.cloudbreak.newway.RandomNameCreator;
import com.sequenceiq.it.cloudbreak.newway.context.Purgable;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

@Prototype
public class ClusterTemplateEntity extends AbstractCloudbreakEntity<ClusterTemplateV4Request, ClusterTemplateV4Response, ClusterTemplateEntity>
        implements Purgable<ClusterTemplateV4Response> {

    public ClusterTemplateEntity(TestContext testContext) {
        super(new ClusterTemplateV4Request(), testContext);
    }

    public ClusterTemplateEntity() {
        super(ClusterTemplateEntity.class.getSimpleName().toUpperCase());
    }

    public ClusterTemplateEntity valid() {
        return withName(getNameCreator().getRandomNameForResource())
                .withStackTemplate(getTestContext().init(StackTemplateEntity.class));
    }

    public ClusterTemplateEntity withName(String name) {
        getRequest().setName(name);
        setName(name);
        return this;
    }

    public ClusterTemplateEntity withoutStackTemplate() {
        getRequest().setStackTemplate(null);
        return this;
    }

    public ClusterTemplateEntity withStackTemplate(StackTemplateEntity stackTemplate) {
        getRequest().setStackTemplate(stackTemplate.getRequest());
        return this;
    }

    public ClusterTemplateEntity withStackTemplate(String key) {
        StackTemplateEntity stackTemplate = getTestContext().get(key);
        getRequest().setStackTemplate(stackTemplate.getRequest());
        return this;
    }

    public ClusterTemplateEntity withDescription(String description) {
        getRequest().setDescription(description);
        return this;
    }

    public ClusterTemplateEntity withType(ClusterTemplateV4Type type) {
        getRequest().setType(type);
        return this;
    }

    @Override
    public Collection<ClusterTemplateV4Response> getAll(CloudbreakClient client) {
        return ClusterTemplateUtil.getResponseFromViews(client.getCloudbreakClient().clusterTemplateV4EndPoint().list(client.getWorkspaceId()).getResponses());
    }

    @Override
    public boolean deletable(ClusterTemplateV4Response entity) {
        return entity.getName().startsWith(RandomNameCreator.PREFIX);
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
