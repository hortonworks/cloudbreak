package com.sequenceiq.it.cloudbreak;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.util.StringUtils;
import org.testng.ITestContext;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;

import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.SuiteContext;
import com.sequenceiq.it.config.IntegrationTestConfiguration;

@ContextConfiguration(classes = IntegrationTestConfiguration.class, initializers = ConfigFileApplicationContextInitializer.class)
public class CloudProviderParamsInitializer extends AbstractTestNGSpringContextTests {
    @Value("${integrationtest.azurermcredential.subscriptionId}")
    private String defaultAzureSubscriptionId;

    @Value("${integrationtest.azurermcredential.secretKey}")
    private String defaultAzureSecretKey;

    @Value("${integrationtest.azurermcredential.accessKey}")
    private String defaultAzureAccesKey;

    @Value("${integrationtest.azurermcredential.tenantId}")
    private String defaultAzureTenantId;

    @Value("${integrationtest.openstackcredential.tenantName}")
    private String defaultOpenstackTenantName;

    @Value("${integrationtest.openstackcredential.userName}")
    private String defaultOpenstackUserName;

    @Value("${integrationtest.openstackcredential.password}")
    private String defaultOpenstackPassword;

    @Value("${integrationtest.openstackcredential.endpoint}")
    private String defaultOpenstackEndpoint;

    @Value("${integrationtest.gcpcredential.name}")
    private String defaultGcpName;

    @Value("${integrationtest.gcpcredential.projectId}")
    private String defaultGcpProjectId;

    @Value("${integrationtest.gcpcredential.serviceAccountId}")
    private String defaultGcpServiceAccountId;

    @Value("${integrationtest.gcpcredential.p12File}")
    private String defaultGcpP12File;

    @Inject
    private SuiteContext suiteContext;

    private IntegrationTestContext itContext;

    private Map<String, String> cloudProviderParams;

    @BeforeSuite(dependsOnGroups = "suiteInit")
    public void initContext(ITestContext testContext) throws Exception {
        // Workaround of https://jira.spring.io/browse/SPR-4072
        springTestContextBeforeTestClass();
        springTestContextPrepareTestInstance();

        itContext = suiteContext.getItContext(testContext.getSuite().getName());
        cloudProviderParams = new HashMap<>();
        suiteContext.getItContext(testContext.getSuite().getName()).putContextParam(CloudbreakITContextConstants.CLOUDPROVIDER_PARAMETERS, cloudProviderParams);
    }

    @BeforeSuite(dependsOnMethods = "initContext")
    @Parameters({"cloudProvider", "azureSubscriptionId", "azureSecretKey", "azureAccesKey", "azureTenantId"})
    public void initAzureCloudProviderParameters(String cloudProvider, @Optional("") String azureSubscriptionId, @Optional ("") String azureSecretKey,
            @Optional ("") String azureAccesKey, @Optional ("") String azureTenantId) throws Exception {
        if ("AZURE".equals(cloudProvider)) {
            azureSubscriptionId = StringUtils.hasLength(azureSubscriptionId) ? azureSubscriptionId : defaultAzureSubscriptionId;
            azureSecretKey = StringUtils.hasLength(azureSecretKey) ? azureSecretKey : defaultAzureSecretKey;
            azureAccesKey = StringUtils.hasLength(azureAccesKey) ? azureAccesKey : defaultAzureAccesKey;
            azureTenantId = StringUtils.hasLength(azureTenantId) ? azureTenantId : defaultAzureTenantId;

            cloudProviderParams.put("cloudProvider", cloudProvider);
            cloudProviderParams.put("subscriptionId", azureSubscriptionId);
            cloudProviderParams.put("secretKey", azureSecretKey);
            cloudProviderParams.put("accesKey", azureAccesKey);
            cloudProviderParams.put("tenantId", azureTenantId);
        }
    }

    @BeforeSuite(dependsOnMethods = "initContext")
    @Parameters({"cloudProvider", "region"})
    public void initAWSCloudProviderParameters(String cloudProvider, @Optional("") String region) {
        if ("AWS".equals(cloudProvider)) {
            cloudProviderParams.put("cloudProvider", cloudProvider);
            cloudProviderParams.put("region", region);
        }
    }

    @BeforeSuite(dependsOnMethods = "initContext")
    @Parameters({"cloudProvider", "openstackTenantName", "openstackUserName", "openstackPassword", "openstackEndpoint" })
    public void setCloudProviderParameters(String cloudProvider, @Optional("") String openstackTenantName, @Optional("") String openstackUserName,
            @Optional("") String openstackPassword, @Optional("") String openstackEndpoint) {
        if ("OPENSTACK".equals(cloudProvider)) {
            openstackTenantName = StringUtils.hasLength(openstackTenantName) ? openstackTenantName : defaultOpenstackTenantName;
            openstackUserName = StringUtils.hasLength(openstackUserName) ? openstackUserName : defaultOpenstackUserName;
            openstackPassword = StringUtils.hasLength(openstackPassword) ? openstackPassword : defaultOpenstackPassword;
            openstackEndpoint = StringUtils.hasLength(openstackEndpoint) ? openstackEndpoint : defaultOpenstackEndpoint;

            cloudProviderParams.put("cloudProvider", cloudProvider);
            cloudProviderParams.put("tenantName", openstackTenantName);
            cloudProviderParams.put("userName", openstackUserName);
            cloudProviderParams.put("password", openstackPassword);
            cloudProviderParams.put("endpoint", openstackEndpoint);
        }
    }

    @BeforeSuite(dependsOnMethods = "initContext")
    @Parameters({ "cloudProvider", "gcpAvailabiltyZone", "gcpAppName", "gcpProjectId", "gcpServiceAccountId", "gcpP12File" })
    public void checkGcpTags(String cloudProvider, @Optional ("europe-west1-b") String gcpAvailabilityZone, @Optional ("") String gcpAppName,
            @Optional ("") String gcpProjectId, @Optional ("") String gcpServiceAccountId, @Optional ("") String gcpP12File) throws Exception {
        if ("GCP".equals(cloudProvider)) {
            gcpAppName = StringUtils.hasLength(gcpAppName) ? gcpAppName : defaultGcpName;
            gcpProjectId = StringUtils.hasLength(gcpProjectId) ? gcpProjectId : defaultGcpProjectId;
            gcpServiceAccountId = StringUtils.hasLength(gcpServiceAccountId) ? gcpServiceAccountId : defaultGcpServiceAccountId;
            gcpP12File = StringUtils.hasLength(gcpP12File) ? gcpP12File : defaultGcpP12File;

            cloudProviderParams.put("cloudProvider", cloudProvider);
            cloudProviderParams.put("availabilityZone", gcpAvailabilityZone);
            cloudProviderParams.put("applicationName", gcpAppName);
            cloudProviderParams.put("projectId", gcpProjectId);
            cloudProviderParams.put("serviceAccountId", gcpServiceAccountId);
            cloudProviderParams.put("p12File", gcpP12File);
        }
    }
}
