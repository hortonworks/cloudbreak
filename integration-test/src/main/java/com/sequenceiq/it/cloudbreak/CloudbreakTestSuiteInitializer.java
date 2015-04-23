package com.sequenceiq.it.cloudbreak;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.ConfigFileApplicationContextInitializer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.testng.ITestContext;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;

import com.sequenceiq.cloudbreak.client.CloudbreakClient;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.SuiteContext;
import com.sequenceiq.it.cloudbreak.config.ITProps;
import com.sequenceiq.it.config.IntegrationTestConfiguration;

@ContextConfiguration(classes = IntegrationTestConfiguration.class, initializers = ConfigFileApplicationContextInitializer.class)
public class CloudbreakTestSuiteInitializer extends AbstractTestNGSpringContextTests {
    private static final int DELETE_SLEEP = 30000;
    private static final int WITH_TYPE_LENGTH = 4;
    private static final Logger LOG = LoggerFactory.getLogger(CloudbreakTestSuiteInitializer.class);

    @Value("${integrationtest.cloudbreak.server}")
    private String defaultCloudbreakServer;
    @Value("${integrationtest.cleanup.retryCount}")
    private int cleanUpRetryCount;
    @Value("${integrationtest.defaultBlueprintName}")
    private String defaultBlueprintName;
    @Value("${integrationtest.testsuite.skipRemainingTestsAfterOneFailed}")
    private boolean skipRemainingSuiteTestsAfterOneFailed;
    @Value("${integrationtest.testsuite.cleanUpOnFailure}")
    private boolean cleanUpOnFailure;

    @Autowired
    private ITProps itProps;
    @Autowired
    private TemplateAdditionHelper templateAdditionHelper;
    @Autowired
    private SuiteContext suiteContext;
    private IntegrationTestContext itContext;

    @BeforeSuite(dependsOnGroups = "suiteInit")
    public void initContext(ITestContext testContext) throws Exception {
        // Workaround of https://jira.spring.io/browse/SPR-4072
        springTestContextBeforeTestClass();
        springTestContextPrepareTestInstance();

        itContext = suiteContext.getItContext(testContext.getSuite().getName());
    }

    @BeforeSuite(dependsOnMethods = "initContext")
    @Parameters({ "cloudbreakServer", "cloudProvider", "credentialName", "instanceGroups", "hostGroups", "blueprintName", "stackName" })
    public void initCloudbreakSuite(@Optional("") String cloudbreakServer, @Optional("") String cloudProvider, @Optional("") String credentialName,
            @Optional("") String instanceGroups, @Optional("") String hostGroups, @Optional("") String blueprintName, @Optional("") String stackName)
            throws Exception {
        cloudbreakServer = StringUtils.hasLength(cloudbreakServer) ? cloudbreakServer : defaultCloudbreakServer;
        itContext.putContextParam(CloudbreakITContextConstants.SKIP_REMAINING_SUITETEST_AFTER_ONE_FAILED, skipRemainingSuiteTestsAfterOneFailed);
        itContext.putContextParam(CloudbreakITContextConstants.CLOUDBREAK_SERVER, cloudbreakServer);
        CloudbreakClient client = new CloudbreakClient(cloudbreakServer, itContext.getContextParam(IntegrationTestContext.AUTH_TOKEN));
        itContext.putContextParam(CloudbreakITContextConstants.CLOUDBREAK_CLIENT, client);
        putBlueprintToContextIfExist(client, blueprintName);
        putCredentialToContext(client, cloudProvider, credentialName);
        putStackToContextIfExist(client, stackName);
        if (StringUtils.hasLength(instanceGroups)) {
            List<String[]> instanceGroupStrings = templateAdditionHelper.parseCommaSeparatedRows(instanceGroups);
            itContext.putContextParam(CloudbreakITContextConstants.TEMPLATE_ID, createInstanceGroups(client, instanceGroupStrings));
        }
        if (StringUtils.hasLength(hostGroups)) {
            List<String[]> hostGroupStrings = templateAdditionHelper.parseCommaSeparatedRows(hostGroups);
            itContext.putContextParam(CloudbreakITContextConstants.HOSTGROUP_ID, createHostGroups(hostGroupStrings));
        }
    }

    private void putBlueprintToContextIfExist(CloudbreakClient client, String blueprintName) throws Exception {
        if (StringUtils.isEmpty(blueprintName)) {
            blueprintName = defaultBlueprintName;
        }
        if (StringUtils.hasLength(blueprintName)) {
            String resourceId = getId(client.getBlueprintByName(blueprintName));
            if (resourceId != null) {
                itContext.putContextParam(CloudbreakITContextConstants.BLUEPRINT_ID, resourceId);
            }
        }
    }

    private void putStackToContextIfExist(CloudbreakClient client, String stackName) throws Exception {
        if (StringUtils.hasLength(stackName)) {
            String resourceId = getId(client.getStackByName(stackName));
            if (resourceId != null) {
                itContext.putContextParam(CloudbreakITContextConstants.STACK_ID, resourceId);
            }
        }
    }

    private void putCredentialToContext(CloudbreakClient client, String cloudProvider, String credentialName) throws Exception {
        if (StringUtils.isEmpty(credentialName)) {
            String defaultCredentialName =  itProps.getCredentialName(cloudProvider);
            if (!"__ignored__".equals(defaultCredentialName)) {
                credentialName = defaultCredentialName;
            }
        }
        if (StringUtils.hasLength(credentialName)) {
            String resourceId = getId(client.getCredentialByName(credentialName));
            if (resourceId != null) {
                itContext.putContextParam(CloudbreakITContextConstants.CREDENTIAL_ID, resourceId);
            }
        }
    }

    private List<InstanceGroup> createInstanceGroups(CloudbreakClient client, List<String[]> instanceGroupStrings) throws Exception {
        List<InstanceGroup> instanceGroups = new ArrayList<>();
        for (String[] instanceGroupStr : instanceGroupStrings) {
            String type = instanceGroupStr.length == WITH_TYPE_LENGTH ? instanceGroupStr[WITH_TYPE_LENGTH - 1] : "HOSTGROUP";
            instanceGroups.add(new InstanceGroup(getId(client.getTemplateByName(instanceGroupStr[0])), instanceGroupStr[1],
                    Integer.parseInt(instanceGroupStr[2]), type));
        }
        return instanceGroups;
    }

    private String getId(Object responseMap) {
        return ((Map<String, Object>) responseMap).get("id").toString();
    }

    private List<HostGroup> createHostGroups(List<String[]> hostGroupStrings) {
        List<HostGroup> hostGroups = new ArrayList<>();
        for (String[] hostGroupStr : hostGroupStrings) {
            hostGroups.add(new HostGroup(hostGroupStr[0], hostGroupStr[1]));
        }
        return hostGroups;
    }

    @AfterSuite(alwaysRun = true)
    @Parameters("cleanUp")
    public void cleanUp(@Optional("true") boolean cleanUp) throws Exception {
        if (isCleanUpNeeded(cleanUp)) {
            CloudbreakClient client = itContext.getContextParam(CloudbreakITContextConstants.CLOUDBREAK_CLIENT, CloudbreakClient.class);
            String stackId = itContext.getCleanUpParameter(CloudbreakITContextConstants.STACK_ID);
            for (int i = 0; i < cleanUpRetryCount; i++) {
                if (deleteStack(client, stackId)) {
                    WaitResult waitResult = CloudbreakUtil.waitForStatus(itContext, stackId, "DELETE_COMPLETED");
                    if (waitResult == WaitResult.SUCCESSFUL) {
                        break;
                    }
                    try {
                        Thread.sleep(DELETE_SLEEP);
                    } catch (InterruptedException e) {
                        LOG.warn("interrupted ex", e);
                    }
                }
            }
            List<InstanceGroup> instanceGroups = itContext.getCleanUpParameter(CloudbreakITContextConstants.TEMPLATE_ID, List.class);
            if (instanceGroups != null && !instanceGroups.isEmpty()) {
                Set<String> deletedTemplates = new HashSet<>();
                for (InstanceGroup ig : instanceGroups) {
                    if (!deletedTemplates.contains(ig.getTemplateId())) {
                        deleteTemplate(client, ig.getTemplateId());
                        deletedTemplates.add(ig.getTemplateId());
                    }
                }
            }
            deleteCredential(client, itContext.getCleanUpParameter(CloudbreakITContextConstants.CREDENTIAL_ID));
            deleteBlueprint(client, itContext.getCleanUpParameter(CloudbreakITContextConstants.BLUEPRINT_ID));
        }
    }

    private boolean deleteCredential(CloudbreakClient client, String credentialId) throws Exception {
        boolean result = false;
        if (credentialId != null) {
            client.deleteCredential(credentialId);
            result = true;
        }
        return result;
    }

    private boolean deleteTemplate(CloudbreakClient client, String templateId) throws Exception {
        boolean result = false;
        if (templateId != null) {
            client.deleteTemplate(templateId);
            result = true;
        }
        return result;
    }

    private boolean deleteBlueprint(CloudbreakClient client, String blueprintId) throws Exception {
        boolean result = false;
        if (blueprintId != null) {
            client.deleteBlueprint(blueprintId);
            result = true;
        }
        return result;
    }

    private boolean deleteStack(CloudbreakClient client, String stackId) throws Exception {
        boolean result = false;
        if (stackId != null) {
            client.deleteStack(stackId);
            result = true;
        }
        return result;
    }

    private boolean isCleanUpNeeded(boolean cleanUp) {
        boolean noTestsFailed = CollectionUtils.isEmpty(itContext.getContextParam(CloudbreakITContextConstants.FAILED_TESTS, List.class));
        return cleanUp && (cleanUpOnFailure || (!cleanUpOnFailure && noTestsFailed));
    }
}
