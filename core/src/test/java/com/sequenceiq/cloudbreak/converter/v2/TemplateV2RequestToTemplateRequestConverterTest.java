package com.sequenceiq.cloudbreak.converter.v2;

import static com.sequenceiq.cloudbreak.api.model.v2.template.BaseTemplateParameter.PLATFORM_TYPE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.convert.ConversionService;

import com.sequenceiq.cloudbreak.api.model.CustomInstanceType;
import com.sequenceiq.cloudbreak.api.model.TemplateRequest;
import com.sequenceiq.cloudbreak.api.model.v2.TemplateV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.template.AwsEncryption;
import com.sequenceiq.cloudbreak.api.model.v2.template.AwsParameters;
import com.sequenceiq.cloudbreak.api.model.v2.template.AzureParameters;
import com.sequenceiq.cloudbreak.api.model.v2.template.EncryptionType;
import com.sequenceiq.cloudbreak.api.model.v2.template.GcpEncryption;
import com.sequenceiq.cloudbreak.api.model.v2.template.GcpParameters;
import com.sequenceiq.cloudbreak.api.model.v2.template.KeyEncryptionMethod;
import com.sequenceiq.cloudbreak.api.model.v2.template.OpenStackParameters;
import com.sequenceiq.cloudbreak.api.model.v2.template.YarnParameters;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.common.type.CloudConstants;
import com.sequenceiq.cloudbreak.converter.v2.template.AwsTemplateParametersToParametersConverter;
import com.sequenceiq.cloudbreak.converter.v2.template.AzureTemplateParametersToParametersConverter;
import com.sequenceiq.cloudbreak.converter.v2.template.GcpTemplateParametersToParametersConverter;
import com.sequenceiq.cloudbreak.converter.v2.template.OpenStackTemplateParametersToParametersConverter;
import com.sequenceiq.cloudbreak.converter.v2.template.YarnTemplateParametersToParametersConverter;
import com.sequenceiq.cloudbreak.service.MissingResourceNameGenerator;
import com.sequenceiq.cloudbreak.service.topology.TopologyService;

@RunWith(MockitoJUnitRunner.class)
public class TemplateV2RequestToTemplateRequestConverterTest {

    @InjectMocks
    private TemplateV2RequestToTemplateRequestConverter underTest;

    @Mock
    private TopologyService topologyService;

    @Mock
    private MissingResourceNameGenerator missingResourceNameGenerator;

    @Mock
    private ConversionService conversionService;

    @Before
    public void setUp() {
        Mockito.reset(conversionService);
    }

    @Test
    public void convertWithAwsParameters() {
        TemplateV2Request request = createBaseTemplateV2Request();

        Map<String, Object> parameters = new HashMap<>();
        request.setParameters(parameters);

        AwsParameters awsParameters = createAwsParameters();
        request.setAwsParameters(awsParameters);

        when(missingResourceNameGenerator.generateName(APIResourceType.TEMPLATE)).thenReturn("name");

        AwsTemplateParametersToParametersConverter converter = new AwsTemplateParametersToParametersConverter();
        when(conversionService.convert(awsParameters, Map.class))
                .thenReturn(converter.convert(awsParameters));

        TemplateRequest result = underTest.convert(request);

        assertCommonFields(request, result);

        assertNotNull(result.getParameters());
        assertEquals(request.getParameters(), result.getParameters());
        assertEquals(awsParameters.isEncrypted(), result.getParameters().get("encrypted"));
        assertEquals(awsParameters.getEncryption().getKey(), result.getParameters().get("key"));
        assertEquals(EncryptionType.CUSTOM, result.getParameters().get("type"));
        assertEquals(awsParameters.getSpotPrice(), result.getParameters().get("spotPrice"));
    }

    public void assertCommonFields(TemplateV2Request request, TemplateRequest result) {
        assertEquals("name", result.getName());
        assertEquals(request.getVolumeCount(), result.getVolumeCount());
        assertEquals(request.getVolumeSize(), result.getVolumeSize());
        assertEquals(request.getInstanceType(), result.getInstanceType());
        assertEquals(request.getRootVolumeSize(), result.getRootVolumeSize());
        assertEquals(request.getVolumeType(), result.getVolumeType());
        assertEquals(request.getCustomInstanceType(), result.getCustomInstanceType());
    }

    @Test
    public void convertWithAzureParameters() {
        TemplateV2Request request = createBaseTemplateV2Request();

        Map<String, Object> parameters = new HashMap<>();
        request.setParameters(parameters);

        AzureParameters azureParameters = createAzureParameters();
        request.setAzureParameters(azureParameters);

        when(missingResourceNameGenerator.generateName(APIResourceType.TEMPLATE)).thenReturn("name");

        AzureTemplateParametersToParametersConverter converter = new AzureTemplateParametersToParametersConverter();
        when(conversionService.convert(azureParameters, Map.class))
                .thenReturn(converter.convert(azureParameters));

        TemplateRequest result = underTest.convert(request);

        assertCommonFields(request, result);

        assertNotNull(result.getParameters());
        assertEquals(request.getParameters(), result.getParameters());
        assertEquals(azureParameters.getPrivateId(), result.getParameters().get("privateId"));
    }

    @Test
    public void convertWithGcpParameters() {
        TemplateV2Request request = createBaseTemplateV2Request();

        Map<String, Object> parameters = new HashMap<>();
        request.setParameters(parameters);

        GcpParameters gcpTemplateParameters = createGcpParameters();
        request.setGcpParameters(gcpTemplateParameters);

        when(missingResourceNameGenerator.generateName(APIResourceType.TEMPLATE)).thenReturn("name");

        GcpTemplateParametersToParametersConverter converter = new GcpTemplateParametersToParametersConverter();
        when(conversionService.convert(gcpTemplateParameters, Map.class))
                .thenReturn(converter.convert(gcpTemplateParameters));

        TemplateRequest result = underTest.convert(request);

        assertCommonFields(request, result);

        assertNotNull(result.getParameters());
        assertEquals(request.getParameters(), result.getParameters());
        assertTrue(result.getParameters().isEmpty());

        assertNotNull(result.getSecretParameters());
        assertEquals(gcpTemplateParameters.getEncryption().getKey(), result.getSecretParameters().get("key"));
        assertEquals(EncryptionType.CUSTOM, result.getSecretParameters().get("type"));
        assertEquals(KeyEncryptionMethod.RAW, result.getSecretParameters().get("keyEncryptionMethod"));
    }

    @Test
    public void convertWithOpenstackParameters() {
        TemplateV2Request request = createBaseTemplateV2Request();

        Map<String, Object> parameters = new HashMap<>();
        request.setParameters(parameters);

        OpenStackParameters openStackParameters = new OpenStackParameters();
        request.setOpenStackParameters(openStackParameters);

        when(missingResourceNameGenerator.generateName(APIResourceType.TEMPLATE)).thenReturn("name");

        OpenStackTemplateParametersToParametersConverter parametersConverter = new OpenStackTemplateParametersToParametersConverter();
        when(conversionService.convert(openStackParameters, Map.class))
                .thenReturn(parametersConverter.convert(openStackParameters));

        TemplateRequest result = underTest.convert(request);

        assertCommonFields(request, result);

        assertNotNull(result.getParameters());
        assertEquals(request.getParameters(), result.getParameters());
        assertEquals(CloudConstants.OPENSTACK, result.getParameters().get(PLATFORM_TYPE));
    }

    @Test
    public void convertWithYarnParameters() {
        TemplateV2Request request = createBaseTemplateV2Request();

        Map<String, Object> parameters = new HashMap<>();
        request.setParameters(parameters);

        YarnParameters yarnParameters = new YarnParameters();
        request.setYarnParameters(yarnParameters);

        when(missingResourceNameGenerator.generateName(APIResourceType.TEMPLATE)).thenReturn("name");

        YarnTemplateParametersToParametersConverter converter = new YarnTemplateParametersToParametersConverter();
        when(conversionService.convert(yarnParameters, Map.class))
                .thenReturn(converter.convert(yarnParameters));

        TemplateRequest result = underTest.convert(request);

        assertCommonFields(request, result);

        assertNotNull(result.getParameters());
        assertEquals(request.getParameters(), result.getParameters());
        assertEquals(CloudConstants.YARN, result.getParameters().get(PLATFORM_TYPE));
    }

    public GcpParameters createGcpParameters() {
        GcpParameters gcpTemplateParameters = new GcpParameters();
        GcpEncryption gcpEncryption = new GcpEncryption();
        gcpEncryption.setKeyEncryptionMethod("RAW");
        gcpEncryption.setKey("key");
        gcpEncryption.setType("CUSTOM");
        gcpTemplateParameters.setEncryption(gcpEncryption);
        return gcpTemplateParameters;
    }

    public AzureParameters createAzureParameters() {
        AzureParameters azureParameters = new AzureParameters();
        azureParameters.setPrivateId("privateId");
        return azureParameters;
    }

    public AwsParameters createAwsParameters() {
        AwsParameters awsParameters = new AwsParameters();
        awsParameters.setEncrypted(true);
        AwsEncryption awsEncryption = new AwsEncryption();
        awsEncryption.setKey("awsKey");
        awsEncryption.setType("CUSTOM");
        awsParameters.setEncryption(awsEncryption);
        awsParameters.setSpotPrice(0.33);
        return awsParameters;
    }

    public TemplateV2Request createBaseTemplateV2Request() {
        TemplateV2Request request = new TemplateV2Request();
        request.setVolumeCount(1);
        request.setVolumeSize(1);
        request.setInstanceType("instanceType");
        request.setRootVolumeSize(1);
        request.setVolumeType("volumeType");
        request.setCustomInstanceType(new CustomInstanceType());
        return request;
    }
}