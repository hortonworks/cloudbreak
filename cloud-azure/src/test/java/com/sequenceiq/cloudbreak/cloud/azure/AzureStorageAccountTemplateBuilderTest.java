package com.sequenceiq.cloudbreak.cloud.azure;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Map;

import org.hamcrest.collection.IsMapContaining;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.microsoft.azure.management.storage.StorageAccountSkuType;
import com.sequenceiq.cloudbreak.cloud.azure.connector.resource.StorageAccountParameters;
import com.sequenceiq.cloudbreak.util.FreeMarkerTemplateUtils;

import freemarker.template.Template;
import freemarker.template.TemplateException;

@ExtendWith(MockitoExtension.class)
class AzureStorageAccountTemplateBuilderTest {

    @Mock
    private FreeMarkerTemplateUtils freeMarkerTemplateUtils;

    @Mock
    private AzureStorageAccoutTemplateProviderService azureStorageAccoutTemplateProviderService;

    @InjectMocks
    private AzureStorageAccountTemplateBuilder underTest;

    @Test
    void whenBuildTemplateThenModelParametersAreSet() throws IOException, TemplateException {
        StorageAccountParameters storageAccountParameters =
                new StorageAccountParameters("my-rg", "my-sa", "sa-location", StorageAccountSkuType.STANDARD_LRS, true, Map.of("tagKey", "tagValue"));
        Template templateMock = mock(Template.class);
        when(azureStorageAccoutTemplateProviderService.getTemplate()).thenReturn(templateMock);

        underTest.build(storageAccountParameters);

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(freeMarkerTemplateUtils).processTemplateIntoString(any(), captor.capture());
        Map<String, Object> templateModel = captor.getValue();
        assertThat(templateModel, IsMapContaining.hasEntry("storageAccountName", "my-sa"));
        assertThat(templateModel, IsMapContaining.hasEntry("location", "sa-location"));
        assertThat(templateModel, IsMapContaining.hasEntry("encrypted", true));
        assertThat(templateModel, IsMapContaining.hasEntry("skuName", StorageAccountSkuType.STANDARD_LRS.name().toString()));
        assertThat(templateModel, IsMapContaining.hasKey("userDefinedTags"));
    }
}
