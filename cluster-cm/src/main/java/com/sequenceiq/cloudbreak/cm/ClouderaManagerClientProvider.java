package com.sequenceiq.cloudbreak.cm;

import javax.net.ssl.SSLContext;

import org.apache.http.ssl.SSLContexts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.Configuration;
import com.sequenceiq.cloudbreak.client.CertificateTrustManager;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.client.KeyStoreUtil;
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;

@Service
public class ClouderaManagerClientProvider {

    public static final String API_V_30 = "/api/v30";

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerClientProvider.class);

    public ApiClient getClouderaManagerClient(HttpClientConfig clientConfig, Integer port, String userName, String password) {
        ApiClient cmClient = Configuration.getDefaultApiClient();
        if (port != null) {
            cmClient.setBasePath("https://" + clientConfig.getApiAddress() + ":" + port + API_V_30);
        } else {
            cmClient.setBasePath("https://" + clientConfig.getApiAddress() + API_V_30);
        }
        cmClient.setUsername(userName);
        cmClient.setPassword(password);
        cmClient.setVerifyingSsl(true);

        try {
            SSLContext sslContext = SSLContexts.custom()
                    .loadTrustMaterial(KeyStoreUtil.createTrustStore(clientConfig.getServerCert()), null)
                    .loadKeyMaterial(KeyStoreUtil.createKeyStore(clientConfig.getClientCert(), clientConfig.getClientKey()), "consul".toCharArray())
                    .build();
            cmClient.getHttpClient().setSslSocketFactory(sslContext.getSocketFactory());
            cmClient.getHttpClient().setHostnameVerifier(CertificateTrustManager.hostnameVerifier());
            return cmClient;
        } catch (Exception e) {
            LOGGER.info("Can not create SSL context for cloudera manager", e);
            throw new CloudbreakServiceException(e);
        }
    }
}
