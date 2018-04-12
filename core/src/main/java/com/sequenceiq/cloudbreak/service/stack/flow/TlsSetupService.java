package com.sequenceiq.cloudbreak.service.stack.flow;

import com.google.common.io.BaseEncoding;
import com.sequenceiq.cloudbreak.client.CertificateTrustManager.SavingX509TrustManager;
import com.sequenceiq.cloudbreak.client.PkiUtil;
import com.sequenceiq.cloudbreak.client.RestClientUtil;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.PollingService;
import org.glassfish.jersey.SslConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

@Component
public class TlsSetupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TlsSetupService.class);

    private static final int POLLING_INTERVAL = 5000;

    private static final int MAX_ATTEMPTS_FOR_HOSTS = 100;

    @Inject
    private PollingService<NginxPollerObject> nginxPollerService;

    @Inject
    private NginxCertListenerTask nginxCertListenerTask;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private InstanceMetaDataRepository instanceMetaDataRepository;

    public void setupTls(Stack stack, InstanceMetaData gwInstance) throws CloudbreakException {
        try {
            SavingX509TrustManager x509TrustManager = new SavingX509TrustManager();
            TrustManager[] trustManagers = {x509TrustManager};
            SSLContext sslContext = SslConfigurator.newInstance().createSSLContext();
            sslContext.init(null, trustManagers, new SecureRandom());
            Client client = RestClientUtil.createClient(sslContext, false, null);
            Integer gatewayPort = stack.getGatewayPort();
            String ip = gatewayConfigService.getGatewayIp(stack, gwInstance);
            LOGGER.info("Trying to fetch the server's certificate: {}:{}", ip, gatewayPort);
            nginxPollerService.pollWithTimeoutSingleFailure(
                nginxCertListenerTask, new NginxPollerObject(stack, client, ip, gatewayPort, x509TrustManager),
                POLLING_INTERVAL, MAX_ATTEMPTS_FOR_HOSTS);
            WebTarget nginxTarget = client.target(String.format("https://%s:%d", ip, gatewayPort));
            nginxTarget.path("/").request().get();
            X509Certificate[] chain = x509TrustManager.getChain();
            String serverCert = PkiUtil.convert(chain[0]);
            InstanceMetaData metaData = instanceMetaDataRepository.findOne(gwInstance.getId());
            metaData.setServerCert(BaseEncoding.base64().encode(serverCert.getBytes()));
            instanceMetaDataRepository.save(metaData);
        } catch (Exception e) {
            throw new CloudbreakException("Failed to retrieve the server's certificate", e);
        }
    }

}
