package com.sequenceiq.it.cloudbreak.newway.dto.kubernetes;

import static com.sequenceiq.it.cloudbreak.newway.util.ResponseUtil.getErrorMessage;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.ws.rs.WebApplicationException;

import com.sequenceiq.cloudbreak.api.endpoint.v4.kubernetes.KubernetesV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kubernetes.requests.KubernetesV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kubernetes.responses.KubernetesV4Response;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.exception.ProxyMethodInvocationException;
import com.sequenceiq.it.cloudbreak.newway.Assertion;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.GherkinTest;
import com.sequenceiq.it.cloudbreak.newway.Prototype;
import com.sequenceiq.it.cloudbreak.newway.ResourceAction;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.DeletableTestDto;
import com.sequenceiq.it.cloudbreak.newway.v4.KubernetesAction;

@Prototype
public class KubernetesTestDto extends DeletableTestDto<KubernetesV4Request, KubernetesV4Response, KubernetesTestDto, KubernetesV4Response> {

    public static final String KERBEROS = "KERBEROS";

    KubernetesTestDto(String newId) {
        super(newId);
        setRequest(new KubernetesV4Request());
    }

    KubernetesTestDto() {
        this(KERBEROS);
    }

    public KubernetesTestDto(TestContext testContext) {
        super(new KubernetesV4Request(), testContext);
    }

    @Override
    public void cleanUp(TestContext context, CloudbreakClient cloudbreakClient) {
        LOGGER.info("Cleaning up resource with name: {}", getName());
        try {
            cloudbreakClient.getCloudbreakClient().kubernetesV4Endpoint().delete(cloudbreakClient.getWorkspaceId(), getName());
        } catch (WebApplicationException ignore) {
            LOGGER.info("Something happend.");
        }
    }

    public KubernetesTestDto valid() {
        return withName(resourceProperyProvider().getName())
                .withContent("content")
                .withDesription("great kubernetes config");
    }

    public KubernetesTestDto withRequest(KubernetesV4Request request) {
        setRequest(request);
        return this;
    }

    public KubernetesTestDto withName(String name) {
        getRequest().setName(name);
        setName(name);
        return this;
    }

    public KubernetesTestDto withContent(String content) {
        getRequest().setContent(content);
        return this;
    }

    public KubernetesTestDto withDesription(String desription) {
        getRequest().setDescription(desription);
        return this;
    }

    @Override
    public List<KubernetesV4Response> getAll(CloudbreakClient client) {
        KubernetesV4Endpoint kubernetesV4Endpoint = client.getCloudbreakClient().kubernetesV4Endpoint();
        return kubernetesV4Endpoint.list(client.getWorkspaceId(), null, Boolean.FALSE).getResponses().stream()
                .filter(s -> s.getName() != null)
                .collect(Collectors.toList());
    }

    @Override
    protected String name(KubernetesV4Response entity) {
        return entity.getName();
    }

    @Override
    public void delete(TestContext testContext, KubernetesV4Response entity, CloudbreakClient client) {
        try {
            client.getCloudbreakClient().kubernetesV4Endpoint().delete(client.getWorkspaceId(), entity.getName());
        } catch (ProxyMethodInvocationException e) {
            LOGGER.warn("Something went wrong on {} purge. {}", entity.getName(), getErrorMessage(e), e);
        }
    }

    @Override
    public int order() {
        return 500;
    }

    private static Function<IntegrationTestContext, KubernetesTestDto> getTestContext(String key) {
        return testContext -> testContext.getContextParam(key, KubernetesTestDto.class);
    }

    static Function<IntegrationTestContext, KubernetesTestDto> getNew() {
        return testContext -> new KubernetesTestDto();
    }

    public static KubernetesTestDto request() {
        return new KubernetesTestDto();
    }

    public static KubernetesTestDto isCreated(String id) {
        var kubernetesEntity = new KubernetesTestDto();
        kubernetesEntity.setCreationStrategy(KubernetesAction::createInGiven);
        return kubernetesEntity;
    }

    public static ResourceAction getAll() {
        return new ResourceAction(getNew(), KubernetesAction::getAll);
    }

    public static ResourceAction delete(String key) {
        return new ResourceAction(getTestContext(key), KubernetesAction::delete);
    }

    public static ResourceAction delete() {
        return delete(KERBEROS);
    }

    public static KubernetesTestDto delete(TestContext testContext, KubernetesTestDto entity, CloudbreakClient cloudbreakClient) {
        entity.setResponse(
                cloudbreakClient.getCloudbreakClient().kubernetesV4Endpoint()
                        .delete(cloudbreakClient.getWorkspaceId(), entity.getName())
        );
        return entity;
    }

    public static Assertion<KubernetesTestDto> assertThis(BiConsumer<KubernetesTestDto, IntegrationTestContext> check) {
        return new Assertion<>(getTestContext(GherkinTest.RESULT), check);
    }

}