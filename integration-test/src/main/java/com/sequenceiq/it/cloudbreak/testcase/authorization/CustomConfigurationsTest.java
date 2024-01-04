package com.sequenceiq.it.cloudbreak.testcase.authorization;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.who;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import jakarta.inject.Inject;
import jakarta.ws.rs.ForbiddenException;

import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.model.CustomConfigurationPropertyParameters;
import com.sequenceiq.it.cloudbreak.actor.CloudbreakUser;
import com.sequenceiq.it.cloudbreak.client.CustomConfigurationsTestClient;
import com.sequenceiq.it.cloudbreak.config.user.TestUserSelectors;
import com.sequenceiq.it.cloudbreak.config.user.TestUsers;
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

    @Override
    protected void setupTest(TestContext testContext) {
        testContext.getTestUsers().setSelector(TestUserSelectors.UMS_ONLY);
        testContext.as(AuthUserKeys.ENV_CREATOR_A);
        testContext.as(AuthUserKeys.ENV_CREATOR_B);
        testContext.as(AuthUserKeys.ZERO_RIGHTS);
        testContext.as(AuthUserKeys.ACCOUNT_ADMIN);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "Valid custom configurations",
            when = "A valid custom configurations create request is sent",
            then = "only owner of the custom configurations should be able to delete or describe it"
    )
    public void testCustomConfigurationsAction(TestContext testContext) {
        TestUsers testUsers = testContext.getTestUsers();
        CloudbreakUser envCreatorB = testUsers.getUserByLabel(AuthUserKeys.ENV_CREATOR_B);
        CloudbreakUser envCreatorA = testUsers.getUserByLabel(AuthUserKeys.ENV_CREATOR_A);
        CloudbreakUser zeroRights = testUsers.getUserByLabel(AuthUserKeys.ZERO_RIGHTS);

        testContext.as(AuthUserKeys.ENV_CREATOR_A)
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
                .whenException(testClient.getV4(), ForbiddenException.class, who(envCreatorB))
                .whenException(testClient.deleteV4(), ForbiddenException.class, who(envCreatorB))
                .whenException(testClient.getV4(), ForbiddenException.class, who(zeroRights))
                .whenException(testClient.deleteV4(), ForbiddenException.class, who(zeroRights))
                .when(testClient.deleteV4(), who(envCreatorA))
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
