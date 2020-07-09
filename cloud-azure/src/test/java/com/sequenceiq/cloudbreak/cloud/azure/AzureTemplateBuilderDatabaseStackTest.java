package com.sequenceiq.cloudbreak.cloud.azure;

import static org.mockito.Mockito.reset;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.freemarker.FreeMarkerConfigurationFactoryBean;

import com.sequenceiq.cloudbreak.cloud.azure.validator.AzureAcceleratedNetworkValidator;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseEngine;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseServer;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.Subnet;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.cloudbreak.util.FreeMarkerTemplateUtils;
import com.sequenceiq.common.api.tag.model.Tags;

import freemarker.template.Configuration;

public class AzureTemplateBuilderDatabaseStackTest {

    private static final String USER_ID = "horton@hortonworks.com";

    private static final Long WORKSPACE_ID = 1L;

    private static final String LATEST_TEMPLATE_PATH = "templates/arm-dbstack.ftl";

    @Mock
    private AzureUtils azureUtils;

    @Mock
    private AzureStorage azureStorage;

    @Mock
    private Configuration freemarkerConfiguration;

    @Mock
    private AzureAcceleratedNetworkValidator azureAcceleratedNetworkValidator;

    @Spy
    private FreeMarkerTemplateUtils freeMarkerTemplateUtils;

    @InjectMocks
    private final AzureTemplateBuilder azureTemplateBuilder = new AzureTemplateBuilder();

    private CloudContext cloudContext;

    private final String templatePath = AzureTemplateBuilderDatabaseStackTest.class.getClassLoader().getResource(LATEST_TEMPLATE_PATH).getPath();

    private final String template = FileReaderUtils.readFileFromPath(Paths.get(templatePath));

    public AzureTemplateBuilderDatabaseStackTest() throws IOException {
    }

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        FreeMarkerConfigurationFactoryBean factoryBean = new FreeMarkerConfigurationFactoryBean();
        factoryBean.setPreferFileSystemAccess(false);
        factoryBean.setTemplateLoaderPath("classpath:/");
        factoryBean.afterPropertiesSet();
        Configuration configuration = factoryBean.getObject();
        ReflectionTestUtils.setField(azureTemplateBuilder, "freemarkerConfiguration", configuration);
        ReflectionTestUtils.setField(azureTemplateBuilder, "armDatabaseTemplatePath", templatePath);
        ReflectionTestUtils.setField(azureTemplateBuilder, "armTemplateParametersPath", "templates/parameters.ftl");
        cloudContext = new CloudContext(7899L, "thisisaverylongazureresourcenamewhichneedstobeshortened", "dummy1", "test",
                Location.location(Region.region("EU"), new AvailabilityZone("availabilityZone")), USER_ID, WORKSPACE_ID);

        reset(azureUtils);
    }

    @Test
    public void shouldProcessTemplate() throws IOException {
        Network network = new Network(new Subnet("192.168.0.0/24"), Map.of("subnets", "192.168.1.0"));
        DatabaseServer databaseServer = DatabaseServer.builder()
                .storageSize(10L)
                .params(Map.of(
                        "dbVersion", "123",
                        "backupRetentionDays", 2,
                        "geoRedundantBackup", false,
                        "skuCapacity", 3,
                        "skuFamily", "Fast&Furious",
                        "skuTier", "tier-1",
                        "storageAutoGrow", true
                ))
                .flavor("chocolate")
                .serverId("id")
                .engine(DatabaseEngine.POSTGRESQL)
                .rootUserName("root")
                .rootPassword("pw")
                .port(1234)
                .build();
        Tags tags = new Tags(Map.of("tagKey", "tagValue"));
        DatabaseStack databaseStack = new DatabaseStack(network, databaseServer, tags, template);

        Assertions.assertDoesNotThrow(() -> azureTemplateBuilder.build(cloudContext, databaseStack));
    }
}
