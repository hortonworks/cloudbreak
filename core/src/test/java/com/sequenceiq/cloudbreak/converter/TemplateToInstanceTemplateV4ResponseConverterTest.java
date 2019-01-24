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
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.AwsEncryptionV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.AwsInstanceTemplateV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.AzureInstanceTemplateV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.GcpEncryptionV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.GcpInstanceTemplateV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.OpenStackInstanceTemplateV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.YarnInstanceTemplateV4Parameters;
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
        when(conversionService.convert(parameters(CloudConstants.AWS).getMap(), AwsInstanceTemplateV4Parameters.class)).thenReturn(awsParameters());
        when(conversionService.convert(parameters(CloudConstants.YARN).getMap(), YarnInstanceTemplateV4Parameters.class)).thenReturn(yarnParameters());
        when(conversionService.convert(parameters(CloudConstants.AZURE).getMap(), AzureInstanceTemplateV4Parameters.class)).thenReturn(azureParameters());
        when(conversionService.convert(parameters(CloudConstants.GCP).getMap(), GcpInstanceTemplateV4Parameters.class)).thenReturn(gcpParameters());
        when(conversionService.convert(parameters(CloudConstants.OPENSTACK).getMap(), OpenStackInstanceTemplateV4Parameters.class)).thenReturn(openStackParameters());
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

    private AwsInstanceTemplateV4Parameters awsParameters() {
        AwsInstanceTemplateV4Parameters templateParameters = new AwsInstanceTemplateV4Parameters();
        templateParameters.setSpotPrice(10.0D);
        AwsEncryptionV4Parameters awsEncryption = new AwsEncryptionV4Parameters();
        awsEncryption.setKey("someKey");
        awsEncryption.setType(EncryptionType.CUSTOM);
        templateParameters.setEncryption(awsEncryption);
        return templateParameters;
    }

    private OpenStackInstanceTemplateV4Parameters openStackParameters() {
        return new OpenStackInstanceTemplateV4Parameters();
    }

    private GcpInstanceTemplateV4Parameters gcpParameters() {
        GcpInstanceTemplateV4Parameters templateParameters = new GcpInstanceTemplateV4Parameters();
        GcpEncryptionV4Parameters encryption = new GcpEncryptionV4Parameters();
        encryption.setKeyEncryptionMethod(KeyEncryptionMethod.RAW);
        encryption.setType(EncryptionType.CUSTOM);
        encryption.setKey("someKey");
        templateParameters.setEncryption(encryption);
        return templateParameters;
    }

    private AzureInstanceTemplateV4Parameters azureParameters() {
        AzureInstanceTemplateV4Parameters templateParameters = new AzureInstanceTemplateV4Parameters();
        templateParameters.setPrivateId("somePrivateId");
        return templateParameters;
    }

    private YarnInstanceTemplateV4Parameters yarnParameters() throws JsonProcessingException {
        return new YarnInstanceTemplateV4Parameters();
    }
}
