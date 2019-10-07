package com.sequenceiq.environment.environment.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.cloudera.cdp.environments.model.CreateAWSEnvironmentRequest;
import com.sequenceiq.common.api.cloudstorage.old.S3CloudStorageV1Parameters;
import com.sequenceiq.common.api.telemetry.request.LoggingRequest;
import com.sequenceiq.common.api.telemetry.request.TelemetryRequest;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkAwsParams;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentAuthenticationRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentNetworkRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.LocationRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.SecurityAccessRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.aws.AwsEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.aws.S3GuardRequestParameters;

class EnvironmentRequestToCreateAWSEnvironmentRequestConverterTest {

    private EnvironmentRequestToCreateAWSEnvironmentRequestConverter underTest;

    @BeforeEach
    void setUp() {
        underTest = new EnvironmentRequestToCreateAWSEnvironmentRequestConverter();
    }

    @Test
    void convert() {
        EnvironmentRequest request = getEnvironmentRequest();
        CreateAWSEnvironmentRequest result = underTest.convert(request);
        assertEquals(request.getAuthentication().getPublicKey(), result.getAuthentication().getPublicKey());
        assertEquals(request.getAuthentication().getPublicKeyId(), result.getAuthentication().getPublicKeyId());
        assertEquals(request.getCredentialName(), result.getCredentialName());
        assertEquals(request.getDescription(), result.getDescription());
        assertEquals(request.getName(), result.getEnvironmentName());
        assertEquals(request.getTelemetry().getLogging().getStorageLocation(), result.getLogStorage().getStorageLocationBase());
        assertEquals(request.getTelemetry().getLogging().getS3().getInstanceProfile(), result.getLogStorage().getInstanceProfile());
        assertEquals(request.getNetwork().getNetworkCidr(), result.getNetworkCidr());
        assertEquals(request.getLocation().getName(), result.getRegion());
        assertEquals(request.getAws().getS3guard().getDynamoDbTableName(), result.getS3GuardTableName());
        assertEquals(request.getSecurityAccess().getCidr(), result.getSecurityAccess().getCidr());
        assertEquals(request.getSecurityAccess().getDefaultSecurityGroupId(), result.getSecurityAccess().getDefaultSecurityGroupId());
        assertEquals(request.getSecurityAccess().getSecurityGroupIdForKnox(), result.getSecurityAccess().getSecurityGroupIdForKnox());
        assertThat(result.getSubnetIds()).hasSameElementsAs(request.getNetwork().getSubnetIds());
        assertEquals(request.getNetwork().getAws().getVpcId(), result.getVpcId());
    }

    private EnvironmentRequest getEnvironmentRequest() {
        EnvironmentRequest request = new EnvironmentRequest();
        EnvironmentAuthenticationRequest authentication = new EnvironmentAuthenticationRequest();
        authentication.setPublicKey("key");
        authentication.setPublicKey("keyid");
        request.setAuthentication(authentication);
        request.setCredentialName("credentialname");
        request.setDescription("desc");
        request.setName("envName");
        TelemetryRequest telemetry = new TelemetryRequest();
        LoggingRequest logging = new LoggingRequest();
        logging.setStorageLocation("storageLocation");
        S3CloudStorageV1Parameters s3 = new S3CloudStorageV1Parameters();
        s3.setInstanceProfile("instanceProfile");
        logging.setS3(s3);
        telemetry.setLogging(logging);
        request.setTelemetry(telemetry);
        EnvironmentNetworkRequest network = new EnvironmentNetworkRequest();
        network.setNetworkCidr("networkCidr");
        EnvironmentNetworkAwsParams awsNetwork = new EnvironmentNetworkAwsParams();
        awsNetwork.setVpcId("vpcId");
        network.setAws(awsNetwork);
        request.setNetwork(network);
        LocationRequest location = new LocationRequest();
        location.setName("region");
        request.setLocation(location);
        AwsEnvironmentParameters awsParams = new AwsEnvironmentParameters();
        S3GuardRequestParameters s3Guard = new S3GuardRequestParameters();
        s3Guard.setDynamoDbTableName("dynamoTable");
        awsParams.setS3guard(s3Guard);
        request.setAws(awsParams);
        SecurityAccessRequest securityAccess = new SecurityAccessRequest();
        securityAccess.setCidr("securityCidr");
        securityAccess.setDefaultSecurityGroupId("defaultSG");
        securityAccess.setSecurityGroupIdForKnox("knoxSG");
        request.setSecurityAccess(securityAccess);
        network.setSubnetIds(Set.of("subnet1", "subnet2"));
        return request;
    }
}
