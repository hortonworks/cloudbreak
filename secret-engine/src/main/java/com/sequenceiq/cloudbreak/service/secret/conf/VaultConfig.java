package com.sequenceiq.cloudbreak.service.secret.conf;

import static org.apache.commons.lang3.StringUtils.isNoneEmpty;

import java.io.IOException;
import java.io.InputStream;
import java.net.ProxySelector;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import jakarta.inject.Inject;

import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.CredentialsStore;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.DefaultSchemePortResolver;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.impl.routing.SystemDefaultRoutePlanner;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.http.ssl.TLS;
import org.apache.hc.core5.pool.PoolConcurrencyPolicy;
import org.apache.hc.core5.pool.PoolReusePolicy;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.util.StringUtils;
import org.springframework.vault.authentication.ClientAuthentication;
import org.springframework.vault.authentication.KubernetesAuthentication;
import org.springframework.vault.authentication.KubernetesAuthenticationOptions;
import org.springframework.vault.authentication.TokenAuthentication;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.config.AbstractVaultConfiguration;
import org.springframework.vault.support.ClientOptions;
import org.springframework.vault.support.SslConfiguration;
import org.springframework.vault.support.SslConfiguration.KeyStoreConfiguration;

import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@Configuration
@ConditionalOnProperty(name = "vault.config.enabled", havingValue = "true")
public class VaultConfig extends AbstractVaultConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(VaultConfig.class);

    private static final char[] EMPTY_ARRAY = new char[0];

    private static final String AUTH_TYPE_K8S = "kubernetes";

    private static final AuthScope AUTH_SCOPE_ANY = new AuthScope(null, -1);

    @Value("${vault.addr:localhost}")
    private String address;

    @Value("${vault.port:}")
    private int port;

    @Value("${vault.root.token:}")
    private String rootToken;

    @Value("${vault.ssl.enabled:false}")
    private Boolean sslEnabled;

    @Value("${vault.auth.type:}")
    private String authType;

    @Value("${vault.auth.kubernetes.mount.path:}")
    private String kubernetesMountPath;

    @Value("${vault.auth.kubernetes.login.role:}")
    private String kubernetesLoginRole;

    @Value("${vault.auth.kubernetes.service.account.token.path:}")
    private String kubernetesSATokenPath;

    @Value("${https.proxyUser:}")
    private String httpsProxyUser;

    @Value("${https.proxyPassword:}")
    private String httpsProxyPassword;

    @Inject
    private LaxRedirectStrategy laxRedirectStrategy;

    @Override
    public VaultEndpoint vaultEndpoint() {
        VaultEndpoint endpoint = VaultEndpoint.create(address, port);
        endpoint.setScheme(sslEnabled ? "https" : "http");
        return endpoint;
    }

    @Override
    public ClientAuthentication clientAuthentication() {
        if (AUTH_TYPE_K8S.equalsIgnoreCase(authType)) {
            LOGGER.info("Kubernetes based Vault auth is configured");
            try {
                String token = FileReaderUtils.readFileFromPath(Paths.get(kubernetesSATokenPath));
                KubernetesAuthenticationOptions k8sOptions = KubernetesAuthenticationOptions.builder()
                        .jwtSupplier(() -> token)
                        .role(kubernetesLoginRole)
                        .path(kubernetesMountPath)
                        .build();
                return new KubernetesAuthentication(k8sOptions, restOperations());
            } catch (IOException e) {
                throw new RuntimeException("Failed to read the Kubernetes service account token", e);
            }
        } else {
            LOGGER.info("Token based Vault auth is configured");
            return new TokenAuthentication(rootToken);
        }
    }

    @Override
    public ClientFactoryWrapper clientHttpRequestFactoryWrapper() {
        try {
            return new ClientFactoryWrapper(new HttpComponents().usingHttpComponents(clientOptions(), sslConfiguration()));
        } catch (Exception e) {
            throw new RuntimeException("Failed to create client factory for Vault connection", e);
        }
    }

    private class HttpComponents {

        private ClientHttpRequestFactory usingHttpComponents(ClientOptions options, SslConfiguration sslConfiguration)
                throws GeneralSecurityException, IOException {
            HttpClientBuilder httpClientBuilder = HttpClients.custom();

            httpClientBuilder.setRoutePlanner(new SystemDefaultRoutePlanner(
                    DefaultSchemePortResolver.INSTANCE, ProxySelector.getDefault()));

            if (isNoneEmpty(httpsProxyUser, httpsProxyPassword)) {
                UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(httpsProxyUser, httpsProxyPassword.toCharArray());
                CredentialsStore provider = new BasicCredentialsProvider();
                provider.setCredentials(AUTH_SCOPE_ANY, credentials);
                httpClientBuilder.setDefaultCredentialsProvider(provider);
            }

            RequestConfig requestConfig = RequestConfig
                    .custom()
                    .setAuthenticationEnabled(true)
                    .build();

            httpClientBuilder.setDefaultRequestConfig(requestConfig);
            httpClientBuilder.setConnectionManager(getConnectionManager(options, sslConfiguration));
            httpClientBuilder.setRedirectStrategy(laxRedirectStrategy);
            return new HttpComponentsClientHttpRequestFactory(httpClientBuilder.build());
        }

        private HttpClientConnectionManager getConnectionManager(ClientOptions options, SslConfiguration sslConfiguration)
                throws GeneralSecurityException, IOException {
            PoolingHttpClientConnectionManagerBuilder connectionManagerBuilder = PoolingHttpClientConnectionManagerBuilder.create();
            if (hasSslConfiguration(sslConfiguration)) {
                SSLContext sslContext = getSSLContext(sslConfiguration,
                        getTrustManagers(sslConfiguration));
                connectionManagerBuilder.setSSLSocketFactory(SSLConnectionSocketFactoryBuilder.create()
                        .setSslContext(sslContext)
                        .setTlsVersions(TLS.V_1_3)
                        .build()
                );
            }
            ConnectionConfig connectionConfig = ConnectionConfig.custom()
                    .setConnectTimeout(Timeout.ofMilliseconds(Math.toIntExact(options.getConnectionTimeout().toMillis())))
                    .setSocketTimeout(Timeout.ofMilliseconds(Math.toIntExact(options.getReadTimeout().toMillis())))
                    .build();
            return connectionManagerBuilder
                    .setDefaultSocketConfig(SocketConfig.custom()
                            .setSoTimeout(Timeout.ofMinutes(1))
                            .build())
                    .setPoolConcurrencyPolicy(PoolConcurrencyPolicy.STRICT)
                    .setConnPoolPolicy(PoolReusePolicy.LIFO)
                    .setDefaultConnectionConfig(connectionConfig)
                    .build();
        }

        private boolean hasSslConfiguration(SslConfiguration sslConfiguration) {
            return sslConfiguration.getTrustStoreConfiguration().isPresent() || sslConfiguration.getKeyStoreConfiguration().isPresent();
        }

        SSLContext getSSLContext(SslConfiguration sslConfiguration, TrustManager[] trustManagers) throws GeneralSecurityException, IOException {
            KeyManager[] keyManagers = sslConfiguration.getKeyStoreConfiguration()
                    .isPresent() ? createKeyManagerFactory(sslConfiguration.getKeyStoreConfiguration()).getKeyManagers() : null;

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagers, trustManagers, null);

            return sslContext;
        }

        private TrustManager[] getTrustManagers(SslConfiguration sslConfiguration) throws GeneralSecurityException, IOException {
            return sslConfiguration.getTrustStoreConfiguration().isPresent() ? createTrustManagerFactory(
                    sslConfiguration.getTrustStoreConfiguration()).getTrustManagers() : null;
        }

        private KeyManagerFactory createKeyManagerFactory(KeyStoreConfiguration keyStoreConfiguration) throws GeneralSecurityException, IOException {
            KeyStore keyStore = KeyStore.getInstance(StringUtils
                    .hasText(keyStoreConfiguration.getStoreType()) ? keyStoreConfiguration.getStoreType() : KeyStore.getDefaultType());

            loadKeyStore(keyStoreConfiguration, keyStore);

            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, keyStoreConfiguration.getStorePassword() == null ? EMPTY_ARRAY : keyStoreConfiguration.getStorePassword());
            return keyManagerFactory;
        }

        private TrustManagerFactory createTrustManagerFactory(KeyStoreConfiguration keyStoreConfiguration) throws GeneralSecurityException, IOException {
            KeyStore trustStore = KeyStore.getInstance(StringUtils
                    .hasText(keyStoreConfiguration.getStoreType()) ? keyStoreConfiguration.getStoreType() : KeyStore.getDefaultType());

            loadKeyStore(keyStoreConfiguration, trustStore);

            TrustManagerFactory trustManagerFactory = TrustManagerFactory
                    .getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(trustStore);

            return trustManagerFactory;
        }

        private void loadKeyStore(KeyStoreConfiguration keyStoreConfiguration, KeyStore keyStore)
                throws IOException, NoSuchAlgorithmException, CertificateException {
            try (InputStream inputStream = keyStoreConfiguration.getResource().getInputStream()) {
                keyStore.load(inputStream, keyStoreConfiguration.getStorePassword());
            }
        }
    }
}
