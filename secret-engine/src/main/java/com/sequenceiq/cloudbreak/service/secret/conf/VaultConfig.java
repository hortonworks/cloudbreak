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

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.impl.conn.DefaultSchemePortResolver;
import org.apache.http.impl.conn.SystemDefaultRoutePlanner;
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

    @Value("${vault.addr:}")
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

            if (isNoneEmpty(httpsProxyUser) && isNoneEmpty(httpsProxyPassword)) {
                UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(httpsProxyUser, httpsProxyPassword);
                CredentialsProvider provider = new BasicCredentialsProvider();
                provider.setCredentials(AuthScope.ANY, credentials);
                httpClientBuilder.setDefaultCredentialsProvider(provider);
            }

            if (hasSslConfiguration(sslConfiguration)) {
                SSLContext sslContext = getSSLContext(sslConfiguration,
                        getTrustManagers(sslConfiguration));
                SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(
                        sslContext);
                httpClientBuilder.setSSLSocketFactory(sslSocketFactory);
                httpClientBuilder.setSSLContext(sslContext);
            }

            RequestConfig requestConfig = RequestConfig
                    .custom()
                    .setConnectTimeout(Math.toIntExact(options.getConnectionTimeout().toMillis()))
                    .setSocketTimeout(Math.toIntExact(options.getReadTimeout().toMillis()))
                    .setAuthenticationEnabled(true)
                    .build();

            httpClientBuilder.setDefaultRequestConfig(requestConfig);

            httpClientBuilder.setRedirectStrategy(new LaxRedirectStrategy());
            return new HttpComponentsClientHttpRequestFactory(httpClientBuilder.build());
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
