package com.sequenceiq.cloudbreak.controller.validation.template.azure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.VmType;
import com.sequenceiq.cloudbreak.cloud.model.VmTypeMeta;
import com.sequenceiq.cloudbreak.cloud.model.instance.AzureInstanceTemplate;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.dto.credential.Credential;
import com.sequenceiq.cloudbreak.service.template.TemplateService;
import com.sequenceiq.environment.api.v1.environment.endpoint.service.azure.HostEncryptionCalculator;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@ExtendWith(MockitoExtension.class)
class HostEncryptionProviderTest {

    @Mock
    private TemplateService templateService;

    @Mock
    private HostEncryptionCalculator hostEncryptionCalculator;

    @InjectMocks
    private HostEncryptionProvider hostEncryptionProvider;

    @Test
    public void testUpdateWithHostEncryptionAzureWithEncryptionEnabled() {
        Credential credential = createMockCredential(CloudPlatform.AZURE);
        Template template = createMockTemplate();
        VmTypeMeta metaData = mock(VmTypeMeta.class);
        VmType vmType = createMockVmType();
        DetailedEnvironmentResponse environmentResponse = mock(DetailedEnvironmentResponse.class);

        when(hostEncryptionCalculator.hostEncryptionRequired(any())).thenReturn(true);
        when(templateService.savePure(any(Template.class))).thenReturn(template);
        when(metaData.getHostEncryptionSupported()).thenReturn(true);
        when(vmType.getMetaData()).thenReturn(metaData);

        Template result = hostEncryptionProvider.updateWithHostEncryption(environmentResponse, credential, template, vmType);

        assertNotNull(result);
        verify(templateService, times(1)).savePure(any(Template.class));
    }

    @Test
    public void testUpdateWithHostEncryptionAzureWithEncryptionDisabled() {
        Credential credential = createMockCredential(CloudPlatform.AZURE);
        Template template = new Template();
        VmType vmType = createMockVmType();
        VmTypeMeta vmTypeMeta = mock(VmTypeMeta.class);
        DetailedEnvironmentResponse environmentResponse = mock(DetailedEnvironmentResponse.class);
        when(hostEncryptionCalculator.hostEncryptionRequired(any())).thenReturn(true);
        when(vmType.getMetaData()).thenReturn(vmTypeMeta);
        when(vmTypeMeta.getHostEncryptionSupported()).thenReturn(false);
        when(templateService.savePure(any())).thenReturn(template);

        Template result = hostEncryptionProvider.updateWithHostEncryption(environmentResponse, credential, template, vmType);

        assertEquals(result.getAttributes(), new Json(Map.of(AzureInstanceTemplate.ENCRYPTION_AT_HOST_ENABLED, false)));
        verify(templateService, atLeast(1)).savePure(any(Template.class));
    }

    @Test
    public void testUpdateWithHostEncryptionAzureWithNullVmType() {
        Credential credential = createMockCredential(CloudPlatform.AZURE);
        Template template = createMockTemplate();
        DetailedEnvironmentResponse environmentResponse = mock(DetailedEnvironmentResponse.class);

        assertThrows(BadRequestException.class,
                () -> hostEncryptionProvider.updateWithHostEncryption(environmentResponse, credential, template, null));
    }

    private Credential createMockCredential(CloudPlatform cloudPlatform) {
        Credential credential = mock(Credential.class);
        when(credential.cloudPlatform()).thenReturn(cloudPlatform.name());
        return credential;
    }

    private Template createMockTemplate() {
        return mock(Template.class);
    }

    private VmType createMockVmType() {
        return mock(VmType.class);
    }
}