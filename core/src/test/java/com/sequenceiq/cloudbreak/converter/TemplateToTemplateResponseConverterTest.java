package com.sequenceiq.cloudbreak.converter;

import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.api.model.TemplateResponse;
import com.sequenceiq.cloudbreak.api.model.v2.template.AwsTemplateParameters;
import com.sequenceiq.cloudbreak.api.model.v2.template.AzureTemplateParameters;
import com.sequenceiq.cloudbreak.api.model.v2.template.BaseTemplateParameter;
import com.sequenceiq.cloudbreak.api.model.v2.template.Encryption;
import com.sequenceiq.cloudbreak.api.model.v2.template.EncryptionType;
import com.sequenceiq.cloudbreak.api.model.v2.template.GcpTemplateParameters;
import com.sequenceiq.cloudbreak.api.model.v2.template.OpenStackTemplateParameters;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.json.Json;

public class TemplateToTemplateResponseConverterTest {

    private final TemplateToTemplateResponseConverter underTest = new TemplateToTemplateResponseConverter();

    @Test
    public void testAwsTemplateParametersWhenNotNull() throws JsonProcessingException {

        Template template = new Template();
        template.setAttributes(getAwsJson());

        TemplateResponse convert = underTest.convert(template);

        Assert.assertNotNull(convert.getAwsTemplateParameters());
        Assert.assertEquals(10.0D, convert.getAwsTemplateParameters().getSpotPrice().doubleValue(), 0);
        Assert.assertEquals("someKey", convert.getAwsTemplateParameters().getEncryption().getKey());
        Assert.assertEquals(EncryptionType.CUSTOM.name(), convert.getAwsTemplateParameters().getEncryption().getType());
        Assert.assertNull(convert.getAzureTemplateParameters());
        Assert.assertNull(convert.getGcpTemlateParameters());
        Assert.assertNull(convert.getOpenStackTemplateParameters());
    }

    @Test
    public void testAzureTemplateParametersWhenNotNull() throws JsonProcessingException {

        Template template = new Template();
        template.setAttributes(getAzureJson());

        TemplateResponse convert = underTest.convert(template);

        Assert.assertNotNull(convert.getAzureTemplateParameters());
        Assert.assertEquals("somePrivateId", convert.getAzureTemplateParameters().getPrivateId());
    }

    @Test
    public void testGcpTemplateParametersWhenNotNull() throws JsonProcessingException {

        Template template = new Template();
        template.setAttributes(getGcpJson());

        TemplateResponse convert = underTest.convert(template);

        Assert.assertNotNull(convert.getGcpTemlateParameters());
        Assert.assertNull(convert.getGcpTemlateParameters().getEncryption());
        Assert.assertNull(convert.getGcpTemlateParameters().isEncrypted());
    }

    @Test
    public void testOpenStackTemplateParametersWhenNotNull() throws JsonProcessingException {

        Template template = new Template();
        template.setAttributes(getOSJson());

        TemplateResponse convert = underTest.convert(template);

        Assert.assertNotNull(convert.getOpenStackTemplateParameters());
    }

    @Test
    public void testTemplateParametersWhenNull() throws JsonProcessingException {

        Template template = new Template();
        template.setAttributes(new Json(Collections.singletonMap("key", "value")));

        TemplateResponse convert = underTest.convert(template);

        Assert.assertNull(convert.getAwsTemplateParameters());
        Assert.assertNull(convert.getAzureTemplateParameters());
        Assert.assertNull(convert.getGcpTemlateParameters());
        Assert.assertNull(convert.getOpenStackTemplateParameters());
    }

    private Json getAwsJson() throws JsonProcessingException {
        AwsTemplateParameters templateParameters = new AwsTemplateParameters();
        templateParameters.setSpotPrice(10.0D);
        fillEncrypt(templateParameters);
        return new Json(templateParameters.asMap());
    }

    private Json getOSJson() throws JsonProcessingException {
        OpenStackTemplateParameters templateParameters = new OpenStackTemplateParameters();
        fillEncrypt(templateParameters);
        return new Json(templateParameters.asMap());
    }

    private Json getGcpJson() throws JsonProcessingException {
        GcpTemplateParameters templateParameters = new GcpTemplateParameters();
        return new Json(templateParameters.asMap());
    }

    private Json getAzureJson() throws JsonProcessingException {
        AzureTemplateParameters templateParameters = new AzureTemplateParameters();
        templateParameters.setPrivateId("somePrivateId");
        fillEncrypt(templateParameters);
        return new Json(templateParameters.asMap());
    }

    private void fillEncrypt(BaseTemplateParameter baseTemplateParameters) {
        baseTemplateParameters.setEncrypted(true);
        Encryption encryption = new Encryption();
        encryption.setKey("someKey");
        encryption.setType("CUSTOM");
        baseTemplateParameters.setEncryption(encryption);
    }
}
