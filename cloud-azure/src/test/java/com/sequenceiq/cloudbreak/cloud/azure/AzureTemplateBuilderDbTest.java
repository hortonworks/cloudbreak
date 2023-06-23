package com.sequenceiq.cloudbreak.cloud.azure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.freemarker.FreeMarkerConfigurationFactoryBean;

import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureDatabaseServerView;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureNetworkView;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseServer;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.Subnet;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.util.FreeMarkerTemplateUtils;
import com.sequenceiq.common.api.type.OutboundInternetTraffic;
import com.sequenceiq.common.model.AzureDatabaseType;

import freemarker.template.Template;

@ExtendWith(MockitoExtension.class)
public class AzureTemplateBuilderDbTest {

    private static final String LATEST_ARM_DB_TEMPLATE_PATH = "templates/arm-dbstack.ftl";

    private static final Long STACK_ID = 1234L;

    private static final String STACK_NAME = "myStack";

    private static final String STACK_CRN = "crn";

    private static final String PLATFORM = "Azure";

    private static final String VARIANT = "";

    private static final String REGION = "westus2";

    private static final String ACCOUNT_ID = UUID.randomUUID().toString();

    private static final String SUBNET_CIDR = "0.0.0.0/0";

    private static final String RESOURCE_GROUP = "rg";

    private static final String SUBNET_ID = "subnet-lkewflerwkj";

    private static final String FULL_SUBNET_ID = RESOURCE_GROUP + '/' + SUBNET_ID;

    private static final String NETWORK_CIDR = "127.0.0.1/32";

    private static final String SERVER_ID = "myServer";

    private static final String ROOT_USER_NAME = "boss";

    private static final String ROOT_PASSWORD = "godmode";

    private static final String KEY_URL = "keyVaultUrl";

    private static final String KEY_VAULT_RESOURCE_GROUP_NAME = "keyVaultResourceGroupName";

    @Spy
    private FreeMarkerTemplateUtils freeMarkerTemplateUtils;

    @InjectMocks
    private AzureDatabaseTemplateBuilder underTest;

    @Mock
    private AzureDatabaseTemplateProvider azureDatabaseTemplateProvider;

    @Mock
    private Map<AzureDatabaseType, AzureDatabaseTemplateModelBuilder> azureDatabaseTemplateModelBuilderMap;

    @Mock
    private AzureDatabaseTemplateModelBuilder azureDatabaseTemplateModelBuilder;

    private CloudContext cloudContext;

    private FreeMarkerConfigurationFactoryBean factoryBean;

    static Iterable<?> templatesPathDataProvider() {
        return List.of(LATEST_ARM_DB_TEMPLATE_PATH);
    }

    @BeforeEach
    void setUp() throws Exception {
        factoryBean = new FreeMarkerConfigurationFactoryBean();
        factoryBean.setPreferFileSystemAccess(false);
        factoryBean.setTemplateLoaderPath("classpath:/");
        factoryBean.afterPropertiesSet();

        cloudContext = CloudContext.Builder.builder()
                .withId(STACK_ID)
                .withName(STACK_NAME)
                .withCrn(STACK_CRN)
                .withPlatform(PLATFORM)
                .withVariant(VARIANT)
                .withLocation(Location.location(Region.region(REGION)))
                .withAccountId(ACCOUNT_ID)
                .build();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("templatesPathDataProvider")
    void buildTestWhenUseSslEnforcementFalse(String templatePath) throws IOException {
        buildTestWhenUseSslEnforcementInternal(templatePath, false);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("templatesPathDataProvider")
    void buildTestWhenUseSslEnforcementTrue(String templatePath) throws IOException {
        buildTestWhenUseSslEnforcementInternal(templatePath, true);
    }

    @Test
    void buildTestWhenDataEncryptionParametersPresent() throws IOException {
        Template template = Optional.ofNullable(factoryBean.getObject())
                .map(config -> {
                    try {
                        return config.getTemplate("templates/arm-dbstack.ftl", "UTF-8");
                    } catch (IOException e) {
                        throw new IllegalStateException(e);
                    }
                }).orElseThrow();
        Subnet subnet = new Subnet(SUBNET_CIDR);
        Network network = new Network(subnet, List.of(NETWORK_CIDR), OutboundInternetTraffic.ENABLED);
        network.putParameter("subnets", FULL_SUBNET_ID);
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("dbVersion", "10");
        params.put(KEY_URL, "https://dummyVault.vault.azure.net/keys/dummyKey/dummyVersion");
        params.put(KEY_VAULT_RESOURCE_GROUP_NAME, "dummyResourceGroup");
        DatabaseServer databaseServer = DatabaseServer.builder()
                .withServerId(SERVER_ID)
                .withRootUserName(ROOT_USER_NAME)
                .withRootPassword(ROOT_PASSWORD)
                .withLocation(REGION)
                .withParams(params)
                .build();

        DatabaseStack databaseStack = new DatabaseStack(network, databaseServer, Collections.emptyMap(), template.toString());
        when(azureDatabaseTemplateModelBuilderMap.get(AzureDatabaseType.SINGLE_SERVER)).thenReturn(azureDatabaseTemplateModelBuilder);
        when(azureDatabaseTemplateModelBuilder.buildModel(any(AzureDatabaseServerView.class), any(AzureNetworkView.class), any(DatabaseStack.class)))
                .thenReturn(createDefaultModel(false));
        when(azureDatabaseTemplateProvider.getTemplate(databaseStack)).thenReturn(template);

        String result = underTest.build(cloudContext, databaseStack);

        assertThat(JsonUtil.isValid(result)).overridingErrorMessage("Invalid JSON: " + result).isTrue();
        JsonNode parameters = JsonUtil.readTree(result).get("parameters");
        checkParameter(parameters.get("keyVaultName"), "string", "dummyVault");
        checkParameter(parameters.get("keyVaultResourceGroupName"), "string", "dummyResourceGroup");
        checkParameter(parameters.get("keyName"), "string", "dummyKey");
        checkParameter(parameters.get("keyVersion"), "string", "dummyVersion");
    }

    private void buildTestWhenUseSslEnforcementInternal(String templatePath, boolean useSslEnforcement) throws IOException {
        Template template = Optional.ofNullable(factoryBean.getObject())
                .map(config -> {
                    try {
                        return config.getTemplate(templatePath, "UTF-8");
                    } catch (IOException e) {
                        throw new IllegalStateException(e);
                    }
                }).orElseThrow();
        DatabaseStack databaseStack = createDatabaseStack(useSslEnforcement, template.toString());
        when(azureDatabaseTemplateProvider.getTemplate(databaseStack)).thenReturn(template);
        when(azureDatabaseTemplateModelBuilderMap.get(AzureDatabaseType.SINGLE_SERVER)).thenReturn(azureDatabaseTemplateModelBuilder);
        when(azureDatabaseTemplateModelBuilder.buildModel(any(AzureDatabaseServerView.class), any(AzureNetworkView.class), any(DatabaseStack.class)))
                .thenReturn(createDefaultModel(useSslEnforcement));

        String result = underTest.build(cloudContext, databaseStack);

        assertThat(JsonUtil.isValid(result)).overridingErrorMessage("Invalid JSON: " + result).isTrue();
        JsonNode parameters = JsonUtil.readTree(result).get("parameters");
        checkParameter(parameters.get("useSslEnforcement"), "bool", String.valueOf(useSslEnforcement));
        checkParameter(parameters.get("privateEndpointName"), "String", "pe-hash-to-myServer");
    }

    private <T> void checkParameter(JsonNode parameter, String type, String valueStr) {
        Assertions.assertEquals(type, parameter.get("type").asText());
        Assertions.assertEquals(valueStr, parameter.get("defaultValue").asText());
    }

    private DatabaseStack createDatabaseStack(boolean useSslEnforcement, String template) {
        Subnet subnet = new Subnet(SUBNET_CIDR);
        Network network = new Network(subnet, List.of(NETWORK_CIDR), OutboundInternetTraffic.ENABLED);
        network.putParameter("subnets", FULL_SUBNET_ID);

        DatabaseServer databaseServer = DatabaseServer.builder()
                .withUseSslEnforcement(useSslEnforcement)
                .withServerId(SERVER_ID)
                .withRootUserName(ROOT_USER_NAME)
                .withRootPassword(ROOT_PASSWORD)
                .withLocation(REGION)
                .withParams(Map.of("dbVersion", "10"))
                .build();

        return new DatabaseStack(network, databaseServer, Collections.emptyMap(), template);
    }

    private Map<String, Object> createDefaultModel(boolean useSslEnforcement) {
        return Map.ofEntries(
                Map.entry("dbServerName", "dbname"),
                Map.entry("dbVersion", "dbversion"),
                Map.entry("adminLoginName", "root"),
                Map.entry("adminPassword", "pwd"),
                Map.entry("location", "location"),
                Map.entry("keyVaultName", "dummyVault"),
                Map.entry("keyVaultResourceGroupName", "dummyResourceGroup"),
                Map.entry("keyName", "dummyKey"),
                Map.entry("keyVersion", "dummyVersion"),
                Map.entry("batchSize", 1),
                Map.entry("subnets", ""),
                Map.entry("usePrivateEndpoints", false),
                Map.entry("privateEndpointName", "pe-hash-to-myServer"),
                Map.entry("dataEncryption", true),
                Map.entry("useSslEnforcement", useSslEnforcement));
    }
}
