package com.sequenceiq.cloudbreak.ldap;

import java.util.Optional;

import javax.inject.Inject;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.vault.VaultException;

import com.sequenceiq.cloudbreak.api.endpoint.v4.ldaps.DirectoryType;
import com.sequenceiq.cloudbreak.dto.LdapView;
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.service.secret.model.SecretResponse;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.freeipa.api.v1.ldap.LdapConfigV1Endpoint;
import com.sequenceiq.freeipa.api.v1.ldap.model.describe.DescribeLdapConfigResponse;

@Service
public class LdapConfigService {
    private static final Logger LOGGER = LoggerFactory.getLogger(LdapConfigService.class);

    @Inject
    private LdapConfigV1Endpoint ldapConfigV1Endpoint;

    @Inject
    private SecretService secretService;

    public boolean isLdapConfigExistsForEnvironment(String environmentCrn) {
        return describeLdapConfig(environmentCrn).isPresent();
    }

    public Optional<LdapView> get(String environmentCrn) {
        Optional<DescribeLdapConfigResponse> describeLdapConfigResponse = describeLdapConfig(environmentCrn);
        return describeLdapConfigResponse.map(this::convert);
    }

    private Optional<DescribeLdapConfigResponse> describeLdapConfig(String environmentCrn) {
        try {
            return Optional.of(ldapConfigV1Endpoint.describe(environmentCrn));
        } catch (NotFoundException | ForbiddenException notFoundEx) {
            LOGGER.debug("No Ldap config found for {} environment. Ldap setup will be skipped!", environmentCrn);
            return Optional.empty();
        } catch (RuntimeException communicationEx) {
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
        return LdapView.LdapViewBuilder.aLdapView()
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
