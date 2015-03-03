package com.sequenceiq.it.cloudbreak;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.util.StringUtils;
import org.testng.ITestContext;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;

import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.SuiteContext;
import com.sequenceiq.it.config.IntegrationTestConfiguration;
import com.sequenceiq.it.util.RestUtil;

@ContextConfiguration(classes = IntegrationTestConfiguration.class)
public class CloudbreakTestSuiteInitializer extends AbstractTestNGSpringContextTests {
    @Value("${integrationtest.cloudbreak.server:}")
    private String defaultCloudbreakServer;

    @Autowired
    private TemplateAdditionParser templateAdditionParser;
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
    @Parameters({ "cloudbreakServer", "credentialName", "instanceGroups", "hostGroups", "blueprintName", "stackName" })
    public void initCloudbreakSuite(@Optional("") String cloudbreakServer, @Optional("") String credentialName,
            @Optional("") String instanceGroups, @Optional("") String hostGroups, @Optional("") String blueprintName, @Optional("") String stackName) {
        cloudbreakServer = StringUtils.hasLength(cloudbreakServer) ? cloudbreakServer : defaultCloudbreakServer;
        itContext.putContextParam(CloudbreakITContextConstants.CLOUDBREAK_SERVER, cloudbreakServer);
        putResourceToContextIfExist(CloudbreakITContextConstants.BLUEPRINT_ID, "/account/blueprints/{name}", blueprintName);
        putResourceToContextIfExist(CloudbreakITContextConstants.CREDENTIAL_ID, "/account/credentials/{name}", credentialName);
        putResourceToContextIfExist(CloudbreakITContextConstants.STACK_ID, "/account/stacks/{name}", stackName);
        if (StringUtils.hasLength(instanceGroups)) {
            List<String[]> instanceGroupStrings = templateAdditionParser.parseCommaSeparatedRows(instanceGroups);
            itContext.putContextParam(CloudbreakITContextConstants.TEMPLATE_ID, createInstanceGroups(instanceGroupStrings));
        }
        if (StringUtils.hasLength(hostGroups)) {
            List<String[]> hostGroupStrings = templateAdditionParser.parseCommaSeparatedRows(hostGroups);
            itContext.putContextParam(CloudbreakITContextConstants.HOSTGROUP_ID, createHostGroups(hostGroupStrings));
        }
    }

    private List<InstanceGroup> createInstanceGroups(List<String[]> instanceGroupStrings) {
        List<InstanceGroup> instanceGroups = new ArrayList<>();
        for (String[] instanceGroupStr : instanceGroupStrings) {
            instanceGroups.add(new InstanceGroup(getResourceIdByName("/account/templates/{name}", instanceGroupStr[0]), instanceGroupStr[1],
                    Integer.parseInt(instanceGroupStr[2])));
        }
        return instanceGroups;
    }

    private List<HostGroup> createHostGroups(List<String[]> hostGroupStrings) {
        List<HostGroup> hostGroups = new ArrayList<>();
        for (String[] hostGroupStr : hostGroupStrings) {
            hostGroups.add(new HostGroup(hostGroupStr[0], hostGroupStr[1]));
        }
        return hostGroups;
    }

    private void putResourceToContextIfExist(String contextId, String resourcePath, String resourceName) {
        if (StringUtils.hasLength(resourceName)) {
            String resourceId = getResourceIdByName(resourcePath, resourceName);
            if (resourceId != null) {
                itContext.putContextParam(contextId, resourceId);
            }
        }

    }

    @AfterSuite
    @Parameters("cleanUp")
    public void cleanUp(@Optional("true") boolean cleanUp) {
        if (cleanUp) {
            String stackId = itContext.getCleanUpParameter(CloudbreakITContextConstants.STACK_ID);
            if (deleteResource("/stacks/{stackId}", "stackId", stackId)) {
                CloudbreakUtil.waitForStackStatus(itContext, stackId, "DELETE_COMPLETED");
            }
            List<InstanceGroup> instanceGroups = itContext.getCleanUpParameter(CloudbreakITContextConstants.TEMPLATE_ID, List.class);
            if (instanceGroups != null && !instanceGroups.isEmpty()) {
                Set<String> deletedTemplates = new HashSet<>();
                for (InstanceGroup ig : instanceGroups) {
                    if (!deletedTemplates.contains(ig.getTemplateId())) {
                        deleteResource("/templates/{templateId}", "templateId", ig.getTemplateId());
                        deletedTemplates.add(ig.getTemplateId());
                    }
                }
            }
            deleteResource("/credentials/{credentialId}", "credentialId", itContext.getCleanUpParameter(CloudbreakITContextConstants.CREDENTIAL_ID));
            deleteResource("/blueprints/{blueprintId}", "blueprintId", itContext.getCleanUpParameter(CloudbreakITContextConstants.BLUEPRINT_ID));
        }
    }

    private boolean deleteResource(String resourcePath, String resourceIdKey, String resourceId) {
        boolean result = false;
        if (resourceId != null) {
            RestUtil.entityPathRequest(itContext.getContextParam(CloudbreakITContextConstants.CLOUDBREAK_SERVER),
                    itContext.getContextParam(IntegrationTestContext.AUTH_TOKEN),
                    resourceIdKey, resourceId).delete(resourcePath);
            result = true;
        }
        return result;
    }

    private String getResourceIdByName(String resourcePath, String name) {
        return RestUtil.getResourceIdByName(itContext.getContextParam(CloudbreakITContextConstants.CLOUDBREAK_SERVER),
                itContext.getContextParam(IntegrationTestContext.AUTH_TOKEN), resourcePath, name);
    }
}
