package com.sequenceiq.environment.environment.v1;

import static com.sequenceiq.cloudbreak.util.NullUtil.doIfNotNull;
import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNull;

import java.util.List;

import org.springframework.stereotype.Component;

import com.cloudera.cdp.environments.model.AuthenticationRequest;
import com.cloudera.cdp.environments.model.AwsLogStorageRequest;
import com.cloudera.cdp.environments.model.CreateAWSEnvironmentRequest;
import com.cloudera.cdp.environments.model.SecurityAccessRequest;
import com.sequenceiq.common.api.telemetry.request.TelemetryRequest;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkAwsParams;
import com.sequenceiq.environment.api.v1.environment.model.base.EnvironmentNetworkBase;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentAuthenticationRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.aws.S3GuardRequestParameters;

@Component
public class EnvironmentRequestToCreateAWSEnvironmentRequestConverter {

    public CreateAWSEnvironmentRequest convert(EnvironmentRequest source) {
        CreateAWSEnvironmentRequest environmentRequest = new CreateAWSEnvironmentRequest();
        environmentRequest.setAuthentication(environmentAuthenticationRequestToAuthenticationRequest(source.getAuthentication()));
        environmentRequest.setCredentialName(source.getCredentialName());
        environmentRequest.setDescription(source.getDescription());
        environmentRequest.setEnvironmentName(source.getName());
        environmentRequest.setLogStorage(environmentTelemetryToLogStorageRequest(source.getTelemetry()));
        environmentRequest.setNetworkCidr(getIfNotNull(source.getNetwork(), EnvironmentNetworkBase::getNetworkCidr));
        environmentRequest.setRegion(source.getLocation().getName());
        environmentRequest.setS3GuardTableName(getS3GuardTableName(source));
        environmentRequest.setSecurityAccess(environmentSecurityAccessToSecurityAccessRequest(source.getSecurityAccess()));
        environmentRequest.setSubnetIds(getSubnetIds(source));
        environmentRequest.setVpcId(getVpcId(source));
        return environmentRequest;
    }

    private AuthenticationRequest environmentAuthenticationRequestToAuthenticationRequest(EnvironmentAuthenticationRequest source) {
        AuthenticationRequest authenticationRequest = new AuthenticationRequest();
        doIfNotNull(source, s -> {
            authenticationRequest.setPublicKey(s.getPublicKey());
            authenticationRequest.setPublicKeyId(s.getPublicKeyId());
        });
        return authenticationRequest;
    }

    private AwsLogStorageRequest environmentTelemetryToLogStorageRequest(TelemetryRequest source) {
        AwsLogStorageRequest awsLogStorageRequest = new AwsLogStorageRequest();
        doIfNotNull(source, s -> {
            doIfNotNull(s.getLogging(), logging -> {
                doIfNotNull(logging.getS3(), s3 -> awsLogStorageRequest.setInstanceProfile(s3.getInstanceProfile()));
                awsLogStorageRequest.setStorageLocationBase(logging.getStorageLocation());
            });
        });
        return awsLogStorageRequest;
    }

    private String getS3GuardTableName(EnvironmentRequest source) {
        return getIfNotNull(source.getAws(), aws ->
                getIfNotNull(aws.getS3guard(), S3GuardRequestParameters::getDynamoDbTableName));
    }

    private SecurityAccessRequest environmentSecurityAccessToSecurityAccessRequest(
            com.sequenceiq.environment.api.v1.environment.model.request.SecurityAccessRequest source) {

        SecurityAccessRequest securityAccessRequest = new SecurityAccessRequest();
        doIfNotNull(source, s -> {
            securityAccessRequest.setCidr(s.getCidr());
            securityAccessRequest.setDefaultSecurityGroupId(s.getDefaultSecurityGroupId());
            securityAccessRequest.setSecurityGroupIdForKnox(s.getSecurityGroupIdForKnox());
        });
        return securityAccessRequest;
    }

    private List<String> getSubnetIds(EnvironmentRequest source) {
        return getIfNotNull(source.getNetwork(), net ->
                getIfNotNull(net.getSubnetIds(), List::copyOf));
    }

    private String getVpcId(EnvironmentRequest source) {
        return getIfNotNull(source.getNetwork(), net ->
                getIfNotNull(net.getAws(), EnvironmentNetworkAwsParams::getVpcId));
    }
}
