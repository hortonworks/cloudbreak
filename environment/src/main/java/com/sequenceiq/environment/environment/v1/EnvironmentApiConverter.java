package com.sequenceiq.environment.environment.v1;

import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNull;
import static com.sequenceiq.environment.environment.dto.EnvironmentChangeCredentialDto.EnvironmentChangeCredentialDtoBuilder.anEnvironmentChangeCredentialDto;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.ws.rs.BadRequestException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.util.NullUtil;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkAwsParams;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkAzureParams;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkMockParams;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkYarnParams;
import com.sequenceiq.environment.api.v1.environment.model.base.PrivateSubnetCreation;
import com.sequenceiq.environment.api.v1.environment.model.request.AttachedFreeIpaRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentAuthenticationRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentChangeCredentialRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentEditRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentNetworkRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.LocationRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.SecurityAccessRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.aws.AwsEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.aws.S3GuardRequestParameters;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentAuthenticationResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentCrnResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.LocationResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.SecurityAccessResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.SimpleEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.TagResponse;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.credential.service.CredentialService;
import com.sequenceiq.environment.credential.v1.converter.CredentialToCredentialV1ResponseConverter;
import com.sequenceiq.environment.credential.v1.converter.CredentialViewConverter;
import com.sequenceiq.environment.credential.v1.converter.TunnelConverter;
import com.sequenceiq.environment.environment.domain.EnvironmentTags;
import com.sequenceiq.environment.environment.domain.ExperimentalFeatures;
import com.sequenceiq.environment.environment.dto.AuthenticationDto;
import com.sequenceiq.environment.environment.dto.EnvironmentChangeCredentialDto;
import com.sequenceiq.environment.environment.dto.EnvironmentCreationDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.EnvironmentEditDto;
import com.sequenceiq.environment.environment.dto.FreeIpaCreationDto;
import com.sequenceiq.environment.environment.dto.LocationDto;
import com.sequenceiq.environment.environment.dto.SecurityAccessDto;
import com.sequenceiq.environment.network.dao.domain.RegistrationType;
import com.sequenceiq.environment.network.dto.AwsParams;
import com.sequenceiq.environment.network.dto.AzureParams;
import com.sequenceiq.environment.network.dto.MockParams;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.environment.network.dto.YarnParams;
import com.sequenceiq.environment.network.service.SubnetIdProvider;
import com.sequenceiq.environment.parameters.dto.AwsParametersDto;
import com.sequenceiq.environment.parameters.dto.ParametersDto;
import com.sequenceiq.environment.telemetry.service.AccountTelemetryService;

@Component
public class EnvironmentApiConverter {

    private static final String DUMMY_SSH_KEY = "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQC0Rfl2G2vDs6yc19RxCqReunFgpYj+ucyLobpTCBtfDwzIbJot2Fmife6M42mBtiTmAK6x8kc"
            + "UEeab6CB4MUzsqF7vGTFUjwWirG/XU5pYXFUBhi8xzey+KS9KVrQ+UuKJh/AN9iSQeMV+rgT1yF5+etVH+bK1/37QCKp3+mCqjFzPyQOrvkGZv4sYyRwX7BKBLleQmIVWpofpj"
            + "T7BfcCxH877RzC5YMIi65aBc82Dl6tH6OEiP7mzByU52yvH6JFuwZ/9fWj1vXCWJzxx2w0F1OU8Zwg8gNNzL+SVb9+xfBE7xBHMpYFg72hBWPh862Ce36F4NZd3MpWMSjMmpDPh"
            + "centos";

    private static final String NETWORK_CONVERT_MESSAGE_TEMPLATE = "Setting up {} network param(s) for environment related dto..";

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentApiConverter.class);

    private final RegionConverter regionConverter;

    private final CredentialToCredentialV1ResponseConverter credentialConverter;

    private final CredentialViewConverter credentialViewConverter;

    private final CredentialService credentialService;

    private final TelemetryApiConverter telemetryApiConverter;

    private final AccountTelemetryService accountTelemetryService;

    private final TunnelConverter tunnelConverter;

    private final SubnetIdProvider subnetIdProvider;

    private final FreeIpaConverter freeIpaConverter;

    public EnvironmentApiConverter(RegionConverter regionConverter,
            CredentialToCredentialV1ResponseConverter credentialConverter,
            CredentialViewConverter credentialViewConverter,
            TelemetryApiConverter telemetryApiConverter,
            TunnelConverter tunnelConverter,
            AccountTelemetryService accountTelemetryService,
            CredentialService credentialService,
            SubnetIdProvider subnetIdProvider,
            FreeIpaConverter freeIpaConverter) {
        this.regionConverter = regionConverter;
        this.credentialConverter = credentialConverter;
        this.credentialViewConverter = credentialViewConverter;
        this.telemetryApiConverter = telemetryApiConverter;
        this.accountTelemetryService = accountTelemetryService;
        this.tunnelConverter = tunnelConverter;
        this.credentialService = credentialService;
        this.subnetIdProvider = subnetIdProvider;
        this.freeIpaConverter = freeIpaConverter;
    }

    public EnvironmentCreationDto initCreationDto(EnvironmentRequest request) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        EnvironmentCreationDto.Builder builder = EnvironmentCreationDto.builder()
                .withAccountId(accountId)
                .withCreator(ThreadBasedUserCrnProvider.getUserCrn())
                .withName(request.getName())
                .withDescription(request.getDescription())
                .withCloudPlatform(getCloudPlatform(request, accountId))
                .withCredential(request)
                .withCreated(System.currentTimeMillis())
                .withFreeIpaCreation(attachedFreeIpaRequestToDto(request.getFreeIpa()))
                .withLocation(locationRequestToDto(request.getLocation()))
                .withTelemetry(telemetryApiConverter.convert(request.getTelemetry(),
                        accountTelemetryService.getOrDefault(accountId).getFeatures()))
                .withRegions(request.getRegions())
                .withAuthentication(authenticationRequestToDto(request.getAuthentication()))
                .withAdminGroupName(request.getAdminGroupName())
                .withTags(request.getTags())
                .withCrn(createCrn(ThreadBasedUserCrnProvider.getAccountId()))
                .withExperimentalFeatures(ExperimentalFeatures.builder()
                        .withIdBrokerMappingSource(request.getIdBrokerMappingSource())
                        .withCloudStorageValidation(request.getCloudStorageValidation())
                        .withTunnel(tunnelConverter.convert(request.getTunnel()))
                        .build())
                .withParameters(getIfNotNull(request.getAws(), this::awsParamsToParametersDto))
                .withParentEnvironmentName(request.getParentEnvironmentName());

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

    private String getCloudPlatform(EnvironmentRequest request, String accountId) {
        if (!Strings.isNullOrEmpty(request.getCredentialName())) {
            try {
                Credential credential = credentialService.getByNameForAccountId(request.getCredentialName(), accountId);
                return credential.getCloudPlatform();
            } catch (NotFoundException e) {
                throw new BadRequestException(String.format("No credential found with name [%s] in the workspace.",
                        request.getCredentialName()), e);
            }
        } else {
            throw new BadRequestException("No credential has been specified in request for environment creation.");
        }
    }

    private String createCrn(@Nonnull String accountId) {
        return Crn.builder()
                .setService(Crn.Service.ENVIRONMENTS)
                .setAccountId(accountId)
                .setResourceType(Crn.ResourceType.ENVIRONMENT)
                .setResource(UUID.randomUUID().toString())
                .build()
                .toString();
    }

    private FreeIpaCreationDto attachedFreeIpaRequestToDto(AttachedFreeIpaRequest request) {
        FreeIpaCreationDto.Builder builder = FreeIpaCreationDto.builder();
        if (request != null) {
            builder.withCreate(request.getCreate());
            NullUtil.doIfNotNull(request.getInstanceCountByGroup(), instanceCountByGroup -> builder.withInstanceCountByGroup(instanceCountByGroup));
        }
        return builder.build();
    }

    private ParametersDto awsParamsToParametersDto(AwsEnvironmentParameters aws) {
        return ParametersDto.builder()
                .withAwsParameters(awsParamsToAwsParameters(aws))
                .build();
    }

    private AwsParametersDto awsParamsToAwsParameters(AwsEnvironmentParameters aws) {
        return AwsParametersDto.builder()
                .withDynamoDbTableName(getIfNotNull(aws.getS3guard(), S3GuardRequestParameters::getDynamoDbTableName))
                .build();
    }

    public LocationDto locationRequestToDto(LocationRequest location) {
        return LocationDto.builder()
                .withName(location.getName())
                .withLatitude(location.getLatitude())
                .withLongitude(location.getLongitude())
                .withDisplayName(location.getName())
                .build();
    }

    public NetworkDto networkRequestToDto(EnvironmentNetworkRequest network) {
        LOGGER.debug("Converting network request to dto");
        NetworkDto.Builder builder = NetworkDto.builder();
        if (network.getAws() != null) {
            LOGGER.debug(NETWORK_CONVERT_MESSAGE_TEMPLATE, "AWS");
            AwsParams awsParams = new AwsParams();
            awsParams.setVpcId(network.getAws().getVpcId());
            builder.withAws(awsParams);
            builder.withNetworkId(network.getAws().getVpcId());
        }
        if (network.getAzure() != null) {
            LOGGER.debug(NETWORK_CONVERT_MESSAGE_TEMPLATE, "Azure");
            AzureParams azureParams = new AzureParams();
            azureParams.setNetworkId(network.getAzure().getNetworkId());
            azureParams.setNoPublicIp(Boolean.TRUE.equals(network.getAzure().getNoPublicIp()));
            azureParams.setResourceGroupName(network.getAzure().getResourceGroupName());
            builder.withAzure(azureParams);
            builder.withNetworkId(network.getAzure().getNetworkId());
        }
        if (network.getYarn() != null) {
            LOGGER.debug(NETWORK_CONVERT_MESSAGE_TEMPLATE, "Yarn");
            YarnParams yarnParams = new YarnParams();
            yarnParams.setQueue(network.getYarn().getQueue());
            builder.withYarn(yarnParams);
        }
        if (network.getSubnetIds() != null) {
            builder.withSubnetMetas(network.getSubnetIds().stream()
                    .collect(Collectors.toMap(id -> id, id -> new CloudSubnet(id, id))));
        }
        if (network.getMock() != null) {
            LOGGER.debug(NETWORK_CONVERT_MESSAGE_TEMPLATE, "Mock");
            MockParams mockParams = new MockParams();
            mockParams.setInternetGatewayId(network.getMock().getInternetGatewayId());
            mockParams.setVpcId(network.getMock().getVpcId());
            builder.withMock(mockParams);
            builder.withNetworkId(mockParams.getVpcId());
        }
        builder.withPrivateSubnetCreation(getPrivateSubnetCreation(network));
        return builder
                .withNetworkCidr(network.getNetworkCidr())
                .build();
    }

    private PrivateSubnetCreation getPrivateSubnetCreation(EnvironmentNetworkRequest network) {
        return Optional.ofNullable(network.getPrivateSubnetCreation()).orElse(PrivateSubnetCreation.DISABLED);
    }

    private AuthenticationDto authenticationRequestToDto(EnvironmentAuthenticationRequest authentication) {
        AuthenticationDto.Builder builder = AuthenticationDto.builder();
        if (authentication != null && (StringUtils.hasLength(authentication.getPublicKey()) || StringUtils.hasLength(authentication.getPublicKeyId()))) {
            String publicKey = nullIfBlank(authentication.getPublicKey());
            builder.withLoginUserName(authentication.getLoginUserName())
                    .withPublicKey(publicKey)
                    .withPublicKeyId(nullIfBlank(authentication.getPublicKeyId()))
                    .withManagedKey(Objects.nonNull(publicKey));
        }
        return builder.build();
    }

    private String nullIfBlank(String value) {
        if (StringUtils.hasLength(value)) {
            return value;
        }
        return null;
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
                .withCreateFreeIpa(environmentDto.getFreeIpaCreation().getCreate())
                .withRegions(regionConverter.convertRegions(environmentDto.getRegions()))
                .withCreator(environmentDto.getCreator())
                .withAuthentication(authenticationDtoToResponse(environmentDto.getAuthentication()))
                .withStatusReason(environmentDto.getStatusReason())
                .withCreated(environmentDto.getCreated())
                .withTag(getIfNotNull(environmentDto.getTags(), this::environtmentTagsToTagResponse))
                .withTelemetry(telemetryApiConverter.convert(environmentDto.getTelemetry()))
                .withRegions(regionConverter.convertRegions(environmentDto.getRegions()))
                .withTunnel(environmentDto.getExperimentalFeatures().getTunnel())
                .withIdBrokerMappingSource(environmentDto.getExperimentalFeatures().getIdBrokerMappingSource())
                .withCloudStorageValidation(environmentDto.getExperimentalFeatures().getCloudStorageValidation())
                .withAdminGroupName(environmentDto.getAdminGroupName())
                .withAws(getIfNotNull(environmentDto.getParameters(), this::awsEnvParamsToAwsEnvironmentParams))
                .withParentEnvironmentCrn(environmentDto.getParentEnvironmentCrn())
                .withParentEnvironmentName(environmentDto.getParentEnvironmentName())
                .withParentEnvironmentCloudPlatform(environmentDto.getParentEnvironmentCloudPlatform());

        NullUtil.doIfNotNull(environmentDto.getNetwork(), network ->
                builder.withNetwork(networkDtoToResponse(network, environmentDto.getExperimentalFeatures().getTunnel())));
        NullUtil.doIfNotNull(environmentDto.getSecurityAccess(), securityAccess -> builder.withSecurityAccess(securityAccessDtoToResponse(securityAccess)));
        return builder.build();
    }

    public SimpleEnvironmentResponse dtoToSimpleResponse(EnvironmentDto environmentDto) {
        SimpleEnvironmentResponse.Builder builder = SimpleEnvironmentResponse.builder()
                .withCrn(environmentDto.getResourceCrn())
                .withName(environmentDto.getName())
                .withDescription(environmentDto.getDescription())
                .withCloudPlatform(environmentDto.getCloudPlatform())
                .withCredentialView(credentialViewConverter.convert(environmentDto.getCredentialView()))
                .withEnvironmentStatus(environmentDto.getStatus().getResponseStatus())
                .withCreator(environmentDto.getCreator())
                .withLocation(locationDtoToResponse(environmentDto.getLocation()))
                .withCreateFreeIpa(environmentDto.getFreeIpaCreation().getCreate())
                .withFreeIpa(freeIpaConverter.convert(environmentDto.getFreeIpaCreation()))
                .withStatusReason(environmentDto.getStatusReason())
                .withCreated(environmentDto.getCreated())
                .withTunnel(environmentDto.getExperimentalFeatures().getTunnel())
                .withAdminGroupName(environmentDto.getAdminGroupName())
                .withTag(getIfNotNull(environmentDto.getTags(), this::environtmentTagsToTagResponse))
                .withTelemetry(telemetryApiConverter.convert(environmentDto.getTelemetry()))
                .withRegions(regionConverter.convertRegions(environmentDto.getRegions()))
                .withAws(getIfNotNull(environmentDto.getParameters(), this::awsEnvParamsToAwsEnvironmentParams))
                .withParentEnvironmentName(environmentDto.getParentEnvironmentName());

        NullUtil.doIfNotNull(environmentDto.getNetwork(), network ->
                builder.withNetwork(networkDtoToResponse(network, environmentDto.getExperimentalFeatures().getTunnel())));
        return builder.build();
    }

    private AwsEnvironmentParameters awsEnvParamsToAwsEnvironmentParams(ParametersDto parameters) {
        return AwsEnvironmentParameters.builder()
                .withS3guard(getIfNotNull(parameters.getAwsParametersDto(), this::awsParametersToS3guardParam))
                .build();
    }

    private S3GuardRequestParameters awsParametersToS3guardParam(AwsParametersDto awsParametersDto) {
        return S3GuardRequestParameters.builder()
                .withDynamoDbTableName(awsParametersDto.getS3GuardTableName())
                .build();
    }

    private TagResponse environtmentTagsToTagResponse(EnvironmentTags tags) {
        TagResponse tagResponse = new TagResponse();
        tagResponse.setDefaults(tags.getDefaultTags());
        tagResponse.setUserDefined(tags.getUserDefinedTags());
        return tagResponse;
    }

    public EnvironmentNetworkResponse networkDtoToResponse(NetworkDto network, Tunnel tunnel) {
        return EnvironmentNetworkResponse.EnvironmentNetworkResponseBuilder.anEnvironmentNetworkResponse()
                .withCrn(network.getResourceCrn())
                .withSubnetIds(network.getSubnetIds())
                .withNetworkCidr(network.getNetworkCidr())
                .withSubnetMetas(network.getSubnetMetas())
                .withCbSubnets(network.getCbSubnets())
                .withDwxSubnets(network.getDwxSubnets())
                .withMlxSubnets(network.getMlxSubnets())
                .withPreferedSubnetId(subnetIdProvider.provide(network, tunnel, network.getCloudPlatform()))
                .withPrivateSubnetCreation(network.getPrivateSubnetCreation())
                .withExistingNetwork(RegistrationType.EXISTING == network.getRegistrationType())
                .withAws(getIfNotNull(network.getAws(), p -> EnvironmentNetworkAwsParams.EnvironmentNetworkAwsParamsBuilder
                        .anEnvironmentNetworkAwsParams()
                        .withVpcId(p.getVpcId())
                        .build()))
                .withAzure(getIfNotNull(network.getAzure(), p -> EnvironmentNetworkAzureParams.EnvironmentNetworkAzureParamsBuilder
                        .anEnvironmentNetworkAzureParams()
                        .withNetworkId(p.getNetworkId())
                        .withResourceGroupName(p.getResourceGroupName())
                        .withNoPublicIp(p.isNoPublicIp())
                        .build()))
                .withYarn(getIfNotNull(network.getYarn(), p -> EnvironmentNetworkYarnParams.EnvironmentNetworkYarnParamsBuilder
                        .anEnvironmentNetworkYarnParams()
                        .withQueue(p.getQueue())
                        .build()))
                .withMock(getIfNotNull(network.getMock(), p -> EnvironmentNetworkMockParams.EnvironmentNetworkMockParamsBuilder
                        .anEnvironmentNetworkMockParams()
                        .withVpcId(p.getVpcId())
                        .withInternetGatewayId(p.getInternetGatewayId())
                        .build()))
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
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        EnvironmentEditDto.EnvironmentEditDtoBuilder builder = EnvironmentEditDto.builder()
                .withDescription(request.getDescription())
                .withAccountId(accountId)
                .withIdBrokerMappingSource(request.getIdBrokerMappingSource())
                .withCloudStorageValidation(request.getCloudStorageValidation())
                .withAdminGroupName(request.getAdminGroupName());
        NullUtil.doIfNotNull(request.getNetwork(), network -> builder.withNetwork(networkRequestToDto(network)));
        NullUtil.doIfNotNull(request.getAuthentication(), authentication -> builder.withAuthentication(authenticationRequestToDto(authentication)));
        NullUtil.doIfNotNull(request.getTelemetry(), telemetryRequest -> builder.withTelemetry(telemetryApiConverter.convert(request.getTelemetry(),
                accountTelemetryService.getOrDefault(accountId).getFeatures())));
        NullUtil.doIfNotNull(request.getSecurityAccess(), securityAccess -> builder.withSecurityAccess(securityAccessRequestToDto(securityAccess)));
        NullUtil.doIfNotNull(request.getAws(), awsParams -> builder.withParameters(awsParamsToParametersDto(awsParams)));
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

    public EnvironmentCrnResponse crnResponse(String environmentName, String crn) {
        EnvironmentCrnResponse response = new EnvironmentCrnResponse();
        response.setEnvironmentName(environmentName);
        response.setEnvironmentCrn(crn);
        return response;
    }
}
