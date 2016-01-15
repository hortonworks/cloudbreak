package com.sequenceiq.it.cloudbreak;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.sequenceiq.cloudbreak.api.endpoint.AccountPreferencesEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.BlueprintEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.ClusterEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.ConnectorEndpoint;
import com.sequenceiq.cloudbreak.api.CoreApi;
import com.sequenceiq.cloudbreak.api.endpoint.CredentialEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.EventEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.NetworkEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.RecipeEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.SecurityGroupEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.StackEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.SubscriptionEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.TemplateEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.UsageEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.UserEndpoint;
import com.sequenceiq.cloudbreak.api.model.SecurityGroupJson;
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

    @Inject
    private ITProps itProps;
    @Inject
    private TemplateAdditionHelper templateAdditionHelper;
    @Inject
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
    @Parameters({ "cloudbreakServer", "cloudProvider", "credentialName", "instanceGroups", "hostGroups", "blueprintName",
            "stackName", "networkName", "securityGroupName" })
    public void initCloudbreakSuite(@Optional("") String cloudbreakServer, @Optional("") String cloudProvider, @Optional("") String credentialName,
                                    @Optional("") String instanceGroups, @Optional("") String hostGroups, @Optional("") String blueprintName,
                                    @Optional("") String stackName, @Optional("") String networkName, @Optional("") String securityGroupName) throws Exception {
        cloudbreakServer = StringUtils.hasLength(cloudbreakServer) ? cloudbreakServer : defaultCloudbreakServer;
        itContext.putContextParam(CloudbreakITContextConstants.SKIP_REMAINING_SUITETEST_AFTER_ONE_FAILED, skipRemainingSuiteTestsAfterOneFailed);
        itContext.putContextParam(CloudbreakITContextConstants.CLOUDBREAK_SERVER, cloudbreakServer);

        itContext.putContextParam(CloudbreakITContextConstants.ENDPOINT_ACCOUNTPREFERENCES, endPointFactory(AccountPreferencesEndpoint.class, cloudbreakServer));
        itContext.putContextParam(CloudbreakITContextConstants.ENDPOINT_CLUSTER, endPointFactory(ClusterEndpoint.class, cloudbreakServer));
        itContext.putContextParam(CloudbreakITContextConstants.ENDPOINT_CONNECTOR, endPointFactory(ConnectorEndpoint.class, cloudbreakServer));
        itContext.putContextParam(CloudbreakITContextConstants.ENDPOINT_CREDENTIAL, endPointFactory(CredentialEndpoint.class, cloudbreakServer));
        itContext.putContextParam(CloudbreakITContextConstants.ENDPOINT_EVENT, endPointFactory(EventEndpoint.class, cloudbreakServer));
        itContext.putContextParam(CloudbreakITContextConstants.ENDPOINT_NETWORK, endPointFactory(NetworkEndpoint.class, cloudbreakServer));
        itContext.putContextParam(CloudbreakITContextConstants.ENDPOINT_RECIPE, endPointFactory(RecipeEndpoint.class, cloudbreakServer));
        itContext.putContextParam(CloudbreakITContextConstants.ENDPOINT_SECURITYGROUP, endPointFactory(SecurityGroupEndpoint.class, cloudbreakServer));
        itContext.putContextParam(CloudbreakITContextConstants.ENDPOINT_STACK, endPointFactory(StackEndpoint.class, cloudbreakServer));
        itContext.putContextParam(CloudbreakITContextConstants.ENDPOINT_SUBSCRIPTION, endPointFactory(SubscriptionEndpoint.class, cloudbreakServer));
        itContext.putContextParam(CloudbreakITContextConstants.ENDPOINT_TEMPLATE, endPointFactory(TemplateEndpoint.class, cloudbreakServer));
        itContext.putContextParam(CloudbreakITContextConstants.ENDPOINT_USAGE, endPointFactory(UsageEndpoint.class, cloudbreakServer));
        itContext.putContextParam(CloudbreakITContextConstants.ENDPOINT_USER, endPointFactory(UserEndpoint.class, cloudbreakServer));
        itContext.putContextParam(CloudbreakITContextConstants.ENDPOINT_BLUEPRINT, endPointFactory(BlueprintEndpoint.class, cloudbreakServer));

        putBlueprintToContextIfExist(itContext.getContextParam(CloudbreakITContextConstants.ENDPOINT_BLUEPRINT, BlueprintEndpoint.class), blueprintName);
        putNetworkToContext(itContext.getContextParam(CloudbreakITContextConstants.ENDPOINT_NETWORK, NetworkEndpoint.class), cloudProvider, networkName);
        putSecurityGroupToContext(itContext.getContextParam(CloudbreakITContextConstants.ENDPOINT_SECURITYGROUP, SecurityGroupEndpoint.class),
                securityGroupName);
        putCredentialToContext(itContext.getContextParam(CloudbreakITContextConstants.ENDPOINT_CREDENTIAL, CredentialEndpoint.class), cloudProvider,
                credentialName);
        putStackToContextIfExist(itContext.getContextParam(CloudbreakITContextConstants.ENDPOINT_STACK, StackEndpoint.class), stackName);
        if (StringUtils.hasLength(instanceGroups)) {
            List<String[]> instanceGroupStrings = templateAdditionHelper.parseCommaSeparatedRows(instanceGroups);
            itContext.putContextParam(CloudbreakITContextConstants.TEMPLATE_ID,
                    createInstanceGroups(itContext.getContextParam(CloudbreakITContextConstants.ENDPOINT_TEMPLATE, TemplateEndpoint.class),
                            instanceGroupStrings));
        }
        if (StringUtils.hasLength(hostGroups)) {
            List<String[]> hostGroupStrings = templateAdditionHelper.parseCommaSeparatedRows(hostGroups);
            itContext.putContextParam(CloudbreakITContextConstants.HOSTGROUP_ID, createHostGroups(hostGroupStrings));
        }
    }

    private void putBlueprintToContextIfExist(BlueprintEndpoint endpoint, String blueprintName) throws Exception {
        endpoint.getPublics();
        if (StringUtils.isEmpty(blueprintName)) {
            blueprintName = defaultBlueprintName;
        }
        if (StringUtils.hasLength(blueprintName)) {
            String resourceId = endpoint.getPublic(blueprintName).getId().toString();
            if (resourceId != null) {
                itContext.putContextParam(CloudbreakITContextConstants.BLUEPRINT_ID, resourceId);
            }
        }
    }

    private <T extends Object> T endPointFactory(Class<T> clazz, String cloudbreakServer) throws Exception {
        String token = itContext.getContextParam(IntegrationTestContext.AUTH_TOKEN);
        JAXRSClientFactoryBean jaxrsClientFactoryBean = jaxrsClientFactoryBean(token, cloudbreakServer);
        jaxrsClientFactoryBean.setResourceClass(clazz);
        Object clientFactory = JAXRSClientFactory.fromClient(jaxrsClientFactoryBean.create(), clazz, true);
        return (T) clientFactory;
    }

    private JAXRSClientFactoryBean jaxrsClientFactoryBean(String token, String cloudbreakAddress) {
        JAXRSClientFactoryBean jaxrsClientFactoryBean = new JAXRSClientFactoryBean();
        String addressWithoutLastSlash = cloudbreakAddress.endsWith("/") ? cloudbreakAddress.substring(0, cloudbreakAddress.length() - 1) : cloudbreakAddress;
        String apiAddress = addressWithoutLastSlash + CoreApi.API_ROOT_CONTEXT;
        jaxrsClientFactoryBean.setAddress(apiAddress);
        jaxrsClientFactoryBean.setProvider(JacksonJsonProvider.class);
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + token);
        jaxrsClientFactoryBean.setHeaders(headers);
        return jaxrsClientFactoryBean;
    }


    private void putNetworkToContext(NetworkEndpoint endpoint, String cloudProvider, String networkName) throws Exception {
        endpoint.getPublics();
        if (StringUtils.isEmpty(networkName)) {
            String defaultNetworkName = itProps.getDefaultNetwork(cloudProvider);
            networkName = defaultNetworkName;
        }
        if (StringUtils.hasLength(networkName)) {
            String resourceId = endpoint.getPublic(networkName).getId();
            if (resourceId != null) {
                itContext.putContextParam(CloudbreakITContextConstants.NETWORK_ID, resourceId);
            }
        }
    }

    private void putSecurityGroupToContext(SecurityGroupEndpoint endpoint, String securityGroupName) throws Exception {
        endpoint.getPublics();
        if (StringUtils.isEmpty(securityGroupName)) {
            String defaultSecurityGroupName = itProps.getDefaultSecurityGroup();
            securityGroupName = defaultSecurityGroupName;
        }
        if (StringUtils.hasLength(securityGroupName)) {
            try {
                String resourceId = endpoint.getPublic(securityGroupName).getId().toString();
                if (resourceId != null) {
                    itContext.putContextParam(CloudbreakITContextConstants.SECURITY_GROUP_ID, resourceId);
                }
            } catch (Exception e) {
                LOG.warn("Could not set security group id", e);
            }
        }
    }

    private void putStackToContextIfExist(StackEndpoint endpoint, String stackName) throws Exception {
        if (StringUtils.hasLength(stackName)) {
            String resourceId = endpoint.getPublic(stackName).getId().toString();
            if (resourceId != null) {
                itContext.putContextParam(CloudbreakITContextConstants.STACK_ID, resourceId);
            }
        }
    }

    private void putCredentialToContext(CredentialEndpoint endpoint, String cloudProvider, String credentialName) throws Exception {
        if (StringUtils.isEmpty(credentialName)) {
            String defaultCredentialName = itProps.getCredentialName(cloudProvider);
            if (!"__ignored__".equals(defaultCredentialName)) {
                credentialName = defaultCredentialName;
            }
        }
        if (StringUtils.hasLength(credentialName)) {
            String resourceId = endpoint.getPublic(credentialName).getId().toString();
            if (resourceId != null) {
                itContext.putContextParam(CloudbreakITContextConstants.CREDENTIAL_ID, resourceId);
            }
        }
    }

    private List<InstanceGroup> createInstanceGroups(TemplateEndpoint endpoint, List<String[]> instanceGroupStrings) throws Exception {
        List<InstanceGroup> instanceGroups = new ArrayList<>();
        for (String[] instanceGroupStr : instanceGroupStrings) {
            String type = instanceGroupStr.length == WITH_TYPE_LENGTH ? instanceGroupStr[WITH_TYPE_LENGTH - 1] : "CORE";
            instanceGroups.add(new InstanceGroup(endpoint.getPublic(instanceGroupStr[0]).getId().toString(), instanceGroupStr[1],
                    Integer.parseInt(instanceGroupStr[2]), type));
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

    @AfterSuite(alwaysRun = true)
    @Parameters("cleanUp")
    public void cleanUp(@Optional("true") boolean cleanUp) throws Exception {
        if (isCleanUpNeeded(cleanUp)) {
            StackEndpoint client = itContext.getContextParam(CloudbreakITContextConstants.ENDPOINT_STACK, StackEndpoint.class);
            String stackId = itContext.getCleanUpParameter(CloudbreakITContextConstants.STACK_ID);
            for (int i = 0; i < cleanUpRetryCount; i++) {
                if (deleteStack(client, stackId)) {
                    WaitResult waitResult = CloudbreakUtil.waitForStackStatus(itContext, stackId, "DELETE_COMPLETED");
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
                        deleteTemplate(itContext.getContextParam(CloudbreakITContextConstants.ENDPOINT_TEMPLATE, TemplateEndpoint.class), ig.getTemplateId());
                        deletedTemplates.add(ig.getTemplateId());
                    }
                }
            }
            Set<Long> recipeIds = itContext.getContextParam(CloudbreakITContextConstants.RECIPE_ID, Set.class);
            if (recipeIds != null) {
                for (Long recipeId : recipeIds) {
                    deleteRecipe(itContext.getContextParam(CloudbreakITContextConstants.ENDPOINT_RECIPE, RecipeEndpoint.class), recipeId);
                }
            }
            deleteCredential(itContext.getContextParam(CloudbreakITContextConstants.ENDPOINT_CREDENTIAL, CredentialEndpoint.class),
                    itContext.getCleanUpParameter(CloudbreakITContextConstants.CREDENTIAL_ID));
            deleteBlueprint(itContext.getContextParam(CloudbreakITContextConstants.ENDPOINT_BLUEPRINT, BlueprintEndpoint.class),
                    itContext.getCleanUpParameter(CloudbreakITContextConstants.BLUEPRINT_ID));
            deleteNetwork(itContext.getContextParam(CloudbreakITContextConstants.ENDPOINT_NETWORK, NetworkEndpoint.class),
                    itContext.getCleanUpParameter(CloudbreakITContextConstants.NETWORK_ID));
            deleteSecurityGroup(itContext.getContextParam(CloudbreakITContextConstants.ENDPOINT_SECURITYGROUP, SecurityGroupEndpoint.class),
                    itContext.getCleanUpParameter(CloudbreakITContextConstants.SECURITY_GROUP_ID));
        }
    }

    private boolean deleteCredential(CredentialEndpoint credentialEndpoint, String credentialId) throws Exception {
        boolean result = false;
        if (credentialId != null) {
            credentialEndpoint.delete(Long.valueOf(credentialId));
            result = true;
        }
        return result;
    }

    private boolean deleteTemplate(TemplateEndpoint templateEndpoint, String templateId) throws Exception {
        boolean result = false;
        if (templateId != null) {
            templateEndpoint.delete(Long.valueOf(templateId));
            result = true;
        }
        return result;
    }

    private boolean deleteNetwork(NetworkEndpoint networkEndpoint, String networkId) throws Exception {
        boolean result = false;
        if (networkId != null) {
            networkEndpoint.delete(Long.valueOf(networkId));
            result = true;
        }
        return result;
    }

    private boolean deleteSecurityGroup(SecurityGroupEndpoint securityGroupEndpoint, String securityGroupId) throws Exception {
        boolean result = false;
        if (securityGroupId != null) {
            SecurityGroupJson securityGroupJson = securityGroupEndpoint.get(Long.valueOf(securityGroupId));
            if (!securityGroupJson.getName().equals(itProps.getDefaultSecurityGroup())) {
                securityGroupEndpoint.delete(Long.valueOf(securityGroupId));
                result = true;
            }
        }
        return result;
    }

    private boolean deleteBlueprint(BlueprintEndpoint blueprintEndpoint, String blueprintId) throws Exception {
        boolean result = false;
        if (blueprintId != null) {
            blueprintEndpoint.delete(Long.valueOf(blueprintId));
            result = true;
        }
        return result;
    }

    private boolean deleteStack(StackEndpoint client, String stackId) throws Exception {
        boolean result = false;
        if (stackId != null) {
            client.delete(Long.valueOf(stackId), false);
            result = true;
        }
        return result;
    }

    private boolean deleteRecipe(RecipeEndpoint recipeEndpoint, Long recipeId) throws Exception {
        recipeEndpoint.delete(recipeId);
        return true;
    }

    private boolean isCleanUpNeeded(boolean cleanUp) {
        boolean noTestsFailed = CollectionUtils.isEmpty(itContext.getContextParam(CloudbreakITContextConstants.FAILED_TESTS, List.class));
        return cleanUp && (cleanUpOnFailure || (!cleanUpOnFailure && noTestsFailed));
    }
}
