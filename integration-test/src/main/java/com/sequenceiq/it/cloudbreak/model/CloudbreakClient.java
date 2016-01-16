package com.sequenceiq.it.cloudbreak.model;

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

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.sequenceiq.cloudbreak.api.CoreApi;
import com.sequenceiq.cloudbreak.api.endpoint.AccountPreferencesEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.BlueprintEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.ClusterEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.ConnectorEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.CredentialEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.EventEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.NetworkEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.RecipeEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.SecurityGroupEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.StackEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.SubscriptionEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.TemplateEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.UsageEndpoint;
import com.sequenceiq.cloudbreak.api.endpoint.UserEndpoint;

public class CloudbreakClient {

    private String cloudbreakAddress;
    private String token;
    private CredentialEndpoint credentialEndpoint;
    private TemplateEndpoint templateEndpoint;
    private UsageEndpoint usageEndpoint;
    private UserEndpoint userEndpoint;
    private EventEndpoint eventEndpoint;
    private SecurityGroupEndpoint securityGroupEndpoint;
    private StackEndpoint stackEndpoint;
    private SubscriptionEndpoint subscriptionEndpoint;
    private NetworkEndpoint networkEndpoint;
    private RecipeEndpoint recipeEndpoint;
    private AccountPreferencesEndpoint accountPreferencesEndpoint;
    private BlueprintEndpoint blueprintEndpoint;
    private ClusterEndpoint clusterEndpoint;
    private ConnectorEndpoint connectorEndpoint;

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

    public CloudbreakClient(String cloudbreakAddress, String token) throws Exception {
        this.cloudbreakAddress = cloudbreakAddress;
        this.token = token;
        this.credentialEndpoint = endPointFactory(CredentialEndpoint.class, token);
        this.templateEndpoint = endPointFactory(TemplateEndpoint.class, token);
        this.usageEndpoint = endPointFactory(UsageEndpoint.class, token);
        this.eventEndpoint = endPointFactory(EventEndpoint.class, token);
        this.securityGroupEndpoint = endPointFactory(SecurityGroupEndpoint.class, token);
        this.stackEndpoint = endPointFactory(StackEndpoint.class, token);
        this.subscriptionEndpoint = endPointFactory(SubscriptionEndpoint.class, token);
        this.networkEndpoint = endPointFactory(NetworkEndpoint.class, token);
        this.recipeEndpoint = endPointFactory(RecipeEndpoint.class, token);
        this.accountPreferencesEndpoint = endPointFactory(AccountPreferencesEndpoint.class, token);
        this.blueprintEndpoint = endPointFactory(BlueprintEndpoint.class, token);
        this.clusterEndpoint = endPointFactory(ClusterEndpoint.class, token);
        this.connectorEndpoint = endPointFactory(ConnectorEndpoint.class, token);
        this.userEndpoint = endPointFactory(UserEndpoint.class, token);
    }

    public CredentialEndpoint credentialEndpoint() throws Exception {
        HTTPConduit httpConduit = WebClient.getConfig(credentialEndpoint).getHttpConduit();
        TLSClientParameters tcp = new TLSClientParameters();
        tcp.setTrustManagers(trustAllCerts);
        httpConduit.setTlsClientParameters(tcp);

        return credentialEndpoint;
    }

    public TemplateEndpoint templateEndpoint() throws Exception {
        HTTPConduit httpConduit = WebClient.getConfig(templateEndpoint).getHttpConduit();
        TLSClientParameters tcp = new TLSClientParameters();
        tcp.setTrustManagers(trustAllCerts);
        httpConduit.setTlsClientParameters(tcp);

        return templateEndpoint;
    }

    public UsageEndpoint usageEndpoint() throws Exception {
        HTTPConduit httpConduit = WebClient.getConfig(usageEndpoint).getHttpConduit();
        TLSClientParameters tcp = new TLSClientParameters();
        tcp.setTrustManagers(trustAllCerts);
        httpConduit.setTlsClientParameters(tcp);

        return usageEndpoint;
    }

    public UserEndpoint userEndpoint() throws Exception {
        HTTPConduit httpConduit = WebClient.getConfig(userEndpoint).getHttpConduit();
        TLSClientParameters tcp = new TLSClientParameters();
        tcp.setTrustManagers(trustAllCerts);
        httpConduit.setTlsClientParameters(tcp);

        return userEndpoint;
    }

    public EventEndpoint eventEndpoint() throws Exception {
        HTTPConduit httpConduit = WebClient.getConfig(eventEndpoint).getHttpConduit();
        TLSClientParameters tcp = new TLSClientParameters();
        tcp.setTrustManagers(trustAllCerts);
        httpConduit.setTlsClientParameters(tcp);

        return eventEndpoint;
    }

    public SecurityGroupEndpoint securityGroupEndpoint() throws Exception {
        HTTPConduit httpConduit = WebClient.getConfig(securityGroupEndpoint).getHttpConduit();
        TLSClientParameters tcp = new TLSClientParameters();
        tcp.setTrustManagers(trustAllCerts);
        httpConduit.setTlsClientParameters(tcp);

        return securityGroupEndpoint;
    }

    public StackEndpoint stackEndpoint() throws Exception {
        HTTPConduit httpConduit = WebClient.getConfig(stackEndpoint).getHttpConduit();
        TLSClientParameters tcp = new TLSClientParameters();
        tcp.setTrustManagers(trustAllCerts);
        httpConduit.setTlsClientParameters(tcp);

        return stackEndpoint;
    }

    public SubscriptionEndpoint subscriptionEndpoint() throws Exception {
        HTTPConduit httpConduit = WebClient.getConfig(subscriptionEndpoint).getHttpConduit();
        TLSClientParameters tcp = new TLSClientParameters();
        tcp.setTrustManagers(trustAllCerts);
        httpConduit.setTlsClientParameters(tcp);

        return subscriptionEndpoint;
    }

    public NetworkEndpoint networkEndpoint() throws Exception {
        HTTPConduit httpConduit = WebClient.getConfig(networkEndpoint).getHttpConduit();
        TLSClientParameters tcp = new TLSClientParameters();
        tcp.setTrustManagers(trustAllCerts);
        httpConduit.setTlsClientParameters(tcp);

        return networkEndpoint;
    }

    public RecipeEndpoint recipeEndpoint() throws Exception {
        HTTPConduit httpConduit = WebClient.getConfig(recipeEndpoint).getHttpConduit();
        TLSClientParameters tcp = new TLSClientParameters();
        tcp.setTrustManagers(trustAllCerts);
        httpConduit.setTlsClientParameters(tcp);

        return recipeEndpoint;
    }

    public AccountPreferencesEndpoint accountPreferencesEndpoint() throws Exception {
        HTTPConduit httpConduit = WebClient.getConfig(accountPreferencesEndpoint).getHttpConduit();
        TLSClientParameters tcp = new TLSClientParameters();
        tcp.setTrustManagers(trustAllCerts);
        httpConduit.setTlsClientParameters(tcp);

        return accountPreferencesEndpoint;
    }

    public BlueprintEndpoint blueprintEndpoint() throws Exception {
        HTTPConduit httpConduit = WebClient.getConfig(blueprintEndpoint).getHttpConduit();
        TLSClientParameters tcp = new TLSClientParameters();
        tcp.setTrustManagers(trustAllCerts);
        httpConduit.setTlsClientParameters(tcp);
        return blueprintEndpoint;
    }

    public ClusterEndpoint clusterEndpoint() throws Exception {
        HTTPConduit httpConduit = WebClient.getConfig(clusterEndpoint).getHttpConduit();
        TLSClientParameters tcp = new TLSClientParameters();
        tcp.setTrustManagers(trustAllCerts);
        httpConduit.setTlsClientParameters(tcp);

        return clusterEndpoint;
    }

    public ConnectorEndpoint connectorEndpoint() throws Exception {
        HTTPConduit httpConduit = WebClient.getConfig(connectorEndpoint).getHttpConduit();
        TLSClientParameters tcp = new TLSClientParameters();
        tcp.setTrustManagers(trustAllCerts);
        httpConduit.setTlsClientParameters(tcp);

        return connectorEndpoint;
    }

    private <T extends Object> T endPointFactory(Class<T> clazz, String token) throws Exception {
        JAXRSClientFactoryBean jaxrsClientFactoryBean = jaxrsClientFactoryBean(token);
        jaxrsClientFactoryBean.setResourceClass(clazz);
        Object clientFactory = JAXRSClientFactory.fromClient(jaxrsClientFactoryBean.create(), clazz, true);
        return (T) clientFactory;
    }

    private JAXRSClientFactoryBean jaxrsClientFactoryBean(String token) {
        JAXRSClientFactoryBean jaxrsClientFactoryBean = new JAXRSClientFactoryBean();
        String addressWithoutLastSlash = cloudbreakAddress.endsWith("/") ? cloudbreakAddress.substring(0, cloudbreakAddress.length() - 1) : cloudbreakAddress;
        String apiAddress = addressWithoutLastSlash + CoreApi.API_ROOT_CONTEXT;
        jaxrsClientFactoryBean.setAddress(apiAddress);
        jaxrsClientFactoryBean.setProvider(JacksonJsonProvider.class);
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + token);
        jaxrsClientFactoryBean.setHeaders(headers);
        return jaxrsClientFactoryBean;
    }

}
