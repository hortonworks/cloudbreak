package com.sequenceiq.cloudbreak.cloud.azure.client;

import static com.sequenceiq.common.api.encryptionprofile.TlsVersion.TLS_1_3;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.azure.core.client.traits.HttpTrait;
import com.azure.core.http.HttpClient;
import com.azure.core.http.okhttp.OkHttpAsyncHttpClientBuilder;
import com.azure.core.http.policy.HttpLogDetailLevel;
import com.azure.core.http.policy.HttpLogOptions;
import com.azure.resourcemanager.marketplaceordering.MarketplaceOrderingManager;
import com.azure.resourcemanager.postgresql.PostgreSqlManager;
import com.azure.resourcemanager.resources.fluentcore.arm.AzureConfigurable;
import com.sequenceiq.cloudbreak.tls.EncryptionProfileProvider;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.okhttp3.OkHttpMetricsEventListener;
import okhttp3.CipherSuite;
import okhttp3.ConnectionSpec;
import okhttp3.Dispatcher;
import okhttp3.JavaNetAuthenticator;
import okhttp3.OkHttpClient;
import okhttp3.TlsVersion;

@Component
public class AzureHttpClientConfigurer {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureHttpClientConfigurer.class);

    private static final String AZURE_METRIC_NAME_PREFIX = "azure";

    private static final String CIPHER_SUITE_SEPARATOR = ":";

    private final HttpLogDetailLevel logLevel;

    private final ExecutorService mdcCopyingThreadPoolExecutor;

    private final boolean tlsHardeningEnabled;

    private final EncryptionProfileProvider encryptionProfileProvider;

    private final OkHttpClient okHttpClient;

    @Inject
    public AzureHttpClientConfigurer(
            @Value("${cb.azure.loglevel:BASIC}") HttpLogDetailLevel logLevel,
            @Value("${cb.azure.tlsHardening:false}") boolean tlsHardeningEnabled,
            @Qualifier("azureClientThreadPool") ExecutorService mdcCopyingThreadPoolExecutor,
            MeterRegistry meterRegistry,
            EncryptionProfileProvider encryptionProfileProvider) {
        this.logLevel = logLevel;
        this.tlsHardeningEnabled = tlsHardeningEnabled;
        this.mdcCopyingThreadPoolExecutor = mdcCopyingThreadPoolExecutor;
        this.encryptionProfileProvider = encryptionProfileProvider;
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .proxyAuthenticator(new JavaNetAuthenticator())
                .dispatcher(new Dispatcher(mdcCopyingThreadPoolExecutor))
                .eventListener(OkHttpMetricsEventListener.builder(meterRegistry, AZURE_METRIC_NAME_PREFIX).uriMapper(new AzureUrlMetricTagMapper()).build());
        applyTlsHardeningIfEnabled(builder);
        okHttpClient = builder.build();
    }

    private void applyTlsHardeningIfEnabled(OkHttpClient.Builder builder) {
        if (tlsHardeningEnabled) {
            String[] ciphers = resolveCipherSuites();
            ConnectionSpec hardenedSpec = buildHardenedConnectionSpec(ciphers);
            builder.connectionSpecs(List.of(hardenedSpec));
            LOGGER.info("Initialising Azure HTTP client with TLS protocols=[{}] ciphers={}", TLS_1_3.getVersion(), Arrays.toString(ciphers));
        } else {
            LOGGER.debug("Azure HTTP client TLS hardening is disabled");
        }
    }

    private String[] resolveCipherSuites() {
        String cipherSuites = encryptionProfileProvider.getTls13RecommendedCipherSuites(true);
        return cipherSuites.split(CIPHER_SUITE_SEPARATOR);
    }

    private static ConnectionSpec buildHardenedConnectionSpec(String[] ianaCiphers) {
        CipherSuite[] okHttpCiphers = Arrays.stream(ianaCiphers)
                .map(CipherSuite::forJavaName)
                .toArray(CipherSuite[]::new);
        return new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                .tlsVersions(TlsVersion.TLS_1_3)
                .cipherSuites(okHttpCiphers)
                .build();
    }

    public <T extends HttpTrait<T>> T configureDefault(T configurable) {
        T client = configurable.httpLogOptions(getHttpLogOptions()).httpClient(newHttpClient());
        AzureQuartzRetryUtils.reconfigureHttpClientRetryOptionsIfNeeded(client::retryOptions);
        return client;
    }

    public <T extends AzureConfigurable<T>> T configureDefault(T configurable) {
        T client = configurable.withLogOptions(getHttpLogOptions()).withHttpClient(newHttpClient());
        AzureQuartzRetryUtils.reconfigureHttpClientRetryOptionsIfNeeded(client::withRetryOptions);
        return client;
    }

    public MarketplaceOrderingManager.Configurable configureDefault(MarketplaceOrderingManager.Configurable configurable) {
        MarketplaceOrderingManager.Configurable client = configurable.withLogOptions(getHttpLogOptions()).withHttpClient(newHttpClient());
        AzureQuartzRetryUtils.reconfigureHttpClientRetryOptionsIfNeeded(client::withRetryOptions);
        return client;
    }

    public PostgreSqlManager.Configurable configureDefault(PostgreSqlManager.Configurable configurable) {
        PostgreSqlManager.Configurable client = configurable.withLogOptions(getHttpLogOptions());
        AzureQuartzRetryUtils.reconfigureHttpClientIfNeeded(client::withRetryPolicy, client::withHttpClient, newHttpClientBuilder());
        return client;
    }

    public com.azure.resourcemanager.postgresqlflexibleserver.PostgreSqlManager.Configurable configureDefault(
            com.azure.resourcemanager.postgresqlflexibleserver.PostgreSqlManager.Configurable configurable) {
        com.azure.resourcemanager.postgresqlflexibleserver.PostgreSqlManager.Configurable client = configurable.withLogOptions(getHttpLogOptions());
        AzureQuartzRetryUtils.reconfigureHttpClientIfNeeded(client::withRetryPolicy, client::withHttpClient, newHttpClientBuilder());
        return client;
    }

    public HttpClient newHttpClient() {
        return newHttpClientBuilder().build();
    }

    private OkHttpAsyncHttpClientBuilder newHttpClientBuilder() {
        return new OkHttpAsyncHttpClientBuilder(okHttpClient);
    }

    private HttpLogOptions getHttpLogOptions() {
        return new HttpLogOptions().setLogLevel(logLevel);
    }
}
