package com.sequenceiq.cloudbreak.converter;

import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.convert.ConversionService;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.api.model.TemplateResponse;
import com.sequenceiq.cloudbreak.api.model.v2.template.AwsParameters;
import com.sequenceiq.cloudbreak.api.model.v2.template.AzureParameters;
import com.sequenceiq.cloudbreak.api.model.v2.template.BaseTemplateParameter;
import com.sequenceiq.cloudbreak.api.model.v2.template.AwsEncryption;
import com.sequenceiq.cloudbreak.api.model.v2.template.EncryptionType;
import com.sequenceiq.cloudbreak.api.model.v2.template.GcpEncryption;
import com.sequenceiq.cloudbreak.api.model.v2.template.GcpParameters;
import com.sequenceiq.cloudbreak.api.model.v2.template.KeyEncryptionMethod;
import com.sequenceiq.cloudbreak.api.model.v2.template.OpenStackParameters;
import com.sequenceiq.cloudbreak.api.model.v2.template.YarnParameters;
import com.sequenceiq.cloudbreak.common.type.CloudConstants;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.json.Json;

public class TemplateToTemplateResponseConverterTest {

    @Mock
    private ConversionService conversionService;

    @InjectMocks
    private TemplateToTemplateResponseConverter underTest;

    @Before
    public void setUp() throws JsonProcessingException {
        MockitoAnnotations.initMocks(this);
        when(conversionService.convert(parameters(CloudConstants.AWS).getMap(), AwsParameters.class)).thenReturn(awsParameters());
        when(conversionService.convert(parameters(CloudConstants.YARN).getMap(), YarnParameters.class)).thenReturn(yarnParameters());
        when(conversionService.convert(parameters(CloudConstants.AZURE).getMap(), AzureParameters.class)).thenReturn(azureParameters());
        when(conversionService.convert(parameters(CloudConstants.GCP).getMap(), GcpParameters.class)).thenReturn(gcpParameters());
        when(conversionService.convert(parameters(CloudConstants.OPENSTACK).getMap(), OpenStackParameters.class)).thenReturn(openStackParameters());
    }

    @Test
    public void testAwsTemplateParametersWhenNotNull() throws JsonProcessingException {

        Template template = new Template();
        template.setAttributes(parameters(CloudConstants.AWS));

        TemplateResponse convert = underTest.convert(template);

        Assert.assertNotNull(convert.getAwsParameters());
        Assert.assertEquals(10.0D, convert.getAwsParameters().getSpotPrice().doubleValue(), 0);
        Assert.assertEquals("someKey", convert.getAwsParameters().getAwsEncryption().getKey());
        Assert.assertEquals(EncryptionType.CUSTOM.name(), convert.getAwsParameters().getAwsEncryption().getType());
        Assert.assertNull(convert.getAzureParameters());
        Assert.assertNull(convert.getGcpParameters());
        Assert.assertNull(convert.getOpenStackParameters());
    }

    @Test
    public void testAzureTemplateParametersWhenNotNull() throws JsonProcessingException {

        Template template = new Template();
        template.setAttributes(parameters(CloudConstants.AZURE));

        TemplateResponse convert = underTest.convert(template);

        Assert.assertNotNull(convert.getAzureParameters());
        Assert.assertEquals("somePrivateId", convert.getAzureParameters().getPrivateId());
    }

    @Test
    public void testGcpTemplateParametersWhenNotNull() throws JsonProcessingException {

        Template template = new Template();
        template.setAttributes(parameters(CloudConstants.GCP));

        TemplateResponse convert = underTest.convert(template);

        Assert.assertNotNull(convert.getGcpParameters());
        Assert.assertEquals(EncryptionType.CUSTOM.name(), convert.getGcpParameters().getEncryption().getType());
        Assert.assertEquals(KeyEncryptionMethod.RAW.name(), convert.getGcpParameters().getEncryption().getKeyEncryptionMethod());
        Assert.assertEquals("someKey", convert.getGcpParameters().getEncryption().getKey());
    }

    @Test
    public void testOpenStackTemplateParametersWhenNotNull() throws JsonProcessingException {

        Template template = new Template();
        template.setAttributes(parameters(CloudConstants.OPENSTACK));

        TemplateResponse convert = underTest.convert(template);

        Assert.assertNotNull(convert.getOpenStackParameters());
    }

    @Test
    public void testTemplateParametersWhenNull() throws JsonProcessingException {

        Template template = new Template();
        template.setAttributes(new Json(Collections.singletonMap("key", "value")));

        TemplateResponse convert = underTest.convert(template);

        Assert.assertNull(convert.getAwsParameters());
        Assert.assertNull(convert.getAzureParameters());
        Assert.assertNull(convert.getGcpParameters());
        Assert.assertNull(convert.getOpenStackParameters());
    }

    private Json parameters(String cloudConstants) throws JsonProcessingException {
        Map<String, Object> map = new HashMap<>();
        map.put(BaseTemplateParameter.PLATFORM_TYPE, cloudConstants);
        return new Json(map);
    }

    private AwsParameters awsParameters() throws JsonProcessingException {
        AwsParameters templateParameters = new AwsParameters();
        templateParameters.setSpotPrice(10.0D);
        templateParameters.setEncrypted(true);
        AwsEncryption awsEncryption = new AwsEncryption();
        awsEncryption.setKey("someKey");
        awsEncryption.setType("CUSTOM");
        templateParameters.setAwsEncryption(awsEncryption);
        return templateParameters;
    }

    private OpenStackParameters openStackParameters() throws JsonProcessingException {
        return new OpenStackParameters();
    }

    private GcpParameters gcpParameters() throws JsonProcessingException {
        GcpParameters templateParameters = new GcpParameters();
        GcpEncryption encryption = new GcpEncryption();
        encryption.setKeyEncryptionMethod("RAW");
        encryption.setType("CUSTOM");
        encryption.setKey("someKey");
        templateParameters.setEncryption(encryption);
        return templateParameters;
    }

    private AzureParameters azureParameters() throws JsonProcessingException {
        AzureParameters templateParameters = new AzureParameters();
        templateParameters.setPrivateId("somePrivateId");
        return templateParameters;
    }

    private YarnParameters yarnParameters() throws JsonProcessingException {
        return new YarnParameters();
    }
}
