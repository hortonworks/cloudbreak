package com.sequenceiq.it.cloudbreak.dto.sdx;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.emptyRunningParameter;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;
import static com.sequenceiq.sdx.api.model.SdxClusterStatusResponse.DELETED;

import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.SdxClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.AbstractSdxTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.util.ResponseUtil;
import com.sequenceiq.sdx.api.model.SdxClusterRequest;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

@Prototype
public class SdxTestDto extends AbstractSdxTestDto<SdxClusterRequest, SdxClusterResponse, SdxTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxTestDto.class);

    private static final String DEFAULT_SDX_NAME = "test-sdx" + '-' + UUID.randomUUID().toString().replaceAll("-", "");

    @Inject
    private SdxTestClient sdxTestClient;

    public SdxTestDto(TestContext testContex) {
        super(new SdxClusterRequest(), testContex);
    }

    @Override
    public SdxTestDto valid() {
        withName(resourceProperyProvider().getName())
                .withEnvironment(getTestContext().get(EnvironmentTestDto.class).getName())
                .withAccessCidr(getCloudProvider().getAccessCIDR())
                .withClusterShape(getCloudProvider().getClusterShape())
                .withTags(getCloudProvider().getTags());
        return getCloudProvider().sdx(this);
    }

    public SdxTestDto withTags(Map<String, String> tags) {
        getRequest().setTags(tags);
        return this;
    }

    public SdxTestDto withClusterShape(String shape) {
        getRequest().setClusterShape(shape);
        return this;
    }

    public SdxTestDto withAccessCidr(String cidr) {
        getRequest().setAccessCidr(cidr);
        return this;
    }

    public SdxTestDto withEnvironment() {
        EnvironmentTestDto environment = getTestContext().get(EnvironmentTestDto.class);
        if (environment == null) {
            throw new IllegalArgumentException("Environment does not exist!");
        }
        return withEnvironment(environment.getName());
    }

    public SdxTestDto withEnvironment(String environment) {
        getRequest().setEnvironment(environment);
        return this;
    }

    public SdxTestDto withName(String name) {
        setName(name);
        return this;
    }

    @Override
    public String getName() {
        return super.getName() == null ? DEFAULT_SDX_NAME : super.getName();
    }

    public SdxTestDto await(SdxClusterStatusResponse status) {
        return await(status, emptyRunningParameter());
    }

    public SdxTestDto await(SdxClusterStatusResponse status, RunningParameter runningParameter) {
        return getTestContext().await(this, status, runningParameter);
    }

    public SdxTestDto refresh(TestContext context, SdxClient client) {
        LOGGER.info("Refresh resource with name: {}", getName());
        return when(sdxTestClient.describe(), key("refresh-sdx-" + getName()));
    }

    public void cleanUp(TestContext context, SdxClient client) {
        LOGGER.info("Cleaning up resource with name: {}", getName());
        when(sdxTestClient.delete(), key("delete-sdx-" + getName()));
        await(DELETED);
    }

    public boolean deletable() {
        return getName().startsWith(resourceProperyProvider().prefix());
    }

    public void delete(TestContext testContext, SdxClient client) {
        try {
            LOGGER.info("Delete resource with name: {}", getName());
            client.getSdxClient().sdxEndpoint().delete(getName());
            testContext.await(this, DELETED, key("wait-purge-sdx-" + getName()));
        } catch (Exception e) {
            LOGGER.warn("Something went wrong on {} purge. {}", getName(), ResponseUtil.getErrorMessage(e), e);
        }
    }
}
