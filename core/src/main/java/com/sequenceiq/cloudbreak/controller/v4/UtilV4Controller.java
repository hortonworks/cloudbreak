package com.sequenceiq.cloudbreak.controller.v4;

import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.AccountIdNotNeeded;
import com.sequenceiq.authorization.annotation.CheckPermissionByAccount;
import com.sequenceiq.authorization.annotation.DisableCheckPermissions;
import com.sequenceiq.authorization.annotation.InternalOnly;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.api.endpoint.v4.events.responses.CloudbreakEventBaseV4;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.UtilV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.requests.RenewCertificateV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.requests.RepoConfigValidationV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.CloudStorageSupportedV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.DeploymentPreferencesV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.RepoConfigValidationV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.ResourceEventResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.SecurityRulesV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.StackMatrixV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.SupportedExternalDatabaseServiceEntryV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.UsedImagesListV4Response;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.provider.ProviderPreferencesService;
import com.sequenceiq.cloudbreak.common.service.PlatformStringTransformer;
import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.converter.SupportedExternalDatabaseServiceEntryToSupportedExternalDatabaseServiceEntryResponseConverter;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.service.StackMatrixService;
import com.sequenceiq.cloudbreak.service.account.PreferencesService;
import com.sequenceiq.cloudbreak.service.cluster.RepositoryConfigValidationService;
import com.sequenceiq.cloudbreak.service.filesystem.FileSystemSupportMatrixService;
import com.sequenceiq.cloudbreak.service.image.UsedImagesProvider;
import com.sequenceiq.cloudbreak.service.securityrule.SecurityRuleService;
import com.sequenceiq.cloudbreak.service.stack.flow.StackOperationService;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.validation.externaldatabase.SupportedDatabaseProvider;
import com.sequenceiq.common.api.util.versionchecker.ClientVersionUtil;
import com.sequenceiq.common.api.util.versionchecker.VersionCheckResult;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.notification.WebSocketNotification;
import com.sequenceiq.notification.WebSocketNotificationService;

@Controller
public class UtilV4Controller extends NotificationController implements UtilV4Endpoint {

    @Inject
    private StackMatrixService stackMatrixService;

    @Inject
    private FileSystemSupportMatrixService fileSystemSupportMatrixService;

    @Inject
    private RepositoryConfigValidationService validationService;

    @Inject
    private ProviderPreferencesService providerPreferencesService;

    @Inject
    private PreferencesService preferencesService;

    @Inject
    private SecurityRuleService securityRuleService;

    @Inject
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    private WebSocketNotificationService webSocketNotificationService;

    @Inject
    private StackOperationService stackOperationService;

    @Inject
    private SupportedExternalDatabaseServiceEntryToSupportedExternalDatabaseServiceEntryResponseConverter supportedExternalDatabaseServiceEntryResponseConverter;

    @Inject
    private UsedImagesProvider usedImagesProvider;

    @Inject
    private PlatformStringTransformer platformStringTransformer;

    @Value("${info.app.version:}")
    private String cbVersion;

    @Value("${crn.region:}")
    private String region;

    @Override
    @DisableCheckPermissions
    public VersionCheckResult checkClientVersion(String version) {
        return ClientVersionUtil.checkClientVersion(cbVersion, version);
    }

    @Override
    @DisableCheckPermissions
    public StackMatrixV4Response getStackMatrix(String imageCatalogName, String platform, boolean govCloud, String os, String architecture)
            throws Exception {
        Architecture architectureEnum = Architecture.fromStringWithValidation(architecture);
        return stackMatrixService.getStackMatrix(restRequestThreadLocalService.getRequestedWorkspaceId(),
                platformStringTransformer.getPlatformStringForImageCatalog(platform, govCloud), os, architectureEnum, imageCatalogName);
    }

    @Override
    @DisableCheckPermissions
    public CloudStorageSupportedV4Responses getCloudStorageMatrix(String stackVersion) {
        return new CloudStorageSupportedV4Responses(fileSystemSupportMatrixService.getCloudStorageMatrix(stackVersion));
    }

    @Override
    @DisableCheckPermissions
    public RepoConfigValidationV4Response repositoryConfigValidationRequest(RepoConfigValidationV4Request repoConfigValidationV4Request) {
        return validationService.validate(repoConfigValidationV4Request);
    }

    @Override
    @DisableCheckPermissions
    public SecurityRulesV4Response getDefaultSecurityRules() {
        return securityRuleService.getDefaultSecurityRules();
    }

    @Override
    @DisableCheckPermissions
    public DeploymentPreferencesV4Response deployment() {
        DeploymentPreferencesV4Response response = new DeploymentPreferencesV4Response();
        response.setFeatureSwitchV4s(preferencesService.getFeatureSwitches());
        Set<SupportedExternalDatabaseServiceEntryV4Response> supportedExternalDatabases =
                SupportedDatabaseProvider.supportedExternalDatabases().stream()
                .map(s -> supportedExternalDatabaseServiceEntryResponseConverter.convert(s))
                .collect(Collectors.toSet());
        response.setSupportedExternalDatabases(supportedExternalDatabases);
        response.setPlatformSelectionDisabled(providerPreferencesService.isPlatformSelectionDisabled());
        response.setPlatformEnablement(providerPreferencesService.platformEnablement());
        response.setGovPlatformEnablement(providerPreferencesService.govPlatformEnablement());
        response.setControlPlaneRegion(region);
        return response;
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public ResourceEventResponse postNotificationTest() {
        CloudbreakUser cloudbreakUser = restRequestThreadLocalService.getCloudbreakUser();
        webSocketNotificationService.send(new WebSocketNotification<>(createTestNotification(cloudbreakUser.getUserId())));
        ResourceEventResponse response = new ResourceEventResponse();
        response.setEvent(ResourceEvent.CREDENTIAL_CREATED);
        return response;
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public Response renewCertificate(RenewCertificateV4Request renewCertificateV4Request) {
        stackOperationService.renewCertificate(renewCertificateV4Request.getStackName(), ThreadBasedUserCrnProvider.getAccountId());
        return Response.ok().build();
    }

    @Override
    @InternalOnly
    @AccountIdNotNeeded
    public UsedImagesListV4Response usedImages(Integer thresholdInDays) {
        return usedImagesProvider.getUsedImages(thresholdInDays);
    }

    private CloudbreakEventBaseV4 createTestNotification(String userId) {
        CloudbreakEventBaseV4 baseEvent = new CloudbreakEventBaseV4();
        baseEvent.setEventType("TEST_NOTIFICATION");
        baseEvent.setEventMessage("Test notification message.");
        baseEvent.setEventTimestamp(System.currentTimeMillis());
        baseEvent.setUserId(userId);
        baseEvent.setNotificationType("TEST_NOTIFICATION");
        return baseEvent;
    }
}
