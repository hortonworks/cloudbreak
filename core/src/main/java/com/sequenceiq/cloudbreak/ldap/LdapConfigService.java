package com.sequenceiq.cloudbreak.ldap;

import java.util.Optional;

import javax.inject.Inject;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.vault.VaultException;

import com.sequenceiq.cloudbreak.api.endpoint.v4.ldaps.DirectoryType;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.cloudbreak.dto.LdapView;
import com.sequenceiq.cloudbreak.dto.LdapView.LdapViewBuilder;
import com.sequenceiq.cloudbreak.service.secret.model.SecretResponse;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.freeipa.api.v1.ldap.LdapConfigV1Endpoint;
import com.sequenceiq.freeipa.api.v1.ldap.model.describe.DescribeLdapConfigResponse;

@Service
public class LdapConfigService {
    private static final Logger LOGGER = LoggerFactory.getLogger(LdapConfigService.class);

    private static final int MAX_ATTEMPT = 5;

    private static final int DELAY = 5000;

    @Inject
    private LdapConfigV1Endpoint ldapConfigV1Endpoint;

    @Inject
    private SecretService secretService;

    @Inject
    private WebApplicationExceptionMessageExtractor exceptionMessageExtractor;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Retryable(value = CloudbreakServiceException.class, maxAttempts = MAX_ATTEMPT, backoff = @Backoff(delay = DELAY))
    public boolean isLdapConfigExistsForEnvironment(String environmentCrn, String clusterName) {
        return describeLdapConfig(environmentCrn, clusterName).isPresent();
    }

    @Retryable(value = CloudbreakServiceException.class, maxAttempts = MAX_ATTEMPT, backoff = @Backoff(delay = DELAY))
    public Optional<LdapView> get(String environmentCrn, String clusterName) {
        Optional<DescribeLdapConfigResponse> describeLdapConfigResponse = describeLdapConfig(environmentCrn, clusterName);
        return describeLdapConfigResponse.map(this::convert);
    }

    private Optional<DescribeLdapConfigResponse> describeLdapConfig(String environmentCrn, String clusterName) {
        try {
            return Optional.of(ThreadBasedUserCrnProvider.doAsInternalActor(
                    regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                    () -> ldapConfigV1Endpoint.getForCluster(environmentCrn, clusterName)));
        } catch (NotFoundException e) {
            LOGGER.debug("No Ldap config found for {} environment. Ldap setup will be skipped!", environmentCrn, e);
            return Optional.empty();
        } catch (WebApplicationException e) {
            String errorMessage = exceptionMessageExtractor.getErrorMessage(e);
            String message = String.format("Failed to get Ldap config from FreeIpa service due to: '%s' ", errorMessage);
            LOGGER.warn(message, e);
            throw new CloudbreakServiceException(message, e);
        } catch (Exception communicationEx) {
            String message = String.format("Failed to get Ldap config from FreeIpa service due to: '%s' ", communicationEx.getMessage());
            LOGGER.warn(message, communicationEx);
            throw new CloudbreakServiceException(message, communicationEx);
        }
    }

    private LdapView convert(DescribeLdapConfigResponse describeLdapConfigResponse) {
        String protocol = describeLdapConfigResponse.getProtocol().toLowerCase();
        String connectionUrl = protocol + "://" + describeLdapConfigResponse.getHost();
        if (describeLdapConfigResponse.getPort() != null) {
            connectionUrl = connectionUrl.toLowerCase() + ':' + describeLdapConfigResponse.getPort();
        }
        return LdapViewBuilder.aLdapView()
                .withProtocol(protocol)
                .withServerHost(describeLdapConfigResponse.getHost())
                .withServerPort(describeLdapConfigResponse.getPort())
                .withConnectionURL(connectionUrl)
                .withDirectoryType(DirectoryType.valueOf(describeLdapConfigResponse.getDirectoryType().name()))
                .withUserSearchBase(describeLdapConfigResponse.getUserSearchBase())
                .withUserNameAttribute(describeLdapConfigResponse.getUserNameAttribute())
                .withUserObjectClass(describeLdapConfigResponse.getUserObjectClass())
                .withGroupSearchBase(describeLdapConfigResponse.getGroupSearchBase())
                .withGroupNameAttribute(describeLdapConfigResponse.getGroupNameAttribute())
                .withGroupObjectClass(describeLdapConfigResponse.getGroupObjectClass())
                .withGroupMemberAttribute(describeLdapConfigResponse.getGroupMemberAttribute())
                .withDomain(describeLdapConfigResponse.getDomain())
                .withUserDnPattern(describeLdapConfigResponse.getUserDnPattern())
                .withAdminGroup(describeLdapConfigResponse.getAdminGroup())
                .withUserGroup(describeLdapConfigResponse.getUserGroup())
                .withCertificate(describeLdapConfigResponse.getCertificate())
                .withBindDn(getSecret(describeLdapConfigResponse.getBindDn()))
                .withBindPassword(getSecret(describeLdapConfigResponse.getBindPassword()))
                .build();
    }

    private String getSecret(SecretResponse userName) {
        try {
            return secretService.getByResponse(userName);
        } catch (VaultException e) {
            String message = String.format("Failed to get Ldap config related secret due to: '%s' ", e.getMessage());
            LOGGER.warn(message, e);
            throw new CloudbreakServiceException(message, e);
        }
    }
}
