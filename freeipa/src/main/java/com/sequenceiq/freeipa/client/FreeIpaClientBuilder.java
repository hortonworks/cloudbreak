package com.sequenceiq.freeipa.client;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.Security;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpHead;
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
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.ssl.SSLContexts;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import com.google.common.collect.ImmutableMap;
import com.googlecode.jsonrpc4j.JsonRpcClient;
import com.googlecode.jsonrpc4j.JsonRpcClient.RequestListener;
import com.googlecode.jsonrpc4j.JsonRpcHttpClient;
import com.sequenceiq.cloudbreak.client.CertificateTrustManager;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.freeipa.client.auth.InvalidPasswordException;
import com.sequenceiq.freeipa.client.auth.InvalidUserOrRealmException;
import com.sequenceiq.freeipa.client.auth.PasswordExpiredException;

public class FreeIpaClientBuilder {

    public static final String DEFAULT_BASE_PATH = "/ipa";

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaClientBuilder.class);

    private static final int CONNECTION_POOL_MAX = 100;

    private static final int CONNECTION_POOL_MAX_PER_ROOT = 50;

    private static final int SO_TIMEOUT = 30 * 1000;

    private static final int READ_TIMEOUT_MILLIS = 60 * 1000 * 5;

    private static final int TEST_CONNECTION_READ_TIMEOUT_MILLIS = 5 * 1000;

    private final PoolingHttpClientConnectionManager connectionManager;

    private final String basePath;

    private final String user;

    private final String pass;

    private final SSLContext sslContext;

    private final int port;

    private final HttpClientConfig clientConfig;

    private final RequestListener rpcRequestListener;

    private Map<String, String> additionalHeaders;

    public FreeIpaClientBuilder(String user, String pass, HttpClientConfig clientConfig, int port, String basePath,
            Map<String, String> additionalHeaders, JsonRpcClient.RequestListener rpcRequestListener) throws Exception {
        this.user = user;
        this.pass = pass;
        this.clientConfig = clientConfig;
        this.port = port;

        if (clientConfig.hasSSLConfigs()) {
            this.sslContext =
                    setupSSLContext(clientConfig.getClientCert(), clientConfig.getClientKey(), clientConfig.getServerCert());
            RegistryBuilder<ConnectionSocketFactory> registryBuilder = RegistryBuilder.create();
            SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(sslContext, hostnameVerifier());
            registryBuilder.register("https", socketFactory);
            Registry<ConnectionSocketFactory> registry = registryBuilder.build();
            connectionManager = new PoolingHttpClientConnectionManager(registry);
        } else {
            this.sslContext = null;
            connectionManager = new PoolingHttpClientConnectionManager();
        }

        connectionManager.setMaxTotal(CONNECTION_POOL_MAX);
        connectionManager.setDefaultMaxPerRoute(CONNECTION_POOL_MAX_PER_ROOT);

        this.basePath = basePath;
        this.additionalHeaders = Map.copyOf(additionalHeaders);
        this.rpcRequestListener = rpcRequestListener;
    }

    public FreeIpaClientBuilder(String user, String pass, HttpClientConfig clientConfig, int port) throws Exception {
        this(user, pass, clientConfig, port, DEFAULT_BASE_PATH, Map.of(), null);
    }

    public FreeIpaClient build(boolean withPing) throws URISyntaxException, IOException, FreeIpaClientException {
        if (withPing) {
            List<BasicHeader> defaultHeaders = additionalHeaders.entrySet().stream()
                    .map(entry -> new BasicHeader(entry.getKey(), entry.getValue())).collect(Collectors.toList());
            try (CloseableHttpClient client = HttpClientBuilder
                    .create()
                    .useSystemProperties()
                    .setConnectionManager(connectionManager)
                    .setConnectionManagerShared(true)
                    .setDefaultRequestConfig(RequestConfig.custom().setConnectTimeout(TEST_CONNECTION_READ_TIMEOUT_MILLIS).build())
                    .setDefaultHeaders(defaultHeaders)
                    .setDefaultSocketConfig(
                            SocketConfig.custom()
                                    .setSoTimeout(SO_TIMEOUT)
                                    .setTcpNoDelay(true)
                                    .build())
                    .build()) {
                URI target = getIpaUrl(clientConfig.getApiAddress(), port, basePath, "/session/login_password").toURI();
                LOGGER.debug("Ping at target: {}", target);
                client.execute(new HttpHead(target));
                LOGGER.debug("Freeipa is reachable");
            }
        }
        String sessionCookie = connect(user, pass, clientConfig.getApiAddress(), port);

        Map<String, String> headers = ImmutableMap.<String, String>builder()
                .put("Cookie", "ipa_session=" + sessionCookie)
                .putAll(additionalHeaders)
                .build();

        JsonRpcHttpClient jsonRpcHttpClient = new JsonRpcHttpClient(ObjectMapperBuilder.getObjectMapper(),
                getIpaUrl(clientConfig.getApiAddress(), port, basePath, "/session/json"), headers);

        if (sslContext != null) {
            jsonRpcHttpClient.setSslContext(sslContext);
        }

        jsonRpcHttpClient.setHostNameVerifier(hostnameVerifier());
        jsonRpcHttpClient.setReadTimeoutMillis(READ_TIMEOUT_MILLIS);
        jsonRpcHttpClient.setRequestListener(rpcRequestListener);
        return new FreeIpaClient(jsonRpcHttpClient);
    }

    private String connect(String user, String pass, String apiAddress, int port)
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

                throw FreeIpaClientExceptionUtil.convertToRetryableIfNeeded(new FreeIpaClientException(String.format(
                        "Encountered unexpected response from FreeIPA; details:%n%n"
                        + "code: %s%n"
                        + "headers: %s", response.getStatusLine().getStatusCode(), response.getAllHeaders()),
                        response.getStatusLine().getStatusCode()));
            }
        }
        Cookie sessionCookie = cookieStore.getCookies().stream().filter(cookie -> "ipa_session".equalsIgnoreCase(cookie.getName())).findFirst().get();
        return sessionCookie.getValue();
    }

    private SSLContext setupSSLContext(String clientCert, String clientKey, String serverCert) throws Exception {
        Security.addProvider(new BouncyCastleProvider());
        SSLContext context;
        if (StringUtils.isNoneBlank(clientCert, clientKey, serverCert)) {
            context = SSLContexts.custom()
                    .loadTrustMaterial(KeystoreUtils.createTrustStore(serverCert), null)
                    .loadKeyMaterial(KeystoreUtils.createKeyStore(clientCert, clientKey), "consul".toCharArray())
                    .build();
        } else {
            context = CertificateTrustManager.sslContext();
        }
        return context;
    }

    private URL getIpaUrl(String apiAddress, int port, String basePath, String context) throws MalformedURLException {
        String scheme = clientConfig.hasSSLConfigs() ? "https://" : "http://";
        String path = StringUtils.isBlank(basePath) ? "" : basePath;
        path += context;
        return new URL(scheme + apiAddress + ':' + port + path);
    }

    private HttpPost getPost(URI target) {
        HttpPost post = new HttpPost(target);
        post.addHeader("Accept", ContentType.APPLICATION_XML.getMimeType());
        post.addHeader("Content-Type",
                ContentType.APPLICATION_FORM_URLENCODED.getMimeType());
        additionalHeaders.forEach(post::addHeader);
        return post;
    }

    private CloseableHttpResponse execute(HttpPost post, CookieStore cookieStore) throws IOException {
        try (CloseableHttpClient client = HttpClientBuilder
                .create()
                .useSystemProperties()
                .setConnectionManager(connectionManager)
                .setDefaultCookieStore(cookieStore)
                .setConnectionManagerShared(true)
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
