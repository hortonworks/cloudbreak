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
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.EncryptionType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.KeyEncryptionMethod;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.AwsEncryptionParametersV4;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.AwsInstanceTemplateParametersV4;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.AzureInstanceTemplateParametersV4;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.GcpEncryptionParametersV4;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.GcpInstanceTemplateParametersV4;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.OpenStackInstanceTemplateParametersV4;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.YarnInstanceTemplateParametersV4;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.template.InstanceTemplateV4Response;
import com.sequenceiq.cloudbreak.common.type.CloudConstants;
import com.sequenceiq.cloudbreak.converter.v4.stacks.instancegroup.template.TemplateToInstanceTemplateV4ResponseConverter;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.json.Json;

public class TemplateToInstanceTemplateV4ResponseConverterTest {

    @Mock
    private ConversionService conversionService;

    @InjectMocks
    private TemplateToInstanceTemplateV4ResponseConverter underTest;

    @Before
    public void setUp() throws JsonProcessingException {
        MockitoAnnotations.initMocks(this);
        when(conversionService.convert(parameters(CloudConstants.AWS).getMap(), AwsInstanceTemplateParametersV4.class)).thenReturn(awsParameters());
        when(conversionService.convert(parameters(CloudConstants.YARN).getMap(), YarnInstanceTemplateParametersV4.class)).thenReturn(yarnParameters());
        when(conversionService.convert(parameters(CloudConstants.AZURE).getMap(), AzureInstanceTemplateParametersV4.class)).thenReturn(azureParameters());
        when(conversionService.convert(parameters(CloudConstants.GCP).getMap(), GcpInstanceTemplateParametersV4.class)).thenReturn(gcpParameters());
        when(conversionService.convert(parameters(CloudConstants.OPENSTACK).getMap(), OpenStackInstanceTemplateParametersV4.class)).thenReturn(openStackParameters());
    }

    @Test
    public void testAwsTemplateParametersWhenNotNull() throws JsonProcessingException {

        Template template = new Template();
        template.setAttributes(parameters(CloudConstants.AWS));

        InstanceTemplateV4Response convert = underTest.convert(template);

        Assert.assertNotNull(convert.getAws());
        Assert.assertEquals(10.0D, convert.getAws().getSpotPrice().doubleValue(), 0);
        Assert.assertEquals("someKey", convert.getAws().getEncryption().getKey());
        Assert.assertEquals(EncryptionType.CUSTOM.name(), convert.getAws().getEncryption().getType());
        Assert.assertNull(convert.getAzure());
        Assert.assertNull(convert.getGcp());
        Assert.assertNull(convert.getOpenstack());
    }

    @Test
    public void testAzureTemplateParametersWhenNotNull() throws JsonProcessingException {

        Template template = new Template();
        template.setAttributes(parameters(CloudConstants.AZURE));

        InstanceTemplateV4Response convert = underTest.convert(template);

        Assert.assertNotNull(convert.getAzure());
        Assert.assertEquals("somePrivateId", convert.getAzure().getPrivateId());
    }

    @Test
    public void testGcpTemplateParametersWhenNotNull() throws JsonProcessingException {

        Template template = new Template();
        template.setAttributes(parameters(CloudConstants.GCP));

        InstanceTemplateV4Response convert = underTest.convert(template);

        Assert.assertNotNull(convert.getGcp());
        Assert.assertEquals(EncryptionType.CUSTOM.name(), convert.getGcp().getEncryption().getType());
        Assert.assertEquals(KeyEncryptionMethod.RAW.name(), convert.getGcp().getEncryption().getKeyEncryptionMethod());
        Assert.assertEquals("someKey", convert.getGcp().getEncryption().getKey());
    }

    @Test
    public void testOpenStackTemplateParametersWhenNotNull() throws JsonProcessingException {

        Template template = new Template();
        template.setAttributes(parameters(CloudConstants.OPENSTACK));

        InstanceTemplateV4Response convert = underTest.convert(template);

        Assert.assertNotNull(convert.getOpenstack());
    }

    @Test
    public void testTemplateParametersWhenNull() throws JsonProcessingException {

        Template template = new Template();
        template.setAttributes(new Json(Collections.singletonMap("key", "value")));

        InstanceTemplateV4Response convert = underTest.convert(template);

        Assert.assertNull(convert.getAws());
        Assert.assertNull(convert.getAzure());
        Assert.assertNull(convert.getGcp());
        Assert.assertNull(convert.getOpenstack());
    }

    private Json parameters(String cloudConstants) throws JsonProcessingException {
        Map<String, Object> map = new HashMap<>();
        map.put("platformType", cloudConstants);
        return new Json(map);
    }

    private AwsInstanceTemplateParametersV4 awsParameters() {
        AwsInstanceTemplateParametersV4 templateParameters = new AwsInstanceTemplateParametersV4();
        templateParameters.setSpotPrice(10.0D);
        AwsEncryptionParametersV4 awsEncryption = new AwsEncryptionParametersV4();
        awsEncryption.setKey("someKey");
        awsEncryption.setType(EncryptionType.CUSTOM);
        templateParameters.setEncryption(awsEncryption);
        return templateParameters;
    }

    private OpenStackInstanceTemplateParametersV4 openStackParameters() {
        return new OpenStackInstanceTemplateParametersV4();
    }

    private GcpInstanceTemplateParametersV4 gcpParameters() {
        GcpInstanceTemplateParametersV4 templateParameters = new GcpInstanceTemplateParametersV4();
        GcpEncryptionParametersV4 encryption = new GcpEncryptionParametersV4();
        encryption.setKeyEncryptionMethod(KeyEncryptionMethod.RAW);
        encryption.setType(EncryptionType.CUSTOM);
        encryption.setKey("someKey");
        templateParameters.setEncryption(encryption);
        return templateParameters;
    }

    private AzureInstanceTemplateParametersV4 azureParameters() {
        AzureInstanceTemplateParametersV4 templateParameters = new AzureInstanceTemplateParametersV4();
        templateParameters.setPrivateId("somePrivateId");
        return templateParameters;
    }

    private YarnInstanceTemplateParametersV4 yarnParameters() throws JsonProcessingException {
        return new YarnInstanceTemplateParametersV4();
    }
}
