package com.sequenceiq.environment.environment.v1;

import static com.sequenceiq.cloudbreak.util.NullUtil.doIfNotNull;
import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNull;

import java.util.List;

import org.springframework.stereotype.Component;

import com.cloudera.cdp.environments.model.AuthenticationRequest;
import com.cloudera.cdp.environments.model.AwsLogStorageRequest;
import com.cloudera.cdp.environments.model.CreateAWSEnvironmentRequest;
import com.cloudera.cdp.environments.model.SecurityAccessRequest;
import com.sequenceiq.environment.environment.dto.AuthenticationDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.SecurityAccessDto;
import com.sequenceiq.environment.environment.dto.telemetry.EnvironmentTelemetry;
import com.sequenceiq.environment.network.dto.AwsParams;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.environment.parameters.dto.AwsParametersDto;

@Component
public class EnvironmentDtoToCreateAWSEnvironmentRequestConverter {

    public CreateAWSEnvironmentRequest convert(EnvironmentDto source) {
        CreateAWSEnvironmentRequest environmentRequest = new CreateAWSEnvironmentRequest();
        environmentRequest.setAuthentication(environmentAuthenticationRequestToAuthenticationRequest(source.getAuthentication()));
        environmentRequest.setCredentialName(source.getCredential().getName());
        environmentRequest.setDescription(source.getDescription());
        environmentRequest.setEnvironmentName(source.getName());
        environmentRequest.setLogStorage(environmentTelemetryToLogStorageRequest(source.getTelemetry()));
        environmentRequest.setNetworkCidr(getIfNotNull(source.getNetwork(), NetworkDto::getNetworkCidr));
        environmentRequest.setRegion(source.getLocation().getName());
        environmentRequest.setS3GuardTableName(getS3GuardTableName(source));
        environmentRequest.setSecurityAccess(environmentSecurityAccessToSecurityAccessRequest(source.getSecurityAccess()));
        environmentRequest.setSubnetIds(getSubnetIds(source));
        environmentRequest.setVpcId(getVpcId(source));
        return environmentRequest;
    }

    private AuthenticationRequest environmentAuthenticationRequestToAuthenticationRequest(AuthenticationDto source) {
        AuthenticationRequest authenticationRequest = new AuthenticationRequest();
        doIfNotNull(source, s -> {
            authenticationRequest.setPublicKey(s.getPublicKey());
            authenticationRequest.setPublicKeyId(s.getPublicKeyId());
        });
        return authenticationRequest;
    }

    private AwsLogStorageRequest environmentTelemetryToLogStorageRequest(EnvironmentTelemetry source) {
        AwsLogStorageRequest awsLogStorageRequest = new AwsLogStorageRequest();
        doIfNotNull(source, s -> {
            doIfNotNull(s.getLogging(), logging -> {
                doIfNotNull(logging.getS3(), s3 -> awsLogStorageRequest.setInstanceProfile(s3.getInstanceProfile()));
                awsLogStorageRequest.setStorageLocationBase(logging.getStorageLocation());
            });
        });
        return awsLogStorageRequest;
    }

    private String getS3GuardTableName(EnvironmentDto source) {
        return getIfNotNull(source.getParameters(), p ->
                getIfNotNull(p.getAwsParametersDto(), AwsParametersDto::getS3GuardTableName));
    }

    private SecurityAccessRequest environmentSecurityAccessToSecurityAccessRequest(
            SecurityAccessDto source) {

        SecurityAccessRequest securityAccessRequest = new SecurityAccessRequest();
        doIfNotNull(source, s -> {
            securityAccessRequest.setCidr(s.getCidr());
            securityAccessRequest.setDefaultSecurityGroupId(s.getDefaultSecurityGroupId());
            securityAccessRequest.setSecurityGroupIdForKnox(s.getSecurityGroupIdForKnox());
        });
        return securityAccessRequest;
    }

    private List<String> getSubnetIds(EnvironmentDto source) {
        return getIfNotNull(source.getNetwork(), net ->
                getIfNotNull(net.getSubnetIds(), List::copyOf));
    }

    private String getVpcId(EnvironmentDto source) {
        return getIfNotNull(source.getNetwork(), net ->
                getIfNotNull(net.getAws(), AwsParams::getVpcId));
    }
}
