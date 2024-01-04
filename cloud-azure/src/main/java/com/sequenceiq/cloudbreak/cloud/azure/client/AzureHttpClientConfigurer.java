package com.sequenceiq.cloudbreak.cloud.azure.client;

import java.util.concurrent.ExecutorService;

import jakarta.inject.Inject;

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

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.okhttp3.OkHttpMetricsEventListener;
import okhttp3.Dispatcher;
import okhttp3.JavaNetAuthenticator;
import okhttp3.OkHttpClient;

@Component
public class AzureHttpClientConfigurer {

    private static final String AZURE_METRIC_NAME_PREFIX = "azure";

    private final HttpLogDetailLevel logLevel;

    private final ExecutorService mdcCopyingThreadPoolExecutor;

    private final OkHttpClient okHttpClient;

    @Inject
    public AzureHttpClientConfigurer(
            @Value("${cb.azure.loglevel:BASIC}") HttpLogDetailLevel logLevel,
            @Qualifier("azureClientThreadPool") ExecutorService mdcCopyingThreadPoolExecutor,
            MeterRegistry meterRegistry) {
        this.logLevel = logLevel;
        this.mdcCopyingThreadPoolExecutor = mdcCopyingThreadPoolExecutor;
        okHttpClient = new OkHttpClient.Builder()
                .proxyAuthenticator(new JavaNetAuthenticator())
                .dispatcher(new Dispatcher(mdcCopyingThreadPoolExecutor))
                .eventListener(OkHttpMetricsEventListener.builder(meterRegistry, AZURE_METRIC_NAME_PREFIX).uriMapper(new AzureUrlMetricTagMapper()).build())
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
        PostgreSqlManager.Configurable client = configurable.withLogOptions(getHttpLogOptions()).withHttpClient(newHttpClient());
        AzureQuartzRetryUtils.reconfigureHttpClientRetryPolicyIfNeeded(client::withRetryPolicy);
        return client;
    }

    public com.azure.resourcemanager.postgresqlflexibleserver.PostgreSqlManager.Configurable configureDefault(
            com.azure.resourcemanager.postgresqlflexibleserver.PostgreSqlManager.Configurable configurable) {
        com.azure.resourcemanager.postgresqlflexibleserver.PostgreSqlManager.Configurable client =
                configurable.withLogOptions(getHttpLogOptions()).withHttpClient(newHttpClient());
        AzureQuartzRetryUtils.reconfigureHttpClientRetryPolicyIfNeeded(client::withRetryPolicy);
        return client;
    }

    public HttpClient newHttpClient() {
        return new OkHttpAsyncHttpClientBuilder(okHttpClient).build();
    }

    private HttpLogOptions getHttpLogOptions() {
        return new HttpLogOptions().setLogLevel(logLevel);
    }
}
