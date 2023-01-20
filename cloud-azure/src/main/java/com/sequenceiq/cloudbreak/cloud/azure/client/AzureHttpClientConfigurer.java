package com.sequenceiq.cloudbreak.cloud.azure.client;

import java.util.concurrent.ExecutorService;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.azure.core.client.traits.HttpTrait;
import com.azure.core.http.HttpClient;
import com.azure.core.http.okhttp.OkHttpAsyncHttpClientBuilder;
import com.azure.core.http.policy.HttpLogDetailLevel;
import com.azure.core.http.policy.HttpLogOptions;
import com.azure.resourcemanager.marketplaceordering.MarketplaceOrderingManager;
import com.azure.resourcemanager.resources.fluentcore.arm.AzureConfigurable;

import okhttp3.Dispatcher;
import okhttp3.JavaNetAuthenticator;
import okhttp3.OkHttpClient;

@Component
public class AzureHttpClientConfigurer {

    private final HttpLogDetailLevel logLevel;

    private final ExecutorService mdcCopyingThreadPoolExecutor;

    private final OkHttpClient okHttpClient;

    @Inject
    public AzureHttpClientConfigurer(
            @Value("${cb.azure.loglevel:BASIC}") HttpLogDetailLevel logLevel,
            @Qualifier("azureClientThreadPool") ExecutorService mdcCopyingThreadPoolExecutor) {
        this.logLevel = logLevel;
        this.mdcCopyingThreadPoolExecutor = mdcCopyingThreadPoolExecutor;
        okHttpClient = new OkHttpClient.Builder()
                .proxyAuthenticator(new JavaNetAuthenticator())
                .dispatcher(new Dispatcher(mdcCopyingThreadPoolExecutor))
                .build();
    }

    public <T extends HttpTrait<T>> T configureDefault(T configurable) {
        return configurable.httpLogOptions(new HttpLogOptions().setLogLevel(logLevel)).httpClient(newHttpClient());
    }

    public <T extends AzureConfigurable<T>> T configureDefault(T configurable) {
        return configurable.withLogLevel(logLevel).withHttpClient(newHttpClient());
    }

    public MarketplaceOrderingManager.Configurable configureDefault(MarketplaceOrderingManager.Configurable configurable) {
        return configurable.withLogOptions(new HttpLogOptions().setLogLevel(logLevel)).withHttpClient(newHttpClient());
    }

    public HttpClient newHttpClient() {
        return new OkHttpAsyncHttpClientBuilder(okHttpClient).build();
    }
}
