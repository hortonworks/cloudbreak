package com.sequenceiq.cloudbreak.cloud.azure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.freemarker.FreeMarkerConfigurationFactoryBean;

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
    private AzureUtils azureUtils;

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
    void buildTestWhenUseSslEnforcementFalse(String templatePath) {
        buildTestWhenUseSslEnforcementInternal(templatePath, false);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("templatesPathDataProvider")
    void buildTestWhenUseSslEnforcementTrue(String templatePath) {
        buildTestWhenUseSslEnforcementInternal(templatePath, true);
    }

    @Test
    void buildTestWhenDataEncryptionParametersPresent() {
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
                .serverId(SERVER_ID)
                .rootUserName(ROOT_USER_NAME)
                .rootPassword(ROOT_PASSWORD)
                .location(REGION)
                .params(params)
                .build();

        DatabaseStack databaseStack = new DatabaseStack(network, databaseServer, Collections.emptyMap(), template.toString());
        Mockito.when(azureDatabaseTemplateProvider.getTemplate(databaseStack)).thenReturn(template);
        Mockito.when(azureUtils.encodeString(SUBNET_ID)).thenReturn("hash");

        String result = underTest.build(cloudContext, databaseStack);

        assertThat(JsonUtil.isValid(result)).overridingErrorMessage("Invalid JSON: " + result).isTrue();
        assertThat(result).contains("\"keyVaultName\": {\n" +
                "              \"type\": \"string\",\n" +
                "              \"defaultValue\" : \"dummyVault\",\n" +
                "              \"metadata\": {\n" +
                "                \"description\": \"Key vault name where the key to use is stored\"\n" +
                "              }\n" +
                "        },\n" +
                "        \"keyVaultResourceGroupName\": {\n" +
                "              \"type\": \"string\",\n" +
                "              \"defaultValue\" : \"dummyResourceGroup\",\n" +
                "              \"metadata\": {\n" +
                "                \"description\": \"Key vault resource group name where it is stored\"\n" +
                "              }\n" +
                "        },\n" +
                "        \"keyName\": {\n" +
                "              \"type\": \"string\",\n" +
                "              \"defaultValue\" : \"dummyKey\",\n" +
                "              \"metadata\": {\n" +
                "                \"description\": \"Key name in the key vault to use as encryption protector\"\n" +
                "              }\n" +
                "        },\n" +
                "        \"keyVersion\": {\n" +
                "              \"type\": \"string\",\n" +
                "              \"defaultValue\" : \"dummyVersion\",\n" +
                "              \"metadata\": {\n" +
                "                \"description\": \"Version of the key in the key vault to use as encryption protector\"\n" +
                "              }\n" +
                "        }");

    }

    @Test
    void buildTestWhenDataEncryptionParametersPresentAndKeyVersionError() {
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
        params.put(KEY_URL, "https://dummyVault.vault.azure.net/keys/dummyKey");
        params.put(KEY_VAULT_RESOURCE_GROUP_NAME, "dummyResourceGroup");
        DatabaseServer databaseServer = DatabaseServer.builder()
                .serverId(SERVER_ID)
                .rootUserName(ROOT_USER_NAME)
                .rootPassword(ROOT_PASSWORD)
                .location(REGION)
                .params(params)
                .build();

        DatabaseStack databaseStack = new DatabaseStack(network, databaseServer, Collections.emptyMap(), template.toString());
        assertThrows(IllegalArgumentException.class, () -> underTest.build(cloudContext, databaseStack));
    }

    private void buildTestWhenUseSslEnforcementInternal(String templatePath, boolean useSslEnforcement) {
        Template template = Optional.ofNullable(factoryBean.getObject())
                .map(config -> {
                    try {
                        return config.getTemplate(templatePath, "UTF-8");
                    } catch (IOException e) {
                        throw new IllegalStateException(e);
                    }
                }).orElseThrow();
        DatabaseStack databaseStack = createDatabaseStack(useSslEnforcement, template.toString());
        Mockito.when(azureDatabaseTemplateProvider.getTemplate(databaseStack)).thenReturn(template);
        Mockito.when(azureUtils.encodeString(SUBNET_ID)).thenReturn("hash");

        String result = underTest.build(cloudContext, databaseStack);

        assertThat(JsonUtil.isValid(result)).overridingErrorMessage("Invalid JSON: " + result).isTrue();
        assertThat(result).contains(
                "        \"useSslEnforcement\": {\n" +
                        "            \"type\": \"bool\",\n" +
                        "            \"defaultValue\": " + useSslEnforcement + ",");
        assertThat(result).contains(
                "        \"privateEndpointName\": {\n" +
                        "            \"defaultValue\": \"pe-hash-to-myServer\",\n" +
                        "            \"type\": \"String\"\n" +
                        "        }"
        );
    }

    private DatabaseStack createDatabaseStack(boolean useSslEnforcement, String template) {
        Subnet subnet = new Subnet(SUBNET_CIDR);
        Network network = new Network(subnet, List.of(NETWORK_CIDR), OutboundInternetTraffic.ENABLED);
        network.putParameter("subnets", FULL_SUBNET_ID);

        DatabaseServer databaseServer = DatabaseServer.builder()
                .useSslEnforcement(useSslEnforcement)
                .serverId(SERVER_ID)
                .rootUserName(ROOT_USER_NAME)
                .rootPassword(ROOT_PASSWORD)
                .location(REGION)
                .params(Map.of("dbVersion", "10"))
                .build();

        return new DatabaseStack(network, databaseServer, Collections.emptyMap(), template);
    }

}
