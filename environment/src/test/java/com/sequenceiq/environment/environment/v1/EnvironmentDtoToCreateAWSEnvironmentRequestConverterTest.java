package com.sequenceiq.environment.environment.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.cloudera.cdp.environments.model.CreateAWSEnvironmentRequest;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.environment.dto.AuthenticationDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.LocationDto;
import com.sequenceiq.environment.environment.dto.SecurityAccessDto;
import com.sequenceiq.environment.environment.dto.telemetry.EnvironmentLogging;
import com.sequenceiq.environment.environment.dto.telemetry.EnvironmentTelemetry;
import com.sequenceiq.environment.environment.dto.telemetry.S3CloudStorageParameters;
import com.sequenceiq.environment.network.dto.AwsParams;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.environment.parameters.dto.AwsParametersDto;
import com.sequenceiq.environment.parameters.dto.ParametersDto;

class EnvironmentDtoToCreateAWSEnvironmentRequestConverterTest {

    private EnvironmentDtoToCreateAWSEnvironmentRequestConverter underTest;

    @BeforeEach
    void setUp() {
        underTest = new EnvironmentDtoToCreateAWSEnvironmentRequestConverter();
    }

    @Test
    void convert() {
        EnvironmentDto environmentDto = getEnvironmentDto();
        CreateAWSEnvironmentRequest result = underTest.convert(environmentDto);
        assertEquals(environmentDto.getAuthentication().getPublicKey(), result.getAuthentication().getPublicKey());
        assertEquals(environmentDto.getAuthentication().getPublicKeyId(), result.getAuthentication().getPublicKeyId());
        assertEquals(environmentDto.getCredential().getName(), result.getCredentialName());
        assertEquals(environmentDto.getDescription(), result.getDescription());
        assertEquals(environmentDto.getName(), result.getEnvironmentName());
        assertEquals(environmentDto.getTelemetry().getLogging().getStorageLocation(), result.getLogStorage().getStorageLocationBase());
        assertEquals(environmentDto.getTelemetry().getLogging().getS3().getInstanceProfile(), result.getLogStorage().getInstanceProfile());
        assertEquals(environmentDto.getNetwork().getNetworkCidr(), result.getNetworkCidr());
        assertEquals(environmentDto.getLocation().getName(), result.getRegion());
        assertEquals(environmentDto.getParameters().getAwsParametersDto().getS3GuardTableName(), result.getS3GuardTableName());
        assertEquals(environmentDto.getSecurityAccess().getCidr(), result.getSecurityAccess().getCidr());
        assertEquals(environmentDto.getSecurityAccess().getDefaultSecurityGroupId(), result.getSecurityAccess().getDefaultSecurityGroupId());
        assertEquals(environmentDto.getSecurityAccess().getSecurityGroupIdForKnox(), result.getSecurityAccess().getSecurityGroupIdForKnox());
        assertThat(result.getSubnetIds()).hasSameElementsAs(environmentDto.getNetwork().getSubnetIds());
        assertEquals(environmentDto.getNetwork().getAws().getVpcId(), result.getVpcId());

    }

    private EnvironmentDto getEnvironmentDto() {
        EnvironmentDto environmentDto = new EnvironmentDto();
        AuthenticationDto authentication = AuthenticationDto.builder()
                .withPublicKey("key")
                .withPublicKeyId("keyid")
                .build();
        environmentDto.setAuthentication(authentication);
        Credential credential = new Credential();
        credential.setName("credentialName");
        environmentDto.setCredential(credential);
        environmentDto.setDescription("desc");
        environmentDto.setName("envName");
        EnvironmentTelemetry telemetry = new EnvironmentTelemetry();
        EnvironmentLogging logging = new EnvironmentLogging();
        logging.setStorageLocation("storageLocation");
        S3CloudStorageParameters s3 = new S3CloudStorageParameters();
        s3.setInstanceProfile("instanceProfile");
        logging.setS3(s3);
        telemetry.setLogging(logging);
        environmentDto.setTelemetry(telemetry);
        AwsParams awsNetwork = new AwsParams();
        awsNetwork.setVpcId("vpcId");
        Map<String, CloudSubnet> subnets = Map.of("subnet1", new CloudSubnet(), "subnet2", new CloudSubnet());
        NetworkDto network = NetworkDto.builder()
                .withNetworkCidr("networkCidr")
                .withAws(awsNetwork)
                .withSubnetMetas(subnets)
                .build();
        environmentDto.setNetwork(network);
        LocationDto location = LocationDto.builder()
                .withName("region")
                .build();
        environmentDto.setLocation(location);
        AwsParametersDto awsParams = AwsParametersDto.builder()
                .withDynamoDbTableName("dynamoTable")
                .build();
        ParametersDto params = ParametersDto.builder()
                .withAwsParameters(awsParams)
                .build();
        environmentDto.setParameters(params);
        SecurityAccessDto securityAccess = SecurityAccessDto.builder()
                .withCidr("securityCidr")
                .withDefaultSecurityGroupId("defaultSG")
                .withSecurityGroupIdForKnox("knoxSG")
                .build();
        environmentDto.setSecurityAccess(securityAccess);
        return environmentDto;
    }
}
