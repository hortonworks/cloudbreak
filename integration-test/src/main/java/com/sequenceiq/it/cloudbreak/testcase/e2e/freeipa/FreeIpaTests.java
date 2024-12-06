package com.sequenceiq.it.cloudbreak.testcase.e2e.freeipa;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Type.PRE_SERVICE_DEPLOYMENT;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests.RecipeV4Type.PRE_TERMINATION;
import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.MASTER;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.waitForFlow;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;
import org.testng.annotations.Test;

import com.sequenceiq.common.model.SeLinux;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetadataType;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationState;
import com.sequenceiq.it.cloudbreak.assertion.freeipa.RecipeTestAssertion;
import com.sequenceiq.it.cloudbreak.assertion.salt.SaltHighStateDurationAssertions;
import com.sequenceiq.it.cloudbreak.client.FreeIpaTestClient;
import com.sequenceiq.it.cloudbreak.client.RecipeTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaUserSyncTestDto;
import com.sequenceiq.it.cloudbreak.dto.recipe.RecipeTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.microservice.FreeIpaClient;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;
import com.sequenceiq.it.cloudbreak.util.CloudFunctionality;
import com.sequenceiq.it.cloudbreak.util.FreeIpaInstanceUtil;
import com.sequenceiq.it.cloudbreak.util.RecipeUtil;
import com.sequenceiq.it.cloudbreak.util.ssh.SshJUtil;

public class FreeIpaTests extends AbstractE2ETest {

    protected static final Status FREEIPA_AVAILABLE = Status.AVAILABLE;

    protected static final Status FREEIPA_DELETE_COMPLETED = Status.DELETE_COMPLETED;

    private static final String EXPECTED_SELINUX_CONFIG = "selinux=enforcing";

    private static final String EXPECTED_SELINUXTYPE_CONFIG = "selinuxtype=targeted";

    private static final int EXPECTED_NUMBER_SELINUX_ENABLED_MODULES = 400;

    @Inject
    private FreeIpaTestClient freeIpaTestClient;

    @Inject
    private RecipeTestClient recipeTestClient;

    @Inject
    private SshJUtil sshJUtil;

    @Inject
    private RecipeUtil recipeUtil;

    @Inject
    private SaltHighStateDurationAssertions saltHighStateDurationAssertions;

    @Inject
    private FreeIpaInstanceUtil freeIpaInstanceUtil;

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a running cloudbreak",
            when = "a valid stack create request is sent with 2 FreeIPA instances " +
                    "AND the stack is stopped " +
                    "AND the stack is started " +
                    "AND the stack is repaired one node at a time",
            then = "the stack should be available AND deletable")
    public void testCreateStopStartRepairFreeIpaWithTwoInstances(TestContext testContext) {
        String freeIpa = resourcePropertyProvider().getName();
        String recipeName = resourcePropertyProvider().getName();
        String preTerminationRecipeName = resourcePropertyProvider().getName();
        String filePath = "/pre-service-deployment";
        String fileName = "pre-service-deployment";

        int instanceGroupCount = 1;
        int instanceCountByGroup = 2;

        testContext
                .given("preTermination", RecipeTestDto.class)
                    .withName(preTerminationRecipeName)
                    .withContent(recipeUtil.generatePreTerminationRecipeContentForE2E(applicationContext, preTerminationRecipeName))
                    .withRecipeType(PRE_TERMINATION)
                .when(recipeTestClient.createV4(), key("preTermination"))
                .given("preDeployment", RecipeTestDto.class)
                    .withName(recipeName)
                    .withContent(recipeUtil.generatePreDeploymentRecipeContent(applicationContext))
                    .withRecipeType(PRE_SERVICE_DEPLOYMENT)
                .when(recipeTestClient.createV4(), key("preDeployment"))
                .given(freeIpa, FreeIpaTestDto.class)
                    .withFreeIpaHa(instanceGroupCount, instanceCountByGroup)
                    .withSeLinuxSecurity(SeLinux.ENFORCING.name())
                    .withTelemetry("telemetry")
                    .withRecipes(Set.of(recipeName, preTerminationRecipeName))
                .when(freeIpaTestClient.create(), key(freeIpa))
                .await(FREEIPA_AVAILABLE)
                .awaitForHealthyInstances()
                .then(RecipeTestAssertion.validateFilesOnFreeIpa(filePath, fileName, 1, sshJUtil))
                .then((tc, testDto, client) -> {
                    validateSeLinux(tc, testDto, client);
                    return testDto;
                })
                .when(freeIpaTestClient.stop())
                .await(Status.STOPPED)
                .when(freeIpaTestClient.start())
                .await(Status.AVAILABLE)
                .awaitForHealthyInstances()
                .when(freeIpaTestClient.repair(InstanceMetadataType.GATEWAY_PRIMARY))
                .await(Status.UPDATE_IN_PROGRESS, waitForFlow().withWaitForFlow(Boolean.FALSE))
                .await(FREEIPA_AVAILABLE)
                .awaitForHealthyInstances()
                .then(RecipeTestAssertion.validateFilesOnFreeIpa(filePath, fileName, 1, sshJUtil))
                .when(freeIpaTestClient.repair(InstanceMetadataType.GATEWAY))
                .await(Status.UPDATE_IN_PROGRESS, waitForFlow().withWaitForFlow(Boolean.FALSE))
                .await(FREEIPA_AVAILABLE)
                .awaitForHealthyInstances()
                .then(RecipeTestAssertion.validateFilesOnFreeIpa(filePath, fileName, 1, sshJUtil))
                .given(FreeIpaUserSyncTestDto.class)
                .forAllEnvironments()
                .when(freeIpaTestClient.getLastSyncOperationStatus())
                .await(OperationState.COMPLETED)
                .when(freeIpaTestClient.syncAll())
                .await(OperationState.COMPLETED)
                .given(freeIpa, FreeIpaTestDto.class)
                .then(saltHighStateDurationAssertions::saltHighStateDurationLimits)
                .then((tc, testDto, client) -> freeIpaTestClient.delete().action(tc, testDto, client))
                .await(FREEIPA_DELETE_COMPLETED)
                .then((tc, testDto, client) -> verifyPreTerminationRecipe(tc, testDto, getBaseLocationForPreTermination(tc), preTerminationRecipeName))
                .validate();
    }

    private FreeIpaTestDto verifyPreTerminationRecipe(TestContext testContext, FreeIpaTestDto testDto, String cloudStorageBaseLocation, String recipeName) {
        testContext.getCloudProvider().getCloudFunctionality().cloudStorageListContainer(cloudStorageBaseLocation, recipeName, false);
        return testDto;
    }

    protected CloudFunctionality getCloudFunctionality(TestContext testContext) {
        return testContext.getCloudProvider().getCloudFunctionality();
    }

    private void validateSeLinux(TestContext testContext, FreeIpaTestDto freeIpaTestDto, FreeIpaClient freeIpaClient) {
        List<InstanceGroupResponse> instanceGroups = freeIpaTestDto.getResponse().getInstanceGroups();
        CloudFunctionality cloudFunctionality = getCloudFunctionality(testContext);
        // CHECK IF SELINUX IS SET TO ENFORCING ON INSTANCES
        List<String> seLinuxPoliciesForInstances = cloudFunctionality.executeSshCommandsOnInstances(instanceGroups, List.of(MASTER.name()),
                commonCloudProperties().getDefaultPrivateKeyFile(), "getenforce");
        if (CollectionUtils.isEmpty(seLinuxPoliciesForInstances) || seLinuxPoliciesForInstances.contains(SeLinux.PERMISSIVE.name().toLowerCase(Locale.ROOT))) {
            throw new TestFailException(String.format("SELinux policy was not set to 'enforced' on instances in CB for group %s",
                    MASTER.name()));
        }

        // CHECK IF SELINUX CONFIG FILES ARE UPDATED ON INSTANCES
        List<String> seLinuxConfigForInstances = cloudFunctionality.executeSshCommandsOnInstances(instanceGroups, List.of(MASTER.name()),
                commonCloudProperties().getDefaultPrivateKeyFile(), "sudo cat /etc/sysconfig/selinux");
        if (CollectionUtils.isEmpty(seLinuxConfigForInstances)) {
            throw new TestFailException(String.format("SELinux config file is missing on instances in CB for group %s",
                    MASTER.name()));
        }
        for (String config : seLinuxConfigForInstances) {
            if (StringUtils.isEmpty(config) || !config.contains(EXPECTED_SELINUX_CONFIG) || !config.contains(EXPECTED_SELINUXTYPE_CONFIG)) {
                throw new TestFailException(String.format("SELinux config file is not updated correctly on instances in CB for group %s",
                        MASTER.name()));
            }
        }

        // CHECK NUMBER OF ENABLED SELINUX MODULES
        List<String> seLinuxEnabledModulesForInstances = cloudFunctionality.executeSshCommandsOnInstances(instanceGroups, List.of(MASTER.name()),
                commonCloudProperties().getDefaultPrivateKeyFile(), "sudo semodule -l | wc -l");
        for (String moduleLength : seLinuxEnabledModulesForInstances) {
            int numOfEnabledModules = Integer.parseInt(moduleLength);
            if (numOfEnabledModules < EXPECTED_NUMBER_SELINUX_ENABLED_MODULES) {
                throw new TestFailException(String.format("SELinux enabled modules is less than expected in CB for group %s",
                        MASTER.name()));
            }
        }
    }
}
