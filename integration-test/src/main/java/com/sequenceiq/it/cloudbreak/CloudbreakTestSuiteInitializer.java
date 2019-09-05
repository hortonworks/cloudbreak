package com.sequenceiq.it.cloudbreak;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.testng.ITestContext;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;

import com.sequenceiq.cloudbreak.api.endpoint.v1.BlueprintEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v1.CredentialEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v3.StackV3Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v3.WorkspaceV3Endpoint;
import com.sequenceiq.cloudbreak.client.CloudbreakClient;
import com.sequenceiq.cloudbreak.client.CloudbreakClient.CloudbreakClientBuilder;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.SuiteContext;
import com.sequenceiq.it.cloudbreak.config.ITProps;
import com.sequenceiq.it.cloudbreak.v2.CloudbreakV2Constants;
import com.sequenceiq.it.cloudbreak.v2.CloudbreakV3Constants;
import com.sequenceiq.it.config.IntegrationTestConfiguration;
import com.sequenceiq.it.util.CleanupService;

@ContextConfiguration(classes = IntegrationTestConfiguration.class, initializers = ConfigFileApplicationContextInitializer.class)
public class CloudbreakTestSuiteInitializer extends AbstractTestNGSpringContextTests {

    private static final int WITH_TYPE_LENGTH = 4;

    private static final Logger LOG = LoggerFactory.getLogger(CloudbreakTestSuiteInitializer.class);

    @Value("${integrationtest.cloudbreak.server}")
    private String defaultCloudbreakServer;

    @Value("${integrationtest.testsuite.cleanUpOnFailure}")
    private boolean cleanUpOnFailure;

    @Value("${integrationtest.defaultBlueprintName}")
    private String defaultBlueprintName;

    @Value("${integrationtest.testsuite.skipRemainingTestsAfterOneFailed}")
    private boolean skipRemainingSuiteTestsAfterOneFailed;

    @Value("${integrationtest.cleanup.cleanupBeforeStart}")
    private boolean cleanUpBeforeStart;

    @Value("${integrationtest.ambari.defaultAmbariUser}")
    private String defaultAmbariUser;

    @Value("${integrationtest.ambari.defaultAmbariPassword}")
    private String defaultAmbariPassword;

    @Value("${integrationtest.ambari.defaultAmbariPort}")
    private String defaultAmbariPort;

    @Value("${server.contextPath:/cb}")
    private String cbRootContextPath;

    @Inject
    private ITProps itProps;

    @Inject
    private TemplateAdditionHelper templateAdditionHelper;

    @Inject
    private SuiteContext suiteContext;

    @Inject
    private CleanupService cleanUpService;

    private IntegrationTestContext itContext;

    @BeforeSuite(dependsOnGroups = "suiteInit")
    public void initContext(ITestContext testContext) throws Exception {
        // Workaround of https://jira.spring.io/browse/SPR-4072
        springTestContextBeforeTestClass();
        springTestContextPrepareTestInstance();

        itContext = suiteContext.getItContext(testContext.getSuite().getName());
    }

    @BeforeSuite(dependsOnMethods = "initContext")
    @Parameters({"cloudbreakServer", "cloudProvider", "credentialName", "instanceGroups", "hostGroups", "blueprintName",
            "stackName", "networkName", "securityGroupName", "workspaceName"})
    public void initCloudbreakSuite(@Optional("") String cloudbreakServer, @Optional("") String cloudProvider, @Optional("") String credentialName,
            @Optional("") String instanceGroups, @Optional("") String hostGroups, @Optional("") String blueprintName,
            @Optional("") String stackName, @Optional("") String networkName, @Optional("") String securityGroupName, @Optional("") String workspaceName) {
        cloudbreakServer = StringUtils.hasLength(cloudbreakServer) ? cloudbreakServer : defaultCloudbreakServer;
        String cbServerRoot = cloudbreakServer + cbRootContextPath;
        itContext.putContextParam(CloudbreakITContextConstants.SKIP_REMAINING_SUITETEST_AFTER_ONE_FAILED, skipRemainingSuiteTestsAfterOneFailed);
        itContext.putContextParam(CloudbreakITContextConstants.CLOUDBREAK_SERVER, cloudbreakServer);
        itContext.putContextParam(CloudbreakITContextConstants.CLOUDBREAK_SERVER_ROOT, cbServerRoot);
        itContext.putContextParam(CloudbreakITContextConstants.CLOUDPROVIDER, cloudProvider);
        String identity = itContext.getContextParam(IntegrationTestContext.IDENTITY_URL);
        String user = itContext.getContextParam(IntegrationTestContext.AUTH_USER);
        String password = itContext.getContextParam(IntegrationTestContext.AUTH_PASSWORD);

        CloudbreakClient cloudbreakClient = new CloudbreakClientBuilder(cbServerRoot, identity, "cloudbreak_shell")
                .withCertificateValidation(false).withIgnorePreValidation(true).withDebug(true).withCredential(user, password).build();
        itContext.putContextParam(CloudbreakITContextConstants.CLOUDBREAK_CLIENT, cloudbreakClient);
        if (cleanUpBeforeStart) {
            cleanUpService.deleteTestStacksAndResources(cloudbreakClient);
        }
        putWorkspaceToContextIfExist(
                itContext.getContextParam(CloudbreakITContextConstants.CLOUDBREAK_CLIENT, CloudbreakClient.class).workspaceV3Endpoint(), workspaceName);
        putBlueprintToContextIfExist(
                itContext.getContextParam(CloudbreakITContextConstants.CLOUDBREAK_CLIENT, CloudbreakClient.class).blueprintEndpoint(), blueprintName);
        putCredentialToContext(
                itContext.getContextParam(CloudbreakITContextConstants.CLOUDBREAK_CLIENT, CloudbreakClient.class).credentialEndpoint(), cloudProvider,
                credentialName);
        putStackToContextIfExist(
                itContext.getContextParam(CloudbreakITContextConstants.CLOUDBREAK_CLIENT, CloudbreakClient.class).stackV3Endpoint(),
                itContext.getContextParam(CloudbreakITContextConstants.WORKSPACE_ID, Long.class),
                stackName);
        if (StringUtils.hasLength(instanceGroups)) {
            List<String[]> instanceGroupStrings = templateAdditionHelper.parseCommaSeparatedRows(instanceGroups);
        }
        if (StringUtils.hasLength(hostGroups)) {
            List<String[]> hostGroupStrings = templateAdditionHelper.parseCommaSeparatedRows(hostGroups);
            itContext.putContextParam(CloudbreakITContextConstants.HOSTGROUP_ID, createHostGroups(hostGroupStrings));
        }
    }

    @BeforeSuite(dependsOnMethods = "initContext")
    @Parameters({"ambariUser", "ambariPassword", "ambariPort"})
    public void initAmbariCredentials(@Optional("") String ambariUser, @Optional("") String ambariPassword, @Optional("") String ambariPort) {
        putAmbariCredentialsToContext(ambariUser, ambariPassword, ambariPort);

    }

    private void putWorkspaceToContextIfExist(WorkspaceV3Endpoint endpoint, String workspaceName) {
        if (StringUtils.hasLength(workspaceName)) {
            Long workspaceId = endpoint.getByName(workspaceName).getId();
            itContext.putContextParam(CloudbreakITContextConstants.WORKSPACE_ID, workspaceId);
            itContext.putContextParam(CloudbreakV3Constants.WORKSPACE_NAME, workspaceName);
        }
    }

    private void putBlueprintToContextIfExist(BlueprintEndpoint endpoint, String blueprintName) {
        endpoint.getPublics();
        if (StringUtils.isEmpty(blueprintName)) {
            blueprintName = defaultBlueprintName;
        }
        if (StringUtils.hasLength(blueprintName)) {
            String resourceId = endpoint.getPublic(blueprintName).getId().toString();
            itContext.putContextParam(CloudbreakITContextConstants.BLUEPRINT_ID, resourceId);
        }
    }

    private void putAmbariCredentialsToContext(String ambariUser, String ambariPassword, String ambariPort) {
        if (StringUtils.isEmpty(ambariUser)) {
            ambariUser = defaultAmbariUser;
        }
        if (StringUtils.isEmpty(ambariPassword)) {
            ambariPassword = defaultAmbariPassword;
        }
        if (StringUtils.isEmpty(ambariPort)) {
            ambariPort = defaultAmbariPort;
        }

        itContext.putContextParam(CloudbreakITContextConstants.AMBARI_USER_ID, ambariUser);
        itContext.putContextParam(CloudbreakITContextConstants.AMBARI_PASSWORD_ID, ambariPassword);
        itContext.putContextParam(CloudbreakITContextConstants.AMBARI_PORT_ID, ambariPort);
    }

    private void putStackToContextIfExist(StackV3Endpoint endpoint, Long workspaceId, String stackName) {
        if (StringUtils.hasLength(stackName) && workspaceId != null) {
            Long resourceId = endpoint.getByNameInWorkspace(workspaceId, stackName, new HashSet<>()).getId();
            itContext.putContextParam(CloudbreakITContextConstants.STACK_ID, resourceId.toString());
            itContext.putContextParam(CloudbreakV2Constants.STACK_NAME, stackName);
        }
    }

    private void putCredentialToContext(CredentialEndpoint endpoint, String cloudProvider, String credentialName) {
        if (StringUtils.isEmpty(credentialName)) {
            String defaultCredentialName = itProps.getCredentialName(cloudProvider);
            if (!"__ignored__".equals(defaultCredentialName)) {
                credentialName = defaultCredentialName;
            }
        }
        if (StringUtils.hasLength(credentialName)) {
            Long resourceId = endpoint.getPublic(credentialName).getId();
            itContext.putContextParam(CloudbreakITContextConstants.CREDENTIAL_ID, resourceId.toString());
        }
    }

    private List<HostGroup> createHostGroups(Iterable<String[]> hostGroupStrings) {
        List<HostGroup> hostGroups = new ArrayList<>();
        for (String[] hostGroupStr : hostGroupStrings) {
            hostGroups.add(new HostGroup(hostGroupStr[0], hostGroupStr[1], Integer.valueOf(hostGroupStr[2])));
        }
        return hostGroups;
    }

    @AfterSuite(alwaysRun = true)
    @Parameters("cleanUp")
    public void cleanUp(@Optional("true") boolean cleanUp) {
        if (isCleanUpNeeded(cleanUp)) {
            CloudbreakClient cloudbreakClient = itContext.getContextParam(CloudbreakITContextConstants.CLOUDBREAK_CLIENT, CloudbreakClient.class);
            Long workspaceId = itContext.getContextParam(CloudbreakITContextConstants.WORKSPACE_ID, Long.class);
            String stackName = itContext.getContextParam(CloudbreakV2Constants.STACK_NAME);
            cleanUpService.deleteStackAndWait(cloudbreakClient, workspaceId, stackName);
            List<InstanceGroup> instanceGroups = itContext.getCleanUpParameter(CloudbreakITContextConstants.TEMPLATE_ID, List.class);
            if (instanceGroups != null && !instanceGroups.isEmpty()) {
                Collection<String> deletedTemplates = new HashSet<>();
                for (InstanceGroup ig : instanceGroups) {
                    if (!deletedTemplates.contains(ig.getTemplateId())) {
                        deletedTemplates.add(ig.getTemplateId());
                    }
                }
            }
            Set<Long> recipeIds = itContext.getContextParam(CloudbreakITContextConstants.RECIPE_ID, Set.class);
            if (recipeIds != null) {
                for (Long recipeId : recipeIds) {
                    cleanUpService.deleteRecipe(cloudbreakClient, recipeId);
                }
            }

            if (itContext.getCleanUpParameter(CloudbreakITContextConstants.CREDENTIAL_ID) != null) {
                String credentialName = itContext.getContextParam(CloudbreakV2Constants.CREDENTIAL_NAME);
                cleanUpService.deleteCredential(cloudbreakClient, workspaceId, credentialName);
            }
            cleanUpService.deleteBlueprint(cloudbreakClient, itContext.getCleanUpParameter(CloudbreakITContextConstants.BLUEPRINT_ID));
            cleanUpService.deleteRdsConfigs(cloudbreakClient, itContext.getCleanUpParameter(CloudbreakITContextConstants.RDS_CONFIG_ID));
            cleanUpService.deleteWorkspace(cloudbreakClient, itContext.getCleanUpParameter(CloudbreakV3Constants.WORKSPACE_NAME));

        }
    }

    private boolean isCleanUpNeeded(boolean cleanUp) {
        boolean noTestsFailed = CollectionUtils.isEmpty(itContext.getContextParam(CloudbreakITContextConstants.FAILED_TESTS, List.class));
        return cleanUp && (cleanUpOnFailure || noTestsFailed);
    }
}
