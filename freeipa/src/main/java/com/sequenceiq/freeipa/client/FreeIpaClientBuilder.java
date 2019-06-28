package com.sequenceiq.freeipa.client;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.Security;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.client.CookieStore;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.ssl.SSLContexts;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import com.googlecode.jsonrpc4j.JsonRpcHttpClient;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.freeipa.client.auth.InvalidPasswordException;
import com.sequenceiq.freeipa.client.auth.InvalidUserOrRealmException;
import com.sequenceiq.freeipa.client.auth.PasswordExpiredException;

public class FreeIpaClientBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaClientBuilder.class);

    private static final int CONNECTION_POOL_MAX = 100;

    private static final int CONNECTION_POOL_MAX_PER_ROOT = 50;

    private static final int SO_TIMEOUT = 30 * 1000;

    private final PoolingHttpClientConnectionManager connectionManager;

    private String basePath = "/ipa";

    private final String user;

    private final String pass;

    private final String realm;

    private final SSLContext sslContext;

    private final String port;

    private final HttpClientConfig clientConfig;

    public FreeIpaClientBuilder(String user, String pass, String realm, HttpClientConfig clientConfig, String port) throws Exception {
        this.user = user;
        this.pass = pass;
        this.realm = realm;
        this.clientConfig = clientConfig;
        this.port = port;
        sslContext = setupSSLContext(clientConfig.getClientCert(), clientConfig.getClientKey(), clientConfig.getServerCert());
        RegistryBuilder<ConnectionSocketFactory> registryBuilder = RegistryBuilder.create();
        SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(sslContext, hostnameVerifier());
        registryBuilder.register("https", socketFactory);
        Registry<ConnectionSocketFactory> registry = registryBuilder.build();
        connectionManager = new PoolingHttpClientConnectionManager(registry);
        connectionManager.setMaxTotal(CONNECTION_POOL_MAX);
        connectionManager.setDefaultMaxPerRoute(CONNECTION_POOL_MAX_PER_ROOT);
    }

    public FreeIpaClientBuilder(String user, String pass, String realm, HttpClientConfig clientConfig, String port, String basePath) throws Exception {
        this(user, pass, realm, clientConfig, port);
        this.basePath = basePath;
    }

    public FreeIpaClient build() throws URISyntaxException, FreeIpaClientException, IOException {
        String sessionCookie = connect(user, pass, sslContext, clientConfig.getApiAddress(), port);
        JsonRpcHttpClient jsonRpcHttpClient = new JsonRpcHttpClient(ObjectMapperBuilder.getObjectMapper(),
                getIpaUrl(clientConfig.getApiAddress(), port, basePath, "/session/json"),
                Map.of("Cookie", "ipa_session=" + sessionCookie));
        jsonRpcHttpClient.setSslContext(sslContext);
        jsonRpcHttpClient.setHostNameVerifier(hostnameVerifier());
        return new FreeIpaClient(jsonRpcHttpClient);
    }

    private String connect(String user, String pass, SSLContext sslContext, String apiAddress, String port)
            throws IOException, URISyntaxException, FreeIpaClientException {

        URI target = getIpaUrl(apiAddress, port, basePath, "/session/login_password").toURI();
        LOGGER.debug("Connecting for user: {} at target: {}", user, target);

        HttpPost post = getPost(target);
        post.setEntity(new UrlEncodedFormEntity(List.of(new BasicNameValuePair("user", user), new BasicNameValuePair("password", pass))));

        CookieStore cookieStore = new BasicCookieStore();
        try (CloseableHttpResponse response = execute(post, cookieStore)) {
            if (response.getStatusLine().getStatusCode() != HttpStatus.OK.value()) {
                if (response.getStatusLine().getStatusCode() == HttpStatus.UNAUTHORIZED.value()) {

                    Header[] reasons = response.getHeaders("X-IPA-Rejection-Reason");

                    if (reasons.length > 0) {
                        if ("password-expired".equalsIgnoreCase(reasons[0].getValue())) {
                            throw new PasswordExpiredException();
                        }

                        if ("invalid-password".equalsIgnoreCase(reasons[0].getValue())) {
                            throw new InvalidPasswordException();
                        }

                        if ("denied".equalsIgnoreCase(reasons[0].getValue())) {
                            throw new InvalidUserOrRealmException();
                        }
                    }
                }

                throw new FreeIpaClientException(String.format("Encountered unexpected response from "
                        + "FreeIPA; details:%n%n"
                        + "code: %s%n"
                        + "headers: %s", response.getStatusLine().getStatusCode(), response.getAllHeaders()));
            }
        }
        Cookie sessionCookie = cookieStore.getCookies().stream().filter(cookie -> "ipa_session".equalsIgnoreCase(cookie.getName())).findFirst().get();
        return sessionCookie.getValue();
    }

    private String getUser(String user, String realm) {
        if (StringUtils.isBlank(realm) && !user.contains("@")) {
            throw new IllegalArgumentException("Realm is required to open a "
                    + "connection to the FreeIPA instance");
        }
        return user.contains("@") ? user : user + '@' + realm;
    }

    private SSLContext setupSSLContext(String clientCert, String clientKey, String serverCert) throws Exception {
        Security.addProvider(new BouncyCastleProvider());
        SSLContext context = SSLContexts.custom()
                .loadTrustMaterial(KeystoreUtils.createTrustStore(serverCert), null)
                .loadKeyMaterial(KeystoreUtils.createKeyStore(clientCert, clientKey), "consul".toCharArray())
                .build();
        return context;
    }

    private URL getIpaUrl(String apiAddress, String port, String basePath, String context) throws MalformedURLException {
        String path = StringUtils.isBlank(basePath) ? "" : basePath;
        path += context;
        return new URL("https://" + apiAddress + ':' + port + path);
    }

    private HttpPost getPost(URI target) {
        HttpPost post = new HttpPost(target);
        post.addHeader("Accept", ContentType.APPLICATION_XML.getMimeType());
        post.addHeader("Content-Type",
                ContentType.APPLICATION_FORM_URLENCODED.getMimeType());
        return post;
    }

    private CloseableHttpResponse execute(HttpPost post, CookieStore cookieStore) throws IOException {
        try (CloseableHttpClient client = HttpClientBuilder
                .create()
                .useSystemProperties()
                .setConnectionManager(connectionManager)
                .setDefaultCookieStore(cookieStore)
                .setDefaultSocketConfig(
                        SocketConfig.custom()
                                .setSoTimeout(SO_TIMEOUT)
                                .setTcpNoDelay(true)
                                .build())
                .build()) {
            CloseableHttpResponse response = client.execute(post);
            LOGGER.debug("Post response:\n"
                    + "code: {}\n"
                    + "headers: {}", response.getStatusLine().getStatusCode(), response.getAllHeaders());

            return response;
        }
    }

    private static HostnameVerifier hostnameVerifier() {
        return (s, sslSession) -> true;
    }
}
