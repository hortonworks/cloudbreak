package com.sequenceiq.it.cloudbreak.testcase.e2e.distrox;

import static com.sequenceiq.it.cloudbreak.assertion.distrox.DistroXExternalDatabaseAssertion.validateTemplateContainsExternalDatabaseHostname;

import java.util.List;

import javax.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.distrox.api.v1.distrox.model.database.DistroXDatabaseAvailabilityType;
import com.sequenceiq.it.TestParameter;
import com.sequenceiq.it.cloudbreak.actor.Actor;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.externaldatabase.DistroXExternalDatabaseTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.instancegroup.DistroXInstanceGroupTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.instancegroup.DistroXInstanceGroupsBuilder;
import com.sequenceiq.it.cloudbreak.testcase.e2e.ImageValidatorE2ETest;

public class DistroXTest extends ImageValidatorE2ETest {

    private static final String DIX_EXTDB_KEY = "distroxExternalDatabaseKey";

    @Inject
    private DistroXTestClient distroXTestClient;

    @Inject
    private TestParameter testParameter;

    @Override
    protected void setupTest(TestContext testContext) {
        testContext.getCloudProvider().getCloudFunctionality().cloudStorageInitialize();
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        createEnvironmentWithNetworkAndFreeIPA(testContext);
        initializeDefaultBlueprints(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a running cloudbreak",
            when = "a valid DistroX create request is sent",
            then = "DistroX cluster with external database is created")
    public void testCreateDistroX(TestContext testContext) {
        testContext.given(DIX_EXTDB_KEY, DistroXExternalDatabaseTestDto.class)
                .withAvailabilityType(DistroXDatabaseAvailabilityType.NON_HA)
                .given(DistroXTestDto.class)
                .withExternalDatabase(DIX_EXTDB_KEY)
                .when(distroXTestClient.create())
                .await(STACK_AVAILABLE)
                .then(validateTemplateContainsExternalDatabaseHostname(Actor.defaultUser(testParameter)))
                .when(distroXTestClient.get())
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a running cloudbreak",
            when = "a valid DistroX create request is sent with encrypted discs",
            then = "DistroX cluster is created")
    public void testCreateDistroXWithEncryptedVolume(TestContext testContext) {
        List<DistroXInstanceGroupTestDto> distroXInstanceGroupTestDtos = new DistroXInstanceGroupsBuilder(testContext)
                .defaultHostGroup()
                .withDiskEncryption()
                .build();
        testContext.given(DistroXTestDto.class)
                .withInstanceGroupsEntity(distroXInstanceGroupTestDtos)
                .when(distroXTestClient.create())
                .await(STACK_AVAILABLE)
                .then((context, distrox, client) -> {
                    distrox.getResponse();
                    return distrox;
                })
                .validate();
    }

    @Override
    protected String getImageId(TestContext testContext) {
        return testContext.get(DistroXTestDto.class).getResponse().getImage().getId();
    }
}
