package com.sequenceiq.cloudbreak.kerberos;

import java.util.Optional;

import javax.inject.Inject;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.vault.VaultException;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.cloudbreak.dto.KerberosConfig;
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

    @Inject
    private WebApplicationExceptionMessageExtractor exceptionMessageExtractor;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    public boolean isKerberosConfigExistsForEnvironment(String environmentCrn, String clusterName) {
        return describeKerberosConfig(environmentCrn, clusterName).isPresent();
    }

    public Optional<KerberosConfig> get(String environmentCrn, String clusterName) {
        Optional<DescribeKerberosConfigResponse> describeKerberosConfigResponse = describeKerberosConfig(environmentCrn, clusterName);
        return describeKerberosConfigResponse.map(this::convert);
    }

    private Optional<DescribeKerberosConfigResponse> describeKerberosConfig(String environmentCrn, String clusterName) {
        try {
            return Optional.of(ThreadBasedUserCrnProvider.doAsInternalActor(
                    regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                    () -> kerberosConfigV1Endpoint.getForCluster(environmentCrn, clusterName)));
        } catch (NotFoundException ex) {
            LOGGER.debug("No Kerberos config found for {} environment. Ldap setup will be skipped!", environmentCrn, ex);
            return Optional.empty();
        } catch (WebApplicationException e) {
            String errorMessage = exceptionMessageExtractor.getErrorMessage(e);
            String message = String.format("Failed to get Kerberos config from FreeIpa service due to: '%s' ", errorMessage);
            LOGGER.warn(message, e);
            throw new CloudbreakServiceException(message, e);
        } catch (Exception communicationEx) {
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
