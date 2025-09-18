package com.sequenceiq.distrox.v1.distrox.service;

import static java.lang.String.format;

import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.kerberos.KerberosConfigService;
import com.sequenceiq.cloudbreak.ldap.LdapConfigService;
import com.sequenceiq.cloudbreak.sdx.common.PlatformAwareSdxConnector;
import com.sequenceiq.cloudbreak.sdx.common.status.StatusCheckResult;
import com.sequenceiq.cloudbreak.service.ReservedTagValidatorService;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentService;
import com.sequenceiq.cloudbreak.service.freeipa.FreeipaClientService;
import com.sequenceiq.cloudbreak.service.image.ImageOsService;
import com.sequenceiq.cloudbreak.service.validation.SeLinuxValidationService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.common.api.type.LoadBalancerSku;
import com.sequenceiq.common.model.AzureDatabaseType;
import com.sequenceiq.common.model.SeLinux;
import com.sequenceiq.distrox.api.v1.distrox.model.AzureDistroXV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.database.DistroXDatabaseAzureRequest;
import com.sequenceiq.distrox.api.v1.distrox.model.database.DistroXDatabaseRequest;
import com.sequenceiq.distrox.api.v1.distrox.model.image.DistroXImageV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.security.SecurityV1Request;
import com.sequenceiq.distrox.v1.distrox.StackOperations;
import com.sequenceiq.distrox.v1.distrox.converter.DistroXV1RequestToStackV4RequestConverter;
import com.sequenceiq.distrox.v1.distrox.fedramp.FedRampModificationService;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.TrustStatus;

@Service
public class DistroXCreateService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistroXCreateService.class);

    @Inject
    private StackOperations stackOperations;

    @Inject
    private WorkspaceService workspaceService;

    @Inject
    private EnvironmentService environmentClientService;

    @Inject
    private DistroXV1RequestToStackV4RequestConverter stackRequestConverter;

    @Inject
    private FreeipaClientService freeipaClientService;

    @Inject
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    private PlatformAwareSdxConnector platformAwareSdxConnector;

    @Inject
    private FedRampModificationService fedRampModificationService;

    @Inject
    private ImageOsService imageOsService;

    @Inject
    private ReservedTagValidatorService reservedTagValidatorService;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private LdapConfigService ldapConfigService;

    @Inject
    private KerberosConfigService kerberosConfigService;

    @Inject
    private SeLinuxValidationService seLinuxValidationService;

    public StackV4Response create(DistroXV1Request request, boolean internalRequest) {
        Workspace workspace = workspaceService.getForCurrentUser();
        validate(request, internalRequest);
        fedRampModificationService.prepare(request, workspace.getTenant().getName());
        return stackOperations.post(
                workspace.getId(),
                restRequestThreadLocalService.getCloudbreakUser(),
                stackRequestConverter.convert(request),
                true);
    }

    private void validate(DistroXV1Request request, boolean internalRequest) {
        if (!internalRequest) {
            Optional.of(request).map(DistroXV1Request::getTags).ifPresent(tagRequest -> {
                reservedTagValidatorService.validateInternalTags(tagRequest.getApplication());
                reservedTagValidatorService.validateInternalTags(tagRequest.getDefaults());
                reservedTagValidatorService.validateInternalTags(tagRequest.getUserDefined());
            });
        }
        DetailedEnvironmentResponse environment = Optional.ofNullable(environmentClientService.getByName(request.getEnvironmentName()))
                .orElseThrow(() -> new BadRequestException("No environment name provided hence unable to obtain some important data"));
        validateEnvironment(request, environment);
        validateIdentityManagement(request, environment);
        validateDatalake(environment);
        validateImageRequest(request.getImage());
        if (entitlementService.isSingleServerRejectEnabled(ThreadBasedUserCrnProvider.getAccountId())) {
            validateAzureDatabaseType(request.getExternalDatabase(), environment.getCloudPlatform());
        }
        validateLoadBalancerSku(request.getAzure());
        validateSeLinuxEntitlement(request);
    }

    private void validateEnvironment(DistroXV1Request request, DetailedEnvironmentResponse environment) {
        if (environment.getEnvironmentStatus().isDeleteInProgress()) {
            throw new BadRequestException(format("'%s' Environment can not be delete in progress state.", request.getEnvironmentName()));
        }
    }

    private void validateIdentityManagement(DistroXV1Request request, DetailedEnvironmentResponse environment) {
        String environmentCrn = environment.getCrn();
        try {
            DescribeFreeIpaResponse freeipa = freeipaClientService.getByEnvironmentCrn(environmentCrn);
            validateFreeipa(environment, freeipa);
        } catch (CloudbreakServiceException e) {
            LOGGER.warn("Failed to fetch FreeIPA for the environment: {}: {}", environmentCrn, e.getMessage());
            validateLdapAndKerberosSettings(environmentCrn, request.getName());
        }
    }

    private void validateFreeipa(DetailedEnvironmentResponse environment, DescribeFreeIpaResponse freeipa) {
        if (freeipa == null || freeipa.getAvailabilityStatus() == null || !freeipa.getAvailabilityStatus().isAvailable()) {
            throw new BadRequestException(format("If you want to provision a Data Hub then the FreeIPA instance must be running in the '%s' Environment.",
                    environment.getName()));
        }
        if (freeipa.getTrust() != null && StringUtils.isNotBlank(freeipa.getTrust().getTrustStatus())
                && !TrustStatus.valueOf(freeipa.getTrust().getTrustStatus()).isActive()) {
            throw new BadRequestException(format("Cross realm trust must be active when launching Data Hub in the '%s' Environment.",
                    environment.getName()));
        }
    }

    private void validateLdapAndKerberosSettings(String environmentCrn, String clusterName) {
        if (!ldapConfigService.isLdapConfigExistsForEnvironment(environmentCrn, clusterName)) {
            throw new BadRequestException("If you want to provision a Data Hub without FreeIPA then please register an LDAP config");
        }
        if (!kerberosConfigService.isKerberosConfigExistsForEnvironment(environmentCrn, clusterName)) {
            throw new BadRequestException("If you want to provision a Data Hub without FreeIPA then please register a Kerberos config");
        }
    }

    private void validateDatalake(DetailedEnvironmentResponse environment) {
        String environmentCrn = environment.getCrn();
        Set<Pair<String, StatusCheckResult>> sdxCrnsWithAvailability = platformAwareSdxConnector.listSdxCrnsWithAvailability(environmentCrn);
        if (sdxCrnsWithAvailability.isEmpty()) {
            throw new BadRequestException(format("Data Lake stack cannot be found for environment: %s (%s)",
                    environment.getName(), environmentCrn));
        }
        if (!sdxCrnsWithAvailability.stream().map(Pair::getValue).allMatch(isSdxAvailable())) {
            throw new BadRequestException("Data Lake stacks of environment should be available.");
        }
    }

    private void validateLoadBalancerSku(AzureDistroXV1Parameters azure) {
        Optional.ofNullable(azure)
                .map(AzureDistroXV1Parameters::getLoadBalancerSku)
                .ifPresent(sku -> {
                    if (LoadBalancerSku.BASIC.equals(sku)) {
                        throw new BadRequestException("The Basic SKU type is no longer supported for Load Balancers. "
                                + "Please use the Standard SKU to provision a Load Balancer. Check documentation for more information: "
                                + "https://azure.microsoft.com/en-gb/updates?id="
                                + "azure-basic-load-balancer-will-be-retired-on-30-september-2025-upgrade-to-standard-load-balancer");
                    }
                });
    }

    private Predicate<StatusCheckResult> isSdxAvailable() {
        return statusResult -> StatusCheckResult.AVAILABLE.name().equals(statusResult.name())
                || StatusCheckResult.ROLLING_UPGRADE_IN_PROGRESS.name().equals(statusResult.name());
    }

    private void validateImageRequest(DistroXImageV1Request imageRequest) {
        if (imageRequest != null) {
            if (!imageOsService.isSupported(imageRequest.getOs())) {
                throw new BadRequestException(String.format("Image os '%s' is not supported in your account.", imageRequest.getOs()));
            }
            if (StringUtils.isNotBlank(imageRequest.getId())) {
                if (StringUtils.isNotBlank(imageRequest.getOs())) {
                    throw new BadRequestException("Image request can not have both image id and os parameters set.");
                }
            }
        }
    }

    private void validateAzureDatabaseType(DistroXDatabaseRequest externalDatabase, String cloudPlatform) {
        if (CloudPlatform.AZURE.equalsIgnoreCase(cloudPlatform)) {
            Optional.ofNullable(externalDatabase)
                    .map(DistroXDatabaseRequest::getDatabaseAzureRequest)
                    .map(DistroXDatabaseAzureRequest::getAzureDatabaseType)
                    .ifPresent(azureDatabaseType -> {
                        if (azureDatabaseType == AzureDatabaseType.SINGLE_SERVER) {
                            throw new BadRequestException("Azure Database for PostgreSQL - Single Server is retired. " +
                                    "New deployments cannot be created anymore. Check documentation for more information: " +
                                    "https://learn.microsoft.com/en-us/azure/postgresql/migrate/whats-happening-to-postgresql-single-server");
                        }
                    });
        }
    }

    private void validateSeLinuxEntitlement(DistroXV1Request request) {
        SeLinux seLinuxModeFromRequest = Optional.ofNullable(request.getSecurity())
                .map(SecurityV1Request::getSeLinux)
                .map(SeLinux::fromStringWithFallback)
                .orElse(SeLinux.PERMISSIVE);
        seLinuxValidationService.validateSeLinuxEntitlementGranted(seLinuxModeFromRequest);
    }
}
