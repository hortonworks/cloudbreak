package com.sequenceiq.cloudbreak.shell.model;

import java.security.cert.CertificateException;
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
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

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
import com.sequenceiq.cloudbreak.shell.configuration.TokenUnavailableException;

public class CloudbreakClient {

    public static final String CLIENT_ID = "cloudbreak_shell";

    private String cloudbreakAddress;
    private String identityServerAddress;
    private String user;
    private String password;
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

    public CloudbreakClient(String cloudbreakAddress, String identityServerAddress, String user, String password) throws Exception {
        this.cloudbreakAddress = cloudbreakAddress;
        this.identityServerAddress = identityServerAddress;
        this.user = user;
        this.password = password;
        String token = getToken(identityServerAddress, user, password);
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
        if (!"".equals(user) && !"".equals(password)) {
            JAXRSClientFactoryBean jaxrsClientFactoryBean = jaxrsClientFactoryBean(token);
            jaxrsClientFactoryBean.setResourceClass(clazz);
            Object clientFactory = JAXRSClientFactory.fromClient(jaxrsClientFactoryBean.create(), clazz, true);
            return (T) clientFactory;
        } else {
            return null;
        }
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

    private String getToken(String identityServerAddress, String user, String password) throws Exception {
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
        SSLContextBuilder sslContextBuilder = new SSLContextBuilder();
        sslContextBuilder.loadTrustMaterial(null, new TrustStrategy() {
            @Override
            public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                return true;
            }
        });
        SSLConnectionSocketFactory sslConnectionSocketFactory = new SSLConnectionSocketFactory(sslContextBuilder.build());
        CloseableHttpClient httpClient = HttpClients.custom().setSSLSocketFactory(sslConnectionSocketFactory).build();
        requestFactory.setHttpClient(httpClient);
        RestTemplate restTemplate = new RestTemplate(requestFactory);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Accept", "application/json");

        MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("credentials", String.format("{\"username\":\"%s\",\"password\":\"%s\"}", user, password));

        String token = null;
        try {
            ResponseEntity<String> authResponse = restTemplate.exchange(
                    String.format("%s/oauth/authorize?response_type=token&client_id=%s", identityServerAddress, CLIENT_ID),
                    HttpMethod.POST,
                    new HttpEntity<Map>(requestBody, headers),
                    String.class
            );
            if (HttpStatus.FOUND == authResponse.getStatusCode() && authResponse.getHeaders().get("Location") != null) {
                token = parseTokenFromLocationHeader(authResponse);
            } else {
                System.out.println("Couldn't get an access token from the identity server, check its configuration! "
                        + "Perhaps cloudbreak_shell is not autoapproved?");
                System.out.println("Response from identity server: ");
                System.out.println("Headers: " + authResponse);
                throw new TokenUnavailableException("Wrong response from identity server.");
            }
        } catch (ResourceAccessException e) {
            System.out.println("Error occurred while trying to connect to identity server: " + e.getMessage());
            System.out.println("Check if your identity server is available and accepting requests on " + identityServerAddress);
            throw new TokenUnavailableException("Error occurred while getting token from identity server", e);
        } catch (HttpClientErrorException e) {
            if (HttpStatus.UNAUTHORIZED == e.getStatusCode()) {
                System.out.println("Error occurred while getting token from identity server: " + identityServerAddress);
                System.out.println("Check your username and password.");
            } else {
                System.out.println("Something unexpected happened, couldn't get token from identity server. Please check your configurations.");
            }
            throw new TokenUnavailableException("Error occurred while getting token from identity server", e);
        } catch (Exception e) {
            System.out.println("Something unexpected happened, couldn't get token from identity server. Please check your configurations.");
            throw new TokenUnavailableException("Error occurred while getting token from identity server", e);
        }
        return token;
    }

    private String parseTokenFromLocationHeader(ResponseEntity<String> authResponse) {
        String location = authResponse.getHeaders().get("Location").get(0);
        String[] parts = location.split("#");
        String[] parameters = parts[1].split("&|=");
        for (int i = 0; i < parameters.length; i++) {
            String param = parameters[i];
            int nextIndex = i + 1;
            if ("access_token".equals(param) && !(nextIndex > parameters.length)) {
                return parameters[nextIndex];
            }
        }
        throw new TokenUnavailableException("Token could not be found in the 'Location' header.");
    }

}
