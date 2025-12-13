package com.sequenceiq.cloudbreak.controller.validation.template.azure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
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
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.dto.credential.Credential;
import com.sequenceiq.cloudbreak.service.template.TemplateService;

@ExtendWith(MockitoExtension.class)
public class ResourceDiskPropertyCalculatorTest {

    @Mock
    private TemplateService templateService;

    @Mock
    private Credential credential;

    @Mock
    private Template template;

    @Mock
    private VmTypeMeta vmTypeMeta;

    @Mock
    private VmType vmType;

    @InjectMocks
    private ResourceDiskPropertyCalculator underTest;

    @Test
    public void testUpdateWithResourceDiskAttachedWhenAzure() {
        when(template.getCloudPlatform()).thenReturn("AZURE");
        when(credential.cloudPlatform()).thenReturn("AZURE");
        when(vmType.getMetaData()).thenReturn(vmTypeMeta);
        when(vmTypeMeta.getResourceDiskAttached()).thenReturn(true);
        Map<String, String> map = new HashMap<>();
        map.put(AzureInstanceTemplate.VOLUME_ENCRYPTION_KEY_ID, "encryptionId");
        when(template.getAttributes()).thenReturn(new Json(map));

        underTest.updateWithResourceDiskAttached(credential, template, vmType);

        verify(templateService, times(1)).savePure(any(Template.class));
    }

    @Test
    public void testUpdateWithResourceDiskAttachedWhenAzureAndVmtypeNullShouldThrowBadRequest() {
        when(credential.cloudPlatform()).thenReturn("AZURE");

        BadRequestException thrown = assertThrows(BadRequestException.class, () ->
                underTest.updateWithResourceDiskAttached(credential, template, null));

        assertEquals("The virtual machine type for Azure probably not supported in you subscription. " +
                "Please make sure you select an instance which enabled for CDP.", thrown.getMessage());
    }

    @Test
    public void testUpdateWithResourceDiskAttachedWhenAws() {
        when(credential.cloudPlatform()).thenReturn("AWS");

        underTest.updateWithResourceDiskAttached(credential, template, vmType);

        verify(templateService, times(0)).savePure(any(Template.class));
    }

}