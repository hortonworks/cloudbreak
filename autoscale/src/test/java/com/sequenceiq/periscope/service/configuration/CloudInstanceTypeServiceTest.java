package com.sequenceiq.periscope.service.configuration;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.AutoscaleV4Endpoint;
import com.sequenceiq.cloudbreak.client.CloudbreakInternalCrnClient;
import com.sequenceiq.cloudbreak.client.CloudbreakServiceCrnEndpoints;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.service.CloudbreakResourceReaderService;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.periscope.model.CloudInstanceType;

@RunWith(MockitoJUnitRunner.class)
public class CloudInstanceTypeServiceTest {

    @InjectMocks
    private CloudInstanceTypeService underTest;

    @Mock
    private CloudbreakResourceReaderService cloudbreakResourceReaderService;

    @Mock
    private CloudbreakInternalCrnClient internalCrnClient;

    @Mock
    private CloudbreakServiceCrnEndpoints cloudbreakServiceCrnEndpoints;

    @Mock
    private AutoscaleV4Endpoint autoscaleV4Endpoint;

    private Set<CloudInstanceType> awsInstances;

    private Set<CloudInstanceType> azureInstances;

    @Before
    public void setUp() throws Exception {
        String awsJson = FileReaderUtils.readFileFromClasspath("definitions/aws-vm.json");
        String azureJson = FileReaderUtils.readFileFromClasspath("definitions/azure-vm.json");

        TypeReference<HashMap<String, Set<CloudInstanceType>>> typeRef = new TypeReference<>() {
        };
        awsInstances = JsonUtil.readValue(awsJson, typeRef).get("items");
        azureInstances = JsonUtil.readValue(azureJson, typeRef).get("items");
        Set<String> cbAwsInstanceNames = awsInstances.stream().map(CloudInstanceType::getInstanceName).collect(Collectors.toSet());
        Set<String> cbAzureInstanceNames = azureInstances.stream().map(CloudInstanceType::getInstanceName).collect(Collectors.toSet());

        when(cloudbreakResourceReaderService.resourceDefinition("aws", "vm")).thenReturn(awsJson);
        when(cloudbreakResourceReaderService.resourceDefinition("azure", "vm")).thenReturn(azureJson);
        when(internalCrnClient.withInternalCrn()).thenReturn(cloudbreakServiceCrnEndpoints);
        when(cloudbreakServiceCrnEndpoints.autoscaleEndpoint()).thenReturn(autoscaleV4Endpoint);
        when(autoscaleV4Endpoint.getSupportedDistroXInstanceTypes("aws")).thenReturn(cbAwsInstanceNames);
        when(autoscaleV4Endpoint.getSupportedDistroXInstanceTypes("azure")).thenReturn(cbAzureInstanceNames);
        underTest.init();
    }

    @Test
    public void testCloudPlatformInstanceTypesCount() {
        Assert.assertEquals("Configured AWS Instance Count should match", 34, awsInstances.size());
        Assert.assertEquals("Configured AZURE Instance Count should match", 36, azureInstances.size());
    }

    @Test
    public void testAWSInstanceTypes() {
        testCloudInstanceType(CloudPlatform.AWS, "m5.large", 2, 8f);
        testCloudInstanceType(CloudPlatform.AWS, "m5.xlarge", 4, 16f);
        testCloudInstanceType(CloudPlatform.AWS, "m5.2xlarge", 8, 32f);
        testCloudInstanceType(CloudPlatform.AWS, "m5.4xlarge", 16, 64f);
        testCloudInstanceType(CloudPlatform.AWS, "m5.8xlarge", 32, 128f);

        testCloudInstanceType(CloudPlatform.AWS, "d2.xlarge", 4, 30.5f);
        testCloudInstanceType(CloudPlatform.AWS, "d2.2xlarge", 8, 61f);
        testCloudInstanceType(CloudPlatform.AWS, "d2.4xlarge", 16, 122f);
        testCloudInstanceType(CloudPlatform.AWS, "d2.8xlarge", 36, 244f);

        testCloudInstanceType(CloudPlatform.AWS, "r5.large", 2, 16f);
        testCloudInstanceType(CloudPlatform.AWS, "r5.xlarge", 4, 32f);
        testCloudInstanceType(CloudPlatform.AWS, "r5.2xlarge", 8, 64f);
        testCloudInstanceType(CloudPlatform.AWS, "r5.4xlarge", 16, 128f);
        testCloudInstanceType(CloudPlatform.AWS, "r5.8xlarge", 32, 256f);

        testCloudInstanceType(CloudPlatform.AWS, "r5d.large", 2, 16f);
        testCloudInstanceType(CloudPlatform.AWS, "r5d.xlarge", 4, 32f);
        testCloudInstanceType(CloudPlatform.AWS, "r5d.2xlarge", 8, 64f);
        testCloudInstanceType(CloudPlatform.AWS, "r5d.4xlarge", 16, 128f);
        testCloudInstanceType(CloudPlatform.AWS, "r5d.8xlarge", 32, 256f);

        testCloudInstanceType(CloudPlatform.AWS, "c5.large", 2, 4f);
        testCloudInstanceType(CloudPlatform.AWS, "c5.xlarge", 4, 8f);
        testCloudInstanceType(CloudPlatform.AWS, "c5.2xlarge", 8, 16f);
        testCloudInstanceType(CloudPlatform.AWS, "c5.4xlarge", 16, 32f);
        testCloudInstanceType(CloudPlatform.AWS, "c5.9xlarge", 36, 72f);

        testCloudInstanceType(CloudPlatform.AWS, "i3.large", 2, 15.25f);
        testCloudInstanceType(CloudPlatform.AWS, "i3.xlarge", 4, 30.5f);
        testCloudInstanceType(CloudPlatform.AWS, "i3.2xlarge", 8, 61f);
        testCloudInstanceType(CloudPlatform.AWS, "i3.4xlarge", 16, 122f);
        testCloudInstanceType(CloudPlatform.AWS, "i3.8xlarge", 32, 244f);

        testCloudInstanceType(CloudPlatform.AWS, "h1.2xlarge", 8, 32f);
        testCloudInstanceType(CloudPlatform.AWS, "h1.4xlarge", 16, 64f);
        testCloudInstanceType(CloudPlatform.AWS, "h1.8xlarge", 32, 128f);

        testCloudInstanceType(CloudPlatform.AWS, "p3.2xlarge", 8, 61f);
        testCloudInstanceType(CloudPlatform.AWS, "p3.8xlarge", 32, 244f);
    }

    @Test
    public void testAzureInstanceTypes() {
        testCloudInstanceType(CloudPlatform.AZURE, "Standard_D2_v3", 2, 8f);
        testCloudInstanceType(CloudPlatform.AZURE, "Standard_D4_v3", 4, 16f);
        testCloudInstanceType(CloudPlatform.AZURE, "Standard_D8_v3", 8, 32f);
        testCloudInstanceType(CloudPlatform.AZURE, "Standard_D16_v3", 16, 64f);
        testCloudInstanceType(CloudPlatform.AZURE, "Standard_D32_v3", 32, 128f);
        testCloudInstanceType(CloudPlatform.AZURE, "Standard_D48_v3", 48, 192f);
        testCloudInstanceType(CloudPlatform.AZURE, "Standard_D64_v3", 64, 256f);

        testCloudInstanceType(CloudPlatform.AZURE, "Standard_E2_v3", 2, 16f);
        testCloudInstanceType(CloudPlatform.AZURE, "Standard_E4_v3", 4, 32f);
        testCloudInstanceType(CloudPlatform.AZURE, "Standard_E8_v3", 8, 64f);
        testCloudInstanceType(CloudPlatform.AZURE, "Standard_E16_v3", 16, 128f);
        testCloudInstanceType(CloudPlatform.AZURE, "Standard_E20_v3", 20, 160f);
        testCloudInstanceType(CloudPlatform.AZURE, "Standard_E32_v3", 32, 256f);
        testCloudInstanceType(CloudPlatform.AZURE, "Standard_E48_v3", 48, 384f);
        testCloudInstanceType(CloudPlatform.AZURE, "Standard_E64_v3", 64, 432f);
        testCloudInstanceType(CloudPlatform.AZURE, "Standard_E64i_v3", 64, 432f);

        testCloudInstanceType(CloudPlatform.AZURE, "Standard_F2s_v2", 2, 4f);
        testCloudInstanceType(CloudPlatform.AZURE, "Standard_F4s_v2", 4, 8f);
        testCloudInstanceType(CloudPlatform.AZURE, "Standard_F8s_v2", 8, 16f);
        testCloudInstanceType(CloudPlatform.AZURE, "Standard_F16s_v2", 16, 32f);
        testCloudInstanceType(CloudPlatform.AZURE, "Standard_F32s_v2", 32, 64f);
        testCloudInstanceType(CloudPlatform.AZURE, "Standard_F48s_v2", 48, 96f);
        testCloudInstanceType(CloudPlatform.AZURE, "Standard_F64s_v2", 64, 128f);

        testCloudInstanceType(CloudPlatform.AZURE, "Standard_L8s_v2", 8, 64f);
        testCloudInstanceType(CloudPlatform.AZURE, "Standard_L16s_v2", 16, 128f);
        testCloudInstanceType(CloudPlatform.AZURE, "Standard_L32s_v2", 32, 256f);
        testCloudInstanceType(CloudPlatform.AZURE, "Standard_L48s_v2", 48, 384f);
        testCloudInstanceType(CloudPlatform.AZURE, "Standard_L64s_v2", 64, 512f);

        testCloudInstanceType(CloudPlatform.AZURE, "Standard_L4s", 4, 32f);
        testCloudInstanceType(CloudPlatform.AZURE, "Standard_L8s", 8, 64f);
        testCloudInstanceType(CloudPlatform.AZURE, "Standard_L16s", 16, 128f);
        testCloudInstanceType(CloudPlatform.AZURE, "Standard_L32s", 32, 256f);

        testCloudInstanceType(CloudPlatform.AZURE, "Standard_NC6", 6, 56f);
        testCloudInstanceType(CloudPlatform.AZURE, "Standard_NC12", 12, 112f);
        testCloudInstanceType(CloudPlatform.AZURE, "Standard_NC24", 24, 224f);
        testCloudInstanceType(CloudPlatform.AZURE, "Standard_NC24r", 24, 224f);
    }

    private void testCloudInstanceType(CloudPlatform cloudPlatform, String hostGroupInstanceType, Integer expectedVCpu, Float expectedMemory) {
        underTest.getCloudVMInstanceType(cloudPlatform, hostGroupInstanceType).ifPresentOrElse(cloudInstanceType -> {
            Integer memoryInMB = Math.round(expectedMemory * 1024);
            assertEquals("Retrieved CloudInstance Name should match for " + hostGroupInstanceType,
                    hostGroupInstanceType, cloudInstanceType.getInstanceName());
            assertEquals("Retrieved CloudInstance Memory in GB should match for " + hostGroupInstanceType,
                    expectedMemory, cloudInstanceType.getMemoryInGB());
            assertEquals("Retrieved CloudInstance Memory in MB should match for " + hostGroupInstanceType,
                    memoryInMB, cloudInstanceType.getMemoryInMB());
            assertEquals("Retrieved CloudInstance CPU should match for " + hostGroupInstanceType,
                    expectedVCpu, cloudInstanceType.getCoreCPU());
        }, () -> fail(hostGroupInstanceType + " hostGroupInstanceType not found."));
    }
}
