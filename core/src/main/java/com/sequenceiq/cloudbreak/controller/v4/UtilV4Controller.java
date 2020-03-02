package com.sequenceiq.cloudbreak.controller.v4;

import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.core.Response;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.DisableCheckPermissions;
import com.sequenceiq.authorization.service.UmsAuthorizationService;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.UtilV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.requests.CheckRightV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.requests.RenewCertificateV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.requests.RepoConfigValidationV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.CheckRightV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.CheckRightV4SingleResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.CloudStorageSupportedV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.DeploymentPreferencesV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.RepoConfigValidationV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.ResourceEventResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.SecurityRulesV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.StackMatrixV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.SupportedExternalDatabaseServiceEntryV4Response;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.notification.NotificationSender;
import com.sequenceiq.cloudbreak.service.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.StackMatrixService;
import com.sequenceiq.cloudbreak.service.account.PreferencesService;
import com.sequenceiq.cloudbreak.service.cluster.RepositoryConfigValidationService;
import com.sequenceiq.cloudbreak.service.filesystem.FileSystemSupportMatrixService;
import com.sequenceiq.cloudbreak.service.securityrule.SecurityRuleService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.validation.externaldatabase.SupportedDatabaseProvider;
import com.sequenceiq.common.api.util.versionchecker.ClientVersionUtil;
import com.sequenceiq.common.api.util.versionchecker.VersionCheckResult;

@Controller
@DisableCheckPermissions
public class UtilV4Controller extends NotificationController implements UtilV4Endpoint {

    @Inject
    private StackMatrixService stackMatrixService;

    @Inject
    private FileSystemSupportMatrixService fileSystemSupportMatrixService;

    @Inject
    private RepositoryConfigValidationService validationService;

    @Inject
    private PreferencesService preferencesService;

    @Inject
    private SecurityRuleService securityRuleService;

    @Inject
    private ConverterUtil converterUtil;

    @Inject
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    private NotificationSender notificationSender;

    @Inject
    private UmsAuthorizationService umsAuthorizationService;

    @Inject
    private StackService stackService;

    @Value("${info.app.version:}")
    private String cbVersion;

    @Override
    public VersionCheckResult checkClientVersion(String version) {
        return ClientVersionUtil.checkClientVersion(cbVersion, version);
    }

    @Override
    public StackMatrixV4Response getStackMatrix() {
        return stackMatrixService.getStackMatrix();
    }

    @Override
    public CloudStorageSupportedV4Responses getCloudStorageMatrix(String stackVersion) {
        return new CloudStorageSupportedV4Responses(fileSystemSupportMatrixService.getCloudStorageMatrix(stackVersion));
    }

    @Override
    public RepoConfigValidationV4Response repositoryConfigValidationRequest(RepoConfigValidationV4Request repoConfigValidationV4Request) {
        return validationService.validate(repoConfigValidationV4Request);
    }

    @Override
    public SecurityRulesV4Response getDefaultSecurityRules() {
        return securityRuleService.getDefaultSecurityRules();
    }

    @Override
    public DeploymentPreferencesV4Response deployment() {
        DeploymentPreferencesV4Response response = new DeploymentPreferencesV4Response();
        response.setFeatureSwitchV4s(preferencesService.getFeatureSwitches());
        Set<SupportedExternalDatabaseServiceEntryV4Response> supportedExternalDatabases =
                converterUtil.convertAllAsSet(SupportedDatabaseProvider.supportedExternalDatabases(), SupportedExternalDatabaseServiceEntryV4Response.class);
        response.setSupportedExternalDatabases(supportedExternalDatabases);
        response.setPlatformSelectionDisabled(preferencesService.isPlatformSelectionDisabled());
        response.setPlatformEnablement(preferencesService.platformEnablement());
        return response;
    }

    @Override
    public ResourceEventResponse postNotificationTest() {
        CloudbreakUser cloudbreakUser = restRequestThreadLocalService.getCloudbreakUser();
        notificationSender.sendTestNotification(cloudbreakUser.getUserId());
        ResourceEventResponse response = new ResourceEventResponse();
        response.setEvent(ResourceEvent.CREDENTIAL_CREATED);
        return response;
    }

    @Override
    public CheckRightV4Response checkRight(CheckRightV4Request checkRightV4Request) {
        String userCrn = restRequestThreadLocalService.getCloudbreakUser().getUserCrn();
        return new CheckRightV4Response(checkRightV4Request.getRights().stream()
                .map(rightReq -> new CheckRightV4SingleResponse(rightReq,
                        umsAuthorizationService.hasRightOfUser(userCrn, rightReq.getResource(), rightReq.getAction())))
                .collect(Collectors.toList()));
    }

    @Override
    public Response renewCertificate(RenewCertificateV4Request renewCertificateV4Request) {
        stackService.renewCertificate(renewCertificateV4Request.getStackName());
        return Response.ok().build();
    }
}
