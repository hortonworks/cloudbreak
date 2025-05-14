package com.sequenceiq.cloudbreak.service.stack.flow;

import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import jakarta.inject.Inject;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.WebTarget;

import org.glassfish.jersey.SslConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.io.BaseEncoding;
import com.sequenceiq.cloudbreak.certificate.PkiUtil;
import com.sequenceiq.cloudbreak.client.CertificateTrustManager.SavingX509TrustManager;
import com.sequenceiq.cloudbreak.client.RestClientUtil;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.polling.PollingService;
import com.sequenceiq.cloudbreak.polling.nginx.NginxCertListenerTask;
import com.sequenceiq.cloudbreak.polling.nginx.NginxPollerObject;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;

@Component
public class TlsSetupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TlsSetupService.class);

    private static final int POLLING_INTERVAL = 5000;

    private static final long TEN_MIN = 10L * 60L;

    private static final int MAX_FAILURE = 1;

    @Inject
    private PollingService<NginxPollerObject> nginxPollerService;

    @Inject
    private NginxCertListenerTask nginxCertListenerTask;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    public void setupTls(StackDtoDelegate stack, InstanceMetadataView gwInstance) throws CloudbreakException {
        try {
            SavingX509TrustManager x509TrustManager = new SavingX509TrustManager();
            TrustManager[] trustManagers = {x509TrustManager};
            SSLContext sslContext = SslConfigurator.newInstance().createSSLContext();
            sslContext.init(null, trustManagers, new SecureRandom());
            Client client = RestClientUtil.createClient(sslContext, false);
            Integer gatewayPort = stack.getGatewayPort();
            String ip = gatewayConfigService.getGatewayIp(stack.getSecurityConfig(), gwInstance);
            LOGGER.debug("Trying to fetch the server's certificate: {}:{}", ip, gatewayPort);
            nginxPollerService.pollWithAbsoluteTimeout(
                    nginxCertListenerTask, new NginxPollerObject(client, ip, gatewayPort, x509TrustManager),
                    POLLING_INTERVAL, TEN_MIN, MAX_FAILURE);
            WebTarget nginxTarget = client.target(String.format("https://%s:%d", ip, gatewayPort));
            nginxTarget.path("/").request().get().close();
            X509Certificate[] chain = x509TrustManager.getChain();
            String serverCert = PkiUtil.convert(chain[0]);
            String serverCertBase64 = BaseEncoding.base64().encode(serverCert.getBytes());
            instanceMetaDataService.updateServerCert(gwInstance.getId(), serverCertBase64);
        } catch (Exception e) {
            throw new CloudbreakException("Failed to retrieve the server's certificate from Nginx."
                    + " Please check your security group is open enough and the Management Console can access your VPC and subnet."
                    + " Please also Make sure your Subnets can route to the internet and you have public DNS and IP options enabled."
                    + " Refer to Cloudera documentation at"
                    + " https://docs.cloudera.com/management-console/cloud/proxy/topics/mc-outbound-internet-access-and-proxy.html", e);
        }
    }
}
