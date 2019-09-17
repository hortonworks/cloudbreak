package com.sequenceiq.cloudbreak.service.stack.flow;

import static com.sequenceiq.cloudbreak.exception.NotFoundException.notFound;

import java.security.KeyPair;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;

import org.glassfish.jersey.SslConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.google.common.io.BaseEncoding;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.certificate.PkiUtil;
import com.sequenceiq.cloudbreak.certificate.service.CertificateCreationService;
import com.sequenceiq.cloudbreak.certificate.service.DnsManagementService;
import com.sequenceiq.cloudbreak.client.CertificateTrustManager.SavingX509TrustManager;
import com.sequenceiq.cloudbreak.client.RestClientUtil;
import com.sequenceiq.cloudbreak.core.flow2.stack.termination.StackTerminationService;
import com.sequenceiq.cloudbreak.dns.EnvironmentBasedDomainNameProvider;
import com.sequenceiq.cloudbreak.domain.SecurityConfig;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.logger.LoggerContextKey;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.polling.PollingService;
import com.sequenceiq.cloudbreak.polling.nginx.NginxCertListenerTask;
import com.sequenceiq.cloudbreak.polling.nginx.NginxPollerObject;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentClientService;
import com.sequenceiq.cloudbreak.service.securityconfig.SecurityConfigService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@Component
public class TlsSetupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TlsSetupService.class);

    private static final int POLLING_INTERVAL = 5000;

    private static final long FIVE_MIN = 5 * 60;

    private static final int MAX_FAILURE = 1;

    @Value("${gateway.cert.generation.enabled:false}")
    private boolean certGenerationEnabled;

    @Inject
    private PollingService<NginxPollerObject> nginxPollerService;

    @Inject
    private NginxCertListenerTask nginxCertListenerTask;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private CertificateCreationService certificateCreationService;

    @Inject
    private ThreadBasedUserCrnProvider threadBasedUserCrnProvider;

    @Inject
    private SecurityConfigService securityConfigService;

    @Inject
    private EnvironmentClientService environmentClientService;

    @Inject
    private DnsManagementService dnsManagementService;

    @Inject
    private StackTerminationService stackTerminationService;

    @Inject
    private EnvironmentBasedDomainNameProvider environmentBasedDomainNameProvider;

    @Inject
    private GrpcUmsClient grpcUmsClient;

    public void generateCertAndSaveForStack(Stack stack) {
        LOGGER.info("Generate cert and save for stack");
        String userCrn = threadBasedUserCrnProvider.getUserCrn();
        String accountId = threadBasedUserCrnProvider.getAccountId();
        KeyPair keyPair;
        SecurityConfig securityConfig = stack.getSecurityConfig();
        if (StringUtils.isEmpty(securityConfig.getUserFacingKey())) {
            keyPair = PkiUtil.generateKeypair();
            securityConfig.setUserFacingKey(PkiUtil.convert(keyPair.getPrivate()));
            securityConfig = securityConfigService.save(securityConfig);
        } else {
            keyPair = PkiUtil.fromPrivateKeyPem(securityConfig.getUserFacingKey());
            if (keyPair == null) {
                keyPair = PkiUtil.generateKeypair();
            }
        }
        try {
            DetailedEnvironmentResponse environment = environmentClientService.getByCrn(stack.getEnvironmentCrn());
            List<String> certs = certificateCreationService.create(userCrn, accountId, stack.getName(), environment.getName(), false, keyPair);
            securityConfig.setUserFacingCert(String.join("", certs));
            securityConfigService.save(securityConfig);
        } catch (Exception e) {
            LOGGER.info("The cert could not be generated by Cluster DNS generator service: " + e.getMessage(), e);
        }
    }

    public void setupTls(Stack stack, InstanceMetaData gwInstance) throws CloudbreakException {
        try {
            SavingX509TrustManager x509TrustManager = new SavingX509TrustManager();
            TrustManager[] trustManagers = {x509TrustManager};
            SSLContext sslContext = SslConfigurator.newInstance().createSSLContext();
            sslContext.init(null, trustManagers, new SecureRandom());
            Client client = RestClientUtil.createClient(sslContext, false);
            Integer gatewayPort = stack.getGatewayPort();
            String ip = gatewayConfigService.getGatewayIp(stack, gwInstance);
            LOGGER.debug("Trying to fetch the server's certificate: {}:{}", ip, gatewayPort);
            nginxPollerService.pollWithAbsolutTimeout(
                    nginxCertListenerTask, new NginxPollerObject(client, ip, gatewayPort, x509TrustManager),
                    POLLING_INTERVAL, FIVE_MIN, MAX_FAILURE);
            WebTarget nginxTarget = client.target(String.format("https://%s:%d", ip, gatewayPort));
            nginxTarget.path("/").request().get().close();
            X509Certificate[] chain = x509TrustManager.getChain();
            String serverCert = PkiUtil.convert(chain[0]);
            InstanceMetaData metaData = getInstanceMetaData(gwInstance);
            metaData.setServerCert(BaseEncoding.base64().encode(serverCert.getBytes()));
            instanceMetaDataService.save(metaData);
        } catch (Exception e) {
            throw new CloudbreakException("Failed to retrieve the server's certificate from Nginx."
                    + " Please check your security group is open enough and Cloudbreak can access your VPC and subnet", e);
        }
    }

    private InstanceMetaData getInstanceMetaData(InstanceMetaData gwInstance) {
        return instanceMetaDataService.findById(gwInstance.getId())
                .orElseThrow(notFound("Instance metadata", gwInstance.getId()));
    }

    public boolean isCertGenerationEnabled() {
        return certGenerationEnabled;
    }

    public String updateDnsEntry(Stack stack) {
        LOGGER.info("Update dns entry");
        String userCrn = threadBasedUserCrnProvider.getUserCrn();
        String accountId = threadBasedUserCrnProvider.getAccountId();
        DetailedEnvironmentResponse environment = environmentClientService.getByCrn(stack.getEnvironmentCrn());

        String ip = stackTerminationService.deleteDnsEntry(stack, environment.getName());
        if (ip == null) {
            return null;
        }
        boolean success = dnsManagementService.createDnsEntryWithIp(userCrn, accountId, stack.getName(), environment.getName(), false, List.of(ip));
        if (success) {
            try {
                String fullQualifiedDomainName = environmentBasedDomainNameProvider
                        .getDomainName(stack.getName(), environment.getName(), getWorkloadSubdomain(userCrn));
                if (fullQualifiedDomainName != null) {
                    LOGGER.info("Dns entry updated: ip: {}, FQDN: {}", ip, fullQualifiedDomainName);
                    return fullQualifiedDomainName;
                }
            } catch (Exception e) {
                LOGGER.info("Cannot generate fqdn.", e.getMessage(), e);
            }
        }
        return null;
    }

    private String getWorkloadSubdomain(String actorCrn) {
        Optional<String> requestIdOptional = Optional.ofNullable(MDCBuilder.getMdcContextMap().get(LoggerContextKey.REQUEST_ID.toString()));
        UserManagementProto.Account account = grpcUmsClient.getAccountDetails(actorCrn, actorCrn, requestIdOptional);
        return account.getWorkloadSubdomain();
    }
}
