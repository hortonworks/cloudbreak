package com.sequenceiq.cloudbreak.converter;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.controller.json.TemplateJson;
import com.sequenceiq.cloudbreak.controller.validation.AzureTemplateParam;
import com.sequenceiq.cloudbreak.domain.AzureTemplate;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;

public class AzureTemplateConverterTest {

    private static final String DUMMY_VM_TYPE = "STANDARD_D1";
    private static final String DUMMY_DESCRIPTION = "dummyDescription";
    private static final String DUMMY_NAME = "dummyName";
    private static final String PORT = "8081";
    private static final String LOCAL_PORT = "8080";
    private static final String DUMMY_PROTOCOL = "dummyProtocol";

    private AzureTemplateConverter underTest;

    private AzureTemplate azureTemplate;

    private TemplateJson templateJson;

    @Before
    public void setUp() {
        underTest = new AzureTemplateConverter();
        azureTemplate = createAzureTemplate();
        templateJson = createTemplateJson();
    }

    @Test
    public void testConvertAzureTemplateEntityToJson() {
        // GIVEN
        // WHEN
        TemplateJson result = underTest.convert(azureTemplate);
        // THEN
        assertEquals(result.getCloudPlatform(), azureTemplate.cloudPlatform());


    }

    @Test
    public void testConvertAzureTemplateJsonToEntity() {
        // GIVEN
        // WHEN
        AzureTemplate result = underTest.convert(templateJson);
        assertEquals(result.cloudPlatform(), templateJson.getCloudPlatform());
        assertEquals(result.getDescription(), templateJson.getDescription());
    }

    @Test
    public void testConvertAzureTemplateJsonToEntityWhenNoPorts() {
        // GIVEN
        Map<String, Object> props = new HashMap<>();
        props.put(AzureTemplateParam.VMTYPE.getName(), DUMMY_VM_TYPE);
        templateJson.setParameters(props);
        // WHEN
        AzureTemplate result = underTest.convert(templateJson);
        assertEquals(result.cloudPlatform(), templateJson.getCloudPlatform());
        assertEquals(result.getDescription(), templateJson.getDescription());
    }

    @Test
    public void testConvertAzureJsonWithSpecifiedVolumeTypeWhenTheVolumeSizeCorrectNoExceptionOccurs() {
        Map<String, Object> props = new HashMap<>();
        props.put(AzureTemplateParam.VMTYPE.getName(), DUMMY_VM_TYPE);
        templateJson.setVolumeCount(1);
        templateJson.setVolumeSize(100);
        templateJson.setParameters(props);

        AzureTemplate result = underTest.convert(templateJson);
    }

    @Test(expected = BadRequestException.class)
    public void testConvertAzureJsonWithSpecifiedVolumeTypeWhenTheVolumeCountInCorrectThenBadRequestException() {
        Map<String, Object> props = new HashMap<>();
        props.put(AzureTemplateParam.VMTYPE.getName(), DUMMY_VM_TYPE);
        templateJson.setVolumeCount(10);
        templateJson.setVolumeSize(10000);
        templateJson.setParameters(props);

        AzureTemplate result = underTest.convert(templateJson);
    }

    @Test(expected = BadRequestException.class)
    public void testConvertAzureJsonWithSpecifiedVolumeTypeWhenTheVolumeSizeIsBiggerWithOneCountThenBadRequestException() {
        Map<String, Object> props = new HashMap<>();
        props.put(AzureTemplateParam.VMTYPE.getName(), DUMMY_VM_TYPE);
        templateJson.setVolumeCount(3);
        templateJson.setVolumeSize(2049);
        templateJson.setParameters(props);

        AzureTemplate result = underTest.convert(templateJson);
    }

    private AzureTemplate createAzureTemplate() {
        AzureTemplate azureTemplate = new AzureTemplate();
        azureTemplate.setVmType(DUMMY_VM_TYPE);
        azureTemplate.setDescription(DUMMY_DESCRIPTION);
        azureTemplate.setName(DUMMY_NAME);
        azureTemplate.setId(1L);
        azureTemplate.setPublicInAccount(true);
        return azureTemplate;
    }

    private TemplateJson createTemplateJson() {
        TemplateJson templateJson = new TemplateJson();
        templateJson.setCloudPlatform(CloudPlatform.AZURE);
        templateJson.setId(1L);
        templateJson.setDescription(DUMMY_DESCRIPTION);
        templateJson.setName(DUMMY_NAME);
        Map<String, Object> props = new HashMap<>();
        props.put(AzureTemplateParam.VMTYPE.getName(), DUMMY_VM_TYPE);
        templateJson.setParameters(props);
        return templateJson;
    }

    private List<Map<String, String>> createPorts() {
        List<Map<String, String>> portsObject = new ArrayList<>();
        Map<String, String> port = new LinkedHashMap<>();
        port.put("localPort", LOCAL_PORT);
        port.put("port", PORT);
        port.put("protocol", DUMMY_PROTOCOL);
        port.put("name", DUMMY_NAME);
        portsObject.add(port);
        return portsObject;
    }
}
