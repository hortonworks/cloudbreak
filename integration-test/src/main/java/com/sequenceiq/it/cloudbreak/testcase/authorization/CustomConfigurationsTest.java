package com.sequenceiq.it.cloudbreak.testcase.authorization;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.who;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import javax.inject.Inject;
import javax.ws.rs.ForbiddenException;


import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.model.CustomConfigurationPropertyParameters;
import com.sequenceiq.it.cloudbreak.actor.CloudbreakActor;
import com.sequenceiq.it.cloudbreak.client.CustomConfigurationsTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.customconfigs.CustomConfigurationsTestDto;
import com.sequenceiq.it.cloudbreak.testcase.AbstractIntegrationTest;

public class CustomConfigurationsTest extends AbstractIntegrationTest {

    private static final Set<CustomConfigurationPropertyParameters> PROPERTIES =
            Set.of(new CustomConfigurationPropertyParameters("hive_server2_transport_mode", "all", "hiveserver2", "hive_on_tez"),
                    new CustomConfigurationPropertyParameters("core_site_safety_valve",
                            "<property><name>fs.s3a.fast.upload.buffer</name><value>disk</value></property>", null, "hdfs"));

    @Inject
    private CustomConfigurationsTestClient testClient;

    @Inject
    private CloudbreakActor cloudbreakActor;

    @Override
    protected void setupTest(TestContext testContext) {
        useRealUmsUser(testContext, AuthUserKeys.ENV_CREATOR_A);
        useRealUmsUser(testContext, AuthUserKeys.ENV_CREATOR_B);
        useRealUmsUser(testContext, AuthUserKeys.ZERO_RIGHTS);
        useRealUmsUser(testContext, AuthUserKeys.ACCOUNT_ADMIN);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "Valid custom configurations",
            when = "A valid custom configurations create request is sent",
            then = "only owner of the custom configurations should be able to delete or describe it"
    )
    public void testCustomConfigurationsAction(TestContext testContext) {
        useRealUmsUser(testContext, AuthUserKeys.ENV_CREATOR_A);
        testContext
                .given(CustomConfigurationsTestDto.class)
                .withConfigurations(PROPERTIES)
                .when(testClient.createV4())
                .when(testClient.getV4())
                .then((tc, dto, client) -> {
                    assertThat(dto.getResponse().getName()).isEqualTo(tc.get(CustomConfigurationsTestDto.class).getName());
                    return dto;
                })
                .when(testClient.listV4())
                .then((tc, dto, client) -> {
                    assertThat(dto.getCustomConfigsResponses().getResponses()).isNotEmpty();
                    return dto;
                })
                .whenException(testClient.getV4(), ForbiddenException.class, who(cloudbreakActor.useRealUmsUser(AuthUserKeys.ENV_CREATOR_B)))
                .whenException(testClient.deleteV4(), ForbiddenException.class, who(cloudbreakActor.useRealUmsUser(AuthUserKeys.ENV_CREATOR_B)))
                .whenException(testClient.getV4(), ForbiddenException.class, who(cloudbreakActor.useRealUmsUser(AuthUserKeys.ZERO_RIGHTS)))
                .whenException(testClient.deleteV4(), ForbiddenException.class, who(cloudbreakActor.useRealUmsUser(AuthUserKeys.ZERO_RIGHTS)))
                .when(testClient.deleteV4(), who(cloudbreakActor.useRealUmsUser(AuthUserKeys.ENV_CREATOR_A)))
                .when(testClient.listV4())
                .then((tc, dto, client) -> {
                    assertThat(dto.getCustomConfigsResponses()
                            .getResponses()
                            .stream()
                            .anyMatch(response -> response.getName().equals(dto.getName()))).isFalse();
                    return dto;
                })
                .validate();
    }
}
