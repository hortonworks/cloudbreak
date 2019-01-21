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
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceTemplateParameterV4Base;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.KeyEncryptionMethod;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.AwsEncryptionParametersV4;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.AwsInstanceTemplateParametersV4;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.AzureInstanceTemplateParametersV4;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.GcpInstanceTemplateParametersV4;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.OpenStackInstanceTemplateParametersV4;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.YarnInstanceTemplateParametersV4;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.template.InstanceTemplateV4Response;
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
import com.sequenceiq.cloudbreak.converter.v4.stacks.instancegroup.template.TemplateToInstanceTemplateV4ResponseConverter;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.json.Json;

public class TemplateToTemplateResponseConverterTest {

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
        Assert.assertNull(convert.getOpenStack());
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

        Assert.assertNotNull(convert.getOpenStack());
    }

    @Test
    public void testTemplateParametersWhenNull() throws JsonProcessingException {

        Template template = new Template();
        template.setAttributes(new Json(Collections.singletonMap("key", "value")));

        InstanceTemplateV4Response convert = underTest.convert(template);

        Assert.assertNull(convert.getAws());
        Assert.assertNull(convert.getAzure());
        Assert.assertNull(convert.getGcp());
        Assert.assertNull(convert.getOpenStack());
    }

    private AwsInstanceTemplateParametersV4 awsParameters() throws JsonProcessingException {
        AwsInstanceTemplateParametersV4 templateParameters = new AwsInstanceTemplateParametersV4();
        templateParameters.setSpotPrice(10.0D);
        AwsEncryptionParametersV4 awsEncryption = new AwsEncryptionParametersV4();
        awsEncryption.setKey("someKey");
        awsEncryption.setType(EncryptionType.CUSTOM);
        templateParameters.setEncryption(awsEncryption);
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
