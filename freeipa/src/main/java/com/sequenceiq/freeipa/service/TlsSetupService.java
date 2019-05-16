package com.sequenceiq.freeipa.service;


import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Set;

import javax.inject.Inject;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;

import org.glassfish.jersey.SslConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.io.BaseEncoding;
import com.sequenceiq.cloudbreak.certificate.CertificateTrustManager.SavingX509TrustManager;
import com.sequenceiq.cloudbreak.certificate.PkiUtil;
import com.sequenceiq.cloudbreak.polling.PollingService;
import com.sequenceiq.cloudbreak.restclient.RestClientUtil;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.repository.InstanceMetaDataRepository;
import com.sequenceiq.freeipa.repository.StackRepository;
import com.sequenceiq.freeipa.service.polling.NginxCertListenerTask;
import com.sequenceiq.freeipa.service.polling.NginxPollerObject;

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
    private InstanceMetaDataRepository instanceMetaDataRepository;

    @Inject
    private StackRepository stackRepository;

    public void setupTls(Long stackId) throws CloudbreakException {
        try {
            SavingX509TrustManager x509TrustManager = new SavingX509TrustManager();
            TrustManager[] trustManagers = {x509TrustManager};
            SSLContext sslContext = SslConfigurator.newInstance().createSSLContext();
            sslContext.init(null, trustManagers, new SecureRandom());
            Client client = RestClientUtil.createClient(sslContext, false, null);
            Set<InstanceMetaData> instanceMetaDataSet = instanceMetaDataRepository.findAllInStack(stackId);
            InstanceMetaData instanceMetaData = instanceMetaDataSet.iterator().next();
            String ip = instanceMetaData.getPublicIpWrapper();
            Stack stack = stackRepository.findById(stackId).get();
            Integer gatewayPort = stack.getGatewayport();
            LOGGER.debug("Trying to fetch the server's certificate: {}:{}", ip, gatewayPort);
            nginxPollerService.pollWithTimeoutSingleFailure(
                nginxCertListenerTask, new NginxPollerObject(stack, client, ip, gatewayPort, x509TrustManager),
                POLLING_INTERVAL, MAX_ATTEMPTS_FOR_HOSTS);
            WebTarget nginxTarget = client.target(String.format("https://%s:%d", ip, gatewayPort));
            nginxTarget.path("/").request().get().close();
            X509Certificate[] chain = x509TrustManager.getChain();
            String serverCert = PkiUtil.convert(chain[0]);
            instanceMetaData.setServerCert(BaseEncoding.base64().encode(serverCert.getBytes()));
            instanceMetaDataRepository.save(instanceMetaData);
        } catch (Exception e) {
            throw new CloudbreakException("Failed to retrieve the server's certificate", e);
        }
    }
}
