package com.sequenceiq.cloudbreak.kerberos;

import java.util.Optional;

import javax.inject.Inject;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.vault.VaultException;

import com.sequenceiq.cloudbreak.dto.KerberosConfig;
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.service.secret.model.SecretResponse;
import com.sequenceiq.cloudbreak.service.secret.service.SecretService;
import com.sequenceiq.cloudbreak.type.KerberosType;
import com.sequenceiq.freeipa.api.v1.kerberos.KerberosConfigV1Endpoint;
import com.sequenceiq.freeipa.api.v1.kerberos.model.describe.DescribeKerberosConfigResponse;

@Service
public class KerberosConfigService {
    private static final Logger LOGGER = LoggerFactory.getLogger(KerberosConfigService.class);

    @Inject
    private KerberosConfigV1Endpoint kerberosConfigV1Endpoint;

    @Inject
    private SecretService secretService;

    public boolean isKerberosConfigExistsForEnvironment(String environmentCrn) {
        return describeKerberosConfig(environmentCrn).isPresent();
    }

    public Optional<KerberosConfig> get(String environmentCrn) {
        Optional<DescribeKerberosConfigResponse> describeKerberosConfigResponse = describeKerberosConfig(environmentCrn);
        return describeKerberosConfigResponse.map(this::convert);
    }

    private Optional<DescribeKerberosConfigResponse> describeKerberosConfig(String environmentCrn) {
        try {
            return Optional.of(kerberosConfigV1Endpoint.describe(environmentCrn));
        } catch (NotFoundException | ForbiddenException notFoundEx) {
            LOGGER.debug("No Kerberos config found for {} environment. Ldap setup will be skipped!", environmentCrn);
            return Optional.empty();
        } catch (RuntimeException communicationEx) {
            String message = String.format("Failed to get Kerberos config from FreeIpa service due to: '%s' ", communicationEx.getMessage());
            LOGGER.warn(message, communicationEx);
            throw new CloudbreakServiceException(message, communicationEx);
        }
    }

    private KerberosConfig convert(DescribeKerberosConfigResponse describeLdapConfigResponse) {
        return KerberosConfig.KerberosConfigBuilder.aKerberosConfig()
                .withAdminUrl(describeLdapConfigResponse.getAdminUrl())
                .withContainerDn(describeLdapConfigResponse.getContainerDn())
                .withDescriptor(getSecret(describeLdapConfigResponse.getDescriptor()))
                .withDomain(describeLdapConfigResponse.getDomain())
                .withKrb5Conf(getSecret(describeLdapConfigResponse.getKrb5Conf()))
                .withLdapUrl(describeLdapConfigResponse.getLdapUrl())
                .withNameServers(describeLdapConfigResponse.getNameServers())
                .withPassword(getSecret(describeLdapConfigResponse.getPassword()))
                .withPrincipal(getSecret(describeLdapConfigResponse.getPrincipal()))
                .withRealm(describeLdapConfigResponse.getRealm())
                .withTcpAllowed(describeLdapConfigResponse.getTcpAllowed())
                .withType(KerberosType.valueOf(describeLdapConfigResponse.getType().name()))
                .withUrl(describeLdapConfigResponse.getUrl())
                .withVerifyKdcTrust(describeLdapConfigResponse.getVerifyKdcTrust())
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
