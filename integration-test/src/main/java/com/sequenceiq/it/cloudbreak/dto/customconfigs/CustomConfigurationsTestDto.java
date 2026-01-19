package com.sequenceiq.it.cloudbreak.dto.customconfigs;

import java.util.Collection;
import java.util.Set;

import com.sequenceiq.cloudbreak.api.endpoint.v4.requests.CustomConfigurationsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.responses.CustomConfigurationsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.responses.CustomConfigurationsV4Responses;
import com.sequenceiq.cloudbreak.api.model.CustomConfigurationPropertyParameters;
import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.DeletableTestDto;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.util.ResponseUtil;

@Prototype
public class CustomConfigurationsTestDto extends
        DeletableTestDto<CustomConfigurationsV4Request, CustomConfigurationsV4Response, CustomConfigurationsTestDto, CustomConfigurationsV4Response> {

    private static final String CUSTOM_CONFIGURATIONS_NAME = "CustomConfigurationsName";

    private static final Set<CustomConfigurationPropertyParameters> PROPERTIES =
            Set.of(new CustomConfigurationPropertyParameters("hive_server2_transport_mode", "all", "hiveserver2", "hive_on_tez"),
            new CustomConfigurationPropertyParameters("core_site_safety_valve",
                    "<property><name>fs.s3a.fast.upload.buffer</name><value>disk</value></property>", null, "hdfs"));

    private CustomConfigurationsV4Responses customConfigsResponses;

    protected CustomConfigurationsTestDto(TestContext testContext) {
        super(new CustomConfigurationsV4Request(), testContext);
    }

    public CustomConfigurationsTestDto withName(String name) {
        getRequest().setName(name);
        setName(name);
        return this;
    }

    public CustomConfigurationsTestDto withConfigurations(Set<CustomConfigurationPropertyParameters> configs) {
        getRequest().setConfigurations(configs);
        return this;
    }

    public CustomConfigurationsTestDto withRuntimeVersion(String runtimeVersion) {
        getRequest().setRuntimeVersion(runtimeVersion);
        return this;
    }

    @Override
    public Collection<CustomConfigurationsV4Response> getAll(CloudbreakClient client) {
        return client.getDefaultClient(getTestContext()).customConfigurationsV4Endpoint().list().getResponses();
    }

    @Override
    public void delete(TestContext testContext, CustomConfigurationsV4Response entity, CloudbreakClient client) {
        try {
            client.getDefaultClient(getTestContext()).customConfigurationsV4Endpoint().deleteByName(entity.getName());
        } catch (Exception e) {
            LOGGER.warn("Something went wrong during delete operation for custom configurations {}, Cause: {}", entity, ResponseUtil.getErrorMessage(e), e);
        }
    }

    @Override
    public void deleteForCleanup() {
        getClientForCleanup().getDefaultClient(getTestContext()).customConfigurationsV4Endpoint().deleteByCrn(getCrn());
    }

    @Override
    public String getCrn() {
        return getResponse().getCrn();
    }

    @Override
    public String getName() {
        return getRequest().getName();
    }

    @Override
    public String getResourceNameType() {
        return CUSTOM_CONFIGURATIONS_NAME;
    }

    @Override
    protected String name(CustomConfigurationsV4Response entity) {
        return entity.getName();
    }

    @Override
    public CustomConfigurationsTestDto valid() {
        return withName(getResourcePropertyProvider().getName(getCloudPlatform()))
                .withConfigurations(PROPERTIES);
    }

    public CustomConfigurationsV4Responses getCustomConfigsResponses() {
        return customConfigsResponses;
    }

    public void setCustomConfigsResponses(CustomConfigurationsV4Responses customConfigsResponses) {
        this.customConfigsResponses = customConfigsResponses;
    }
}
