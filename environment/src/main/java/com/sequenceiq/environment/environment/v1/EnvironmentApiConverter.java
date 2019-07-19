package com.sequenceiq.environment.environment.v1;

import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNull;
import static com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentAuthenticationRequest.DEFAULT_USER_NAME;
import static com.sequenceiq.environment.environment.dto.EnvironmentChangeCredentialDto.EnvironmentChangeCredentialDtoBuilder.anEnvironmentChangeCredentialDto;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.util.NullUtil;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkAwsParams;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkAzureParams;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkYarnParams;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentAuthenticationRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentChangeCredentialRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentEditRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentNetworkRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.LocationRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.SecurityAccessRequest;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentAuthenticationResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.LocationResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.SecurityAccessResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.SimpleEnvironmentResponse;
import com.sequenceiq.environment.credential.v1.converter.CredentialToCredentialV1ResponseConverter;
import com.sequenceiq.environment.credential.v1.converter.TelemetryApiConverter;
import com.sequenceiq.environment.credential.v1.converter.TunnelConverter;
import com.sequenceiq.environment.environment.dto.AuthenticationDto;
import com.sequenceiq.environment.environment.dto.EnvironmentChangeCredentialDto;
import com.sequenceiq.environment.environment.dto.EnvironmentCreationDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.EnvironmentEditDto;
import com.sequenceiq.environment.environment.dto.LocationDto;
import com.sequenceiq.environment.environment.dto.SecurityAccessDto;
import com.sequenceiq.environment.environment.v1.converter.RegionConverter;
import com.sequenceiq.environment.network.dto.AwsParams;
import com.sequenceiq.environment.network.dto.AzureParams;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.environment.network.dto.YarnParams;

@Component
public class EnvironmentApiConverter {

    private static final String DUMMY_SSH_KEY = "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQC0Rfl2G2vDs6yc19RxCqReunFgpYj+ucyLobpTCBtfDwzIbJot2Fmife6M42mBtiTmAK6x8kc"
            + "UEeab6CB4MUzsqF7vGTFUjwWirG/XU5pYXFUBhi8xzey+KS9KVrQ+UuKJh/AN9iSQeMV+rgT1yF5+etVH+bK1/37QCKp3+mCqjFzPyQOrvkGZv4sYyRwX7BKBLleQmIVWpofpj"
            + "T7BfcCxH877RzC5YMIi65aBc82Dl6tH6OEiP7mzByU52yvH6JFuwZ/9fWj1vXCWJzxx2w0F1OU8Zwg8gNNzL+SVb9+xfBE7xBHMpYFg72hBWPh862Ce36F4NZd3MpWMSjMmpDPh"
            + "centos";

    private final ThreadBasedUserCrnProvider threadBasedUserCrnProvider;

    private final RegionConverter regionConverter;

    private final CredentialToCredentialV1ResponseConverter credentialConverter;

    private final TelemetryApiConverter telemetryApiConverter;

    private final TunnelConverter tunnelConverter;

    public EnvironmentApiConverter(ThreadBasedUserCrnProvider threadBasedUserCrnProvider,
            RegionConverter regionConverter,
            CredentialToCredentialV1ResponseConverter credentialConverter,
            TelemetryApiConverter telemetryApiConverter,
            TunnelConverter tunnelConverter) {
        this.threadBasedUserCrnProvider = threadBasedUserCrnProvider;
        this.regionConverter = regionConverter;
        this.credentialConverter = credentialConverter;
        this.telemetryApiConverter = telemetryApiConverter;
        this.tunnelConverter = tunnelConverter;
    }

    public EnvironmentCreationDto initCreationDto(EnvironmentRequest request) {
        EnvironmentCreationDto.Builder builder = EnvironmentCreationDto.Builder.anEnvironmentCreationDto()
                .withAccountId(threadBasedUserCrnProvider.getAccountId())
                .withName(request.getName())
                .withDescription(request.getDescription())
                .withCloudPlatform(request.getCloudPlatform())
                .withCredential(request)
                .withCreated(System.currentTimeMillis())
                .withTunnel(tunnelConverter.convert(request.getTunnel()))
                .withCreateFreeIpa(request.getFreeIpa() == null ? true : request.getFreeIpa().getCreate())
                .withLocation(locationRequestToDto(request.getLocation()))
                .withTelemetry(telemetryApiConverter.convert(request.getTelemetry()))
                .withRegions(request.getRegions())
                .withAuthentication(authenticationRequestToDto(request.getAuthentication()))
                .withIdBrokerMappingSource(request.getIdBrokerMappingSource());

        NullUtil.doIfNotNull(request.getNetwork(), network -> builder.withNetwork(networkRequestToDto(network)));
        NullUtil.doIfNotNull(request.getSecurityAccess(), securityAccess -> builder.withSecurityAccess(securityAccessRequestToDto(securityAccess)));

        // TODO temporary until CCM not really integrated
        if (request.getSecurityAccess() == null) {
            SecurityAccessDto securityAccess = SecurityAccessDto.builder()
                    .withCidr("0.0.0.0/0")
                    .build();
            builder.withSecurityAccess(securityAccess);
        }
        return builder.build();
    }

    public LocationDto locationRequestToDto(LocationRequest location) {
        return LocationDto.LocationDtoBuilder.aLocationDto()
                .withName(location.getName())
                .withLatitude(location.getLatitude())
                .withLongitude(location.getLongitude())
                .withDisplayName(location.getName())
                .build();
    }

    public NetworkDto networkRequestToDto(EnvironmentNetworkRequest network) {
        NetworkDto.Builder builder = NetworkDto.Builder.aNetworkDto();
        if (network.getAws() != null) {
            AwsParams awsParams = new AwsParams();
            awsParams.setVpcId(network.getAws().getVpcId());
            builder.withAws(awsParams);
        }
        if (network.getAzure() != null) {
            AzureParams azureParams = new AzureParams();
            azureParams.setNetworkId(network.getAzure().getNetworkId());
            azureParams.setNoFirewallRules(network.getAzure().getNoFirewallRules());
            azureParams.setNoPublicIp(network.getAzure().getNoPublicIp());
            azureParams.setResourceGroupName(network.getAzure().getResourceGroupName());
            builder.withAzure(azureParams);
        }
        if (network.getYarn() != null) {
            YarnParams yarnParams = new YarnParams();
            yarnParams.setQueue(network.getYarn().getQueue());
            builder.withYarn(yarnParams);
        }
        return builder
                .withSubnetIds(network.getSubnetIds())
                .withNetworkCidr(network.getNetworkCidr())
                .build();
    }

    private AuthenticationDto authenticationRequestToDto(EnvironmentAuthenticationRequest authentication) {
        AuthenticationDto.Builder builder = AuthenticationDto.builder();
        if (authentication != null) {
            builder.withLoginUserName(authentication.getLoginUserName())
                    .withPublicKey(authentication.getPublicKey())
                    .withPublicKeyId(authentication.getPublicKeyId());
        } else {
            builder.withLoginUserName(DEFAULT_USER_NAME)
                    .withPublicKey(DUMMY_SSH_KEY)
                    .withPublicKeyId(null);
        }
        return builder.build();
    }

    private SecurityAccessDto securityAccessRequestToDto(SecurityAccessRequest securityAccess) {
        return SecurityAccessDto.builder()
                .withCidr(securityAccess.getCidr())
                .withSecurityGroupIdForKnox(securityAccess.getSecurityGroupIdForKnox())
                .withDefaultSecurityGroupId(securityAccess.getDefaultSecurityGroupId())
                .build();
    }

    public DetailedEnvironmentResponse dtoToDetailedResponse(EnvironmentDto environmentDto) {
        DetailedEnvironmentResponse.Builder builder = DetailedEnvironmentResponse.Builder.builder()
                .withCrn(environmentDto.getResourceCrn())
                .withName(environmentDto.getName())
                .withDescription(environmentDto.getDescription())
                .withCloudPlatform(environmentDto.getCloudPlatform())
                .withCredential(credentialConverter.convert(environmentDto.getCredential()))
                .withEnvironmentStatus(environmentDto.getStatus().getResponseStatus())
                .withLocation(locationDtoToResponse(environmentDto.getLocation()))
                .withCreateFreeIpa(environmentDto.isCreateFreeIpa())
                .withRegions(regionConverter.convertRegions(environmentDto.getRegionSet()))
                .withAuthentication(authenticationDtoToResponse(environmentDto.getAuthentication()))
                .withStatusReason(environmentDto.getStatusReason())
                .withCreated(environmentDto.getCreated())
                .withTelemetry(telemetryApiConverter.convertFromJson(environmentDto.getTelemetry()))
                .withTunnel(environmentDto.getTunnel())
                .withRegions(regionConverter.convertRegions(environmentDto.getRegionSet()))
                .withIdBrokerMappingSource(environmentDto.getIdBrokerMappingSource());

        NullUtil.doIfNotNull(environmentDto.getNetwork(), network -> builder.withNetwork(networkDtoToResponse(network)));
        NullUtil.doIfNotNull(environmentDto.getSecurityAccess(), securityAccess -> builder.withSecurityAccess(securityAccessDtoToResponse(securityAccess)));
        return builder.build();
    }

    public SimpleEnvironmentResponse dtoToSimpleResponse(EnvironmentDto environmentDto) {
        SimpleEnvironmentResponse.Builder builder = SimpleEnvironmentResponse.builder()
                .withCrn(environmentDto.getResourceCrn())
                .withName(environmentDto.getName())
                .withDescription(environmentDto.getDescription())
                .withCloudPlatform(environmentDto.getCloudPlatform())
                .withCredential(credentialConverter.convert(environmentDto.getCredential()))
                .withEnvironmentStatus(environmentDto.getStatus().getResponseStatus())
                .withLocation(locationDtoToResponse(environmentDto.getLocation()))
                .withCreateFreeIpa(environmentDto.isCreateFreeIpa())
                .withStatusReason(environmentDto.getStatusReason())
                .withCreated(environmentDto.getCreated())
                .withTunnel(environmentDto.getTunnel())
                .withTelemetry(telemetryApiConverter.convertFromJson(environmentDto.getTelemetry()))
                .withRegions(regionConverter.convertRegions(environmentDto.getRegionSet()));

        NullUtil.doIfNotNull(environmentDto.getNetwork(), network -> builder.withNetwork(networkDtoToResponse(network)));
        return builder.build();
    }

    public EnvironmentNetworkResponse networkDtoToResponse(NetworkDto network) {
        return EnvironmentNetworkResponse.EnvironmentNetworkResponseBuilder.anEnvironmentNetworkResponse()
                .withCrn(network.getResourceCrn())
                .withSubnetIds(network.getSubnetIds())
                .withSubnetMetas(network.getSubnetMetas())
                .withAws(EnvironmentNetworkAwsParams.EnvironmentNetworkAwsParamsBuilder.anEnvironmentNetworkAwsParams()
                        .withVpcId(getIfNotNull(network.getAws(), AwsParams::getVpcId))
                        .build())
                .withAzure(EnvironmentNetworkAzureParams.EnvironmentNetworkAzureParamsBuilder.anEnvironmentNetworkAzureParams()
                        .withNetworkId(getIfNotNull(network.getAzure(), AzureParams::getNetworkId))
                        .withResourceGroupName(getIfNotNull(network.getAzure(), AzureParams::getResourceGroupName))
                        .withNoFirewallRules(getIfNotNull(network.getAzure(), AzureParams::isNoFirewallRules))
                        .withNoPublicIp(getIfNotNull(network.getAzure(), AzureParams::isNoPublicIp))
                        .build())
                .withYarn(EnvironmentNetworkYarnParams.EnvironmentNetworkYarnParamsBuilder.anEnvironmentNetworkYarnParams()
                        .withQueue(getIfNotNull(network.getYarn(), YarnParams::getQueue))
                        .build())
                .build();
    }

    public LocationResponse locationDtoToResponse(LocationDto locationDto) {
        return LocationResponse.LocationResponseBuilder.aLocationResponse()
                .withName(locationDto.getName())
                .withDisplayName(locationDto.getDisplayName())
                .withLatitude(locationDto.getLatitude())
                .withLongitude(locationDto.getLongitude())
                .build();
    }

    public EnvironmentEditDto initEditDto(EnvironmentEditRequest request) {
        EnvironmentEditDto.EnvironmentEditDtoBuilder builder = EnvironmentEditDto.EnvironmentEditDtoBuilder.anEnvironmentEditDto()
                .withDescription(request.getDescription())
                .withAccountId(threadBasedUserCrnProvider.getAccountId())
                .withRegions(request.getRegions())
                .withIdBrokerMappingSource(request.getIdBrokerMappingSource());
        NullUtil.doIfNotNull(request.getNetwork(), network -> builder.withNetwork(networkRequestToDto(network)));
        NullUtil.doIfNotNull(request.getLocation(), location -> builder.withLocation(locationRequestToDto(location)));
        NullUtil.doIfNotNull(request.getAuthentication(), authentication -> builder.withAuthentication(authenticationRequestToDto(authentication)));
        NullUtil.doIfNotNull(request.getTelemetry(), telemetryRequest -> builder.withTelemetry(telemetryApiConverter.convert(request.getTelemetry())));
        NullUtil.doIfNotNull(request.getSecurityAccess(), securityAccess -> builder.withSecurityAccess(securityAccessRequestToDto(securityAccess)));
        return builder.build();
    }

    public EnvironmentChangeCredentialDto convertEnvironmentChangeCredentialDto(EnvironmentChangeCredentialRequest request) {
        return anEnvironmentChangeCredentialDto()
                .withCredentialName(request.getCredential() != null ? request.getCredential().getName() : request.getCredentialName())
                .build();
    }

    public EnvironmentAuthenticationResponse authenticationDtoToResponse(AuthenticationDto authenticationDto) {
        return EnvironmentAuthenticationResponse.builder()
                .withLoginUserName(authenticationDto.getLoginUserName())
                .withPublicKey(authenticationDto.getPublicKey())
                .withPublicKeyId(authenticationDto.getPublicKeyId())
                .build();
    }

    private SecurityAccessResponse securityAccessDtoToResponse(SecurityAccessDto securityAccess) {
        return SecurityAccessResponse.builder()
                .withCidr(securityAccess.getCidr())
                .withSecurityGroupIdForKnox(securityAccess.getSecurityGroupIdForKnox())
                .withDefaultSecurityGroupId(securityAccess.getDefaultSecurityGroupId())
                .build();
    }
}
