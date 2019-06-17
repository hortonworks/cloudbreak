package com.sequenceiq.cloudbreak.cloud.aws;

import static com.sequenceiq.cloudbreak.cloud.aws.TestConstants.LATEST_AWS_CLOUD_FORMATION_DB_TEMPLATE_PATH;
import static java.util.Collections.emptyList;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.freemarker.FreeMarkerConfigurationFactoryBean;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.cloud.aws.CloudFormationTemplateBuilder.RDSModelContext;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseEngine;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseServer;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.Security;
import com.sequenceiq.cloudbreak.common.service.DefaultCostTaggingService;
import com.sequenceiq.cloudbreak.common.type.CloudbreakResourceType;
import com.sequenceiq.cloudbreak.util.FreeMarkerTemplateUtils;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;

import freemarker.template.Configuration;

@RunWith(Parameterized.class)
public class CloudFormationTemplateBuilderDBTest {

    private static final String V16 = "1.16";

    private static final String USER_ID = "horton@hortonworks.com";

    private static final Long WORKSPACE_ID = 1L;

    private static final String CIDR = "10.0.0.0/16";

    private static final int ROOT_VOLUME_SIZE = 17;

    @Mock
    private DefaultCostTaggingService defaultCostTaggingService;

    @Mock
    private FreeMarkerTemplateUtils freeMarkerTemplateUtils;

    @InjectMocks
    private CloudFormationTemplateBuilder cloudFormationTemplateBuilder;

    private DatabaseStack databaseStack;

    private RDSModelContext modelContext;

    private String awsCloudFormationTemplate;

    private AuthenticatedContext authenticatedContext;

    private final String templatePath;

    private final Map<String, String> defaultTags = new HashMap<>();

    public CloudFormationTemplateBuilderDBTest(String templatePath) {
        this.templatePath = templatePath;
    }

    @Parameters(name = "{0}")
    public static Iterable<?> getTemplatesPath() {
        List<String> templates = Lists.newArrayList(LATEST_AWS_CLOUD_FORMATION_DB_TEMPLATE_PATH);
        File[] templateFiles = new File(CloudFormationTemplateBuilderDBTest.class.getClassLoader().getResource("dbtemplates").getPath()).listFiles();
        List<String> olderTemplates = Arrays.stream(templateFiles).map(file -> {
            String[] path = file.getPath().split("/");
            return "templates/" + path[path.length - 1];
        }).filter(s -> !s.contains(".keep")).collect(Collectors.toList());
        templates.addAll(olderTemplates);
        return templates;
    }

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        FreeMarkerConfigurationFactoryBean factoryBean = new FreeMarkerConfigurationFactoryBean();
        factoryBean.setPreferFileSystemAccess(false);
        factoryBean.setTemplateLoaderPath("classpath:/");
        factoryBean.afterPropertiesSet();
        Configuration configuration = factoryBean.getObject();
        ReflectionTestUtils.setField(cloudFormationTemplateBuilder, "freemarkerConfiguration", configuration);

        when(freeMarkerTemplateUtils.processTemplateIntoString(any(), any())).thenCallRealMethod();

        awsCloudFormationTemplate = configuration.getTemplate(templatePath, "UTF-8").toString();
        authenticatedContext = authenticatedContext();

        defaultTags.put(CloudbreakResourceType.DATABASE.templateVariable(), CloudbreakResourceType.DATABASE.key());
        when(defaultCostTaggingService.prepareAllTagsForTemplate()).thenReturn(defaultTags);
        databaseStack = createDefaultDatabaseStack(getDefaultDatabaseStackTags());
    }

    @Test
    public void buildTestDBServer() throws IOException {
        //WHEN
        modelContext = new RDSModelContext()
                // .withAuthenticatedContext(authenticatedContext)
                // .withStack(databaseStack)
                .withTemplate(awsCloudFormationTemplate);
        String templateString = cloudFormationTemplateBuilder.build(modelContext);
        //THEN
        Assert.assertTrue("Invalid JSON: " + templateString, JsonUtil.isValid(templateString));
        // FIXME this is apparently intentional, but it doesn't make sense
        assertThat(templateString, not(containsString("testtagkey")));
        assertThat(templateString, not(containsString("testtagvalue")));
        JsonNode jsonNode = JsonUtil.readTree(templateString);
        jsonNode.findValues("Tags").forEach(jsonNode1 -> {
            assertTrue(jsonNode1.findValues("Key").stream().anyMatch(jsonNode2 -> "cb-resource-type".equals(jsonNode2.textValue())));
        });
    }

    private AuthenticatedContext authenticatedContext() {
        Location location = Location.location(Region.region("region"), AvailabilityZone.availabilityZone("az"));
        CloudContext cloudContext = new CloudContext(5L, "name", "platform", "variant",
                location, USER_ID, WORKSPACE_ID);
        CloudCredential credential = new CloudCredential("crn", null);
        return new AuthenticatedContext(cloudContext, credential);
    }

    private DatabaseStack createDefaultDatabaseStack(Map<String, String> tags) {
        Network network = createDefaultNetwork();
        DatabaseServer server = createDefaultDatabaseServer();
        return new DatabaseStack(network, server, tags, null);
    }

    private Network createDefaultNetwork() {
        return new Network(null, Map.of("subnetId", "subnet-123,subnet-456,subnet-789"));
    }

    private DatabaseServer createDefaultDatabaseServer() {
        return new DatabaseServer("myserver", "db.m3.medium", DatabaseEngine.POSTGRESQL, "root", "cloudera", 50L,
                createDefaultSecurity(), InstanceStatus.CREATE_REQUESTED,
                Map.of("engineVersion", "1.2.3"));
    }

    private Map<String, String> getDefaultDatabaseStackTags() {
        return Map.of("testtagkey", "testtagvalue");
    }

    private Security createDefaultSecurity() {
        return new Security(emptyList(), getDefaultSecurityIds());
    }

    private List<String> getDefaultSecurityIds() {
        return List.of("sg-1234");
    }

    private JsonNode getJsonNode(JsonNode node, String value) {
        if (node == null) {
            throw new RuntimeException("No Json node provided for seeking value!");
        }
        return Optional.ofNullable(node.findValue(value)).orElseThrow(() -> new RuntimeException("No value find in json with the name of: \"" + value + "\""));
    }

}
