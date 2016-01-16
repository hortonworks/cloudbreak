package com.sequenceiq.periscope.model;

import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.transport.http.HTTPConduit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.sequenceiq.cloudbreak.api.CoreApi;
import com.sequenceiq.cloudbreak.api.endpoint.ClusterEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.StackEndpoint;
import com.sequenceiq.periscope.service.security.TokenService;


@Service
public class CloudbreakClient {

    @Autowired
    private TokenService tokenService;

    @Autowired
    @Qualifier("cloudbreakUrl")
    private String cloudbreakUrl;

    private TrustManager[] trustAllCerts = new TrustManager[]{
            new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                }

                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                }
            }
    };

    public StackEndpoint stackEndpoint() throws Exception {
        StackEndpoint stackEndpoint = endPointFactory(StackEndpoint.class, tokenService.getToken());
        HTTPConduit httpConduit = WebClient.getConfig(stackEndpoint).getHttpConduit();
        TLSClientParameters tcp = new TLSClientParameters();
        tcp.setTrustManagers(trustAllCerts);
        httpConduit.setTlsClientParameters(tcp);

        return stackEndpoint;
    }

    public ClusterEndpoint clusterEndpoint() throws Exception {
        ClusterEndpoint clusterEndpoint = endPointFactory(ClusterEndpoint.class, tokenService.getToken());
        HTTPConduit httpConduit = WebClient.getConfig(clusterEndpoint).getHttpConduit();
        TLSClientParameters tcp = new TLSClientParameters();
        tcp.setTrustManagers(trustAllCerts);
        httpConduit.setTlsClientParameters(tcp);

        return clusterEndpoint;
    }

    private <T extends Object> T endPointFactory(Class<T> clazz, String token) throws Exception {
        JAXRSClientFactoryBean jaxrsClientFactoryBean = jaxrsClientFactoryBean(token);
        jaxrsClientFactoryBean.setResourceClass(clazz);
        Object clientFactory = JAXRSClientFactory.fromClient(jaxrsClientFactoryBean.create(), clazz, true);
        return (T) clientFactory;
    }

    private JAXRSClientFactoryBean jaxrsClientFactoryBean(String token) {
        JAXRSClientFactoryBean jaxrsClientFactoryBean = new JAXRSClientFactoryBean();
        String addressWithoutLastSlash = cloudbreakUrl.endsWith("/") ? cloudbreakUrl.substring(0, cloudbreakUrl.length() - 1) : cloudbreakUrl;
        String apiAddress = addressWithoutLastSlash + CoreApi.API_ROOT_CONTEXT;
        jaxrsClientFactoryBean.setAddress(apiAddress);
        jaxrsClientFactoryBean.setProvider(JacksonJsonProvider.class);
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + token);
        jaxrsClientFactoryBean.setHeaders(headers);
        return jaxrsClientFactoryBean;
    }

}
