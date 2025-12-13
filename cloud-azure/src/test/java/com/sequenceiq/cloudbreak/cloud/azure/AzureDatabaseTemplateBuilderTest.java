package com.sequenceiq.cloudbreak.cloud.azure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseServer;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.util.FreeMarkerTemplateUtils;
import com.sequenceiq.common.model.AzureDatabaseType;

import freemarker.template.Template;

@ExtendWith(MockitoExtension.class)
class AzureDatabaseTemplateBuilderTest {
    @InjectMocks
    private AzureDatabaseTemplateBuilder underTest;

    @Mock
    private FreeMarkerTemplateUtils freeMarkerTemplateUtils;

    @Mock
    private AzureDatabaseTemplateProvider azureDatabaseTemplateProvider;

    @Mock
    private Map<AzureDatabaseType, AzureDatabaseTemplateModelBuilder> azureDatabaseTemplateModelBuilderMap;

    @Mock
    private AzureDatabaseTemplateModelBuilder azureDatabaseTemplateModelBuilder;

    @Mock
    private Template template;

    @Test
    void testBuild() throws Exception {
        CloudContext cloudContext = CloudContext.Builder.builder()
                .withLocation(Location.location(Region.region("region"), null))
                .build();
        DatabaseServer databaseServer = DatabaseServer.builder()
                .withParams(Map.of(AzureDatabaseType.AZURE_DATABASE_TYPE_KEY, AzureDatabaseType.SINGLE_SERVER.name()))
                .build();
        DatabaseStack databaseStack = new DatabaseStack(null, databaseServer, Map.of(), null);

        when(azureDatabaseTemplateModelBuilderMap.get(AzureDatabaseType.SINGLE_SERVER)).thenReturn(azureDatabaseTemplateModelBuilder);
        when(azureDatabaseTemplateProvider.getTemplate(databaseStack)).thenReturn(template);
        when(freeMarkerTemplateUtils.processTemplateIntoString(template, Map.of())).thenReturn("template");
        String actualResult = underTest.build(cloudContext, databaseStack);
        assertEquals(actualResult, "template");
    }

    @Test
    void testBuildException() throws Exception {
        CloudContext cloudContext = CloudContext.Builder.builder()
                .withLocation(Location.location(Region.region("region"), null))
                .build();
        DatabaseServer databaseServer = DatabaseServer.builder()
                .withParams(Map.of(AzureDatabaseType.AZURE_DATABASE_TYPE_KEY, AzureDatabaseType.SINGLE_SERVER.name()))
                .build();
        DatabaseStack databaseStack = new DatabaseStack(null, databaseServer, Map.of(), null);

        when(azureDatabaseTemplateModelBuilderMap.get(AzureDatabaseType.SINGLE_SERVER)).thenReturn(azureDatabaseTemplateModelBuilder);
        when(azureDatabaseTemplateProvider.getTemplate(databaseStack)).thenReturn(template);
        when(freeMarkerTemplateUtils.processTemplateIntoString(template, Map.of())).thenThrow(new IOException("exception"));
        CloudConnectorException exception = assertThrows(CloudConnectorException.class, () -> underTest.build(cloudContext, databaseStack));
        assertEquals("Failed to process the ARM TemplateBuilder", exception.getMessage());
    }
}
