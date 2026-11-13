package com.sequenceiq.maintenance.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.sequenceiq.cloudbreak.client.ApiClientRequestFilter;
import com.sequenceiq.cloudbreak.registry.ServiceAddressResolvingException;
import com.sequenceiq.freeipa.api.client.internal.FreeIpaApiClientParams;

class MaintenanceClientConfigContextTest {

    /**
     * Both configurations are picked up by MaintenanceApplication's component scan and both contribute a
     * {@code freeIpaApiClientParams} bean. Spring Boot disables bean-definition overriding by default, so loading
     * them together must leave a single definition - a duplicate would fail startup with
     * BeanDefinitionOverrideException. The runner pins overriding to false to mirror the production application.
     */
    @Test
    void bothMaintenanceClientConfigsCoexistWithSingleFreeIpaApiClientParamsBean() throws ServiceAddressResolvingException {
        MaintenancePeerServiceUrlResolver resolver = mock(MaintenancePeerServiceUrlResolver.class);
        when(resolver.resolveBaseUrl(any())).thenReturn("https://localhost:8080");

        new ApplicationContextRunner()
                .withAllowBeanDefinitionOverriding(false)
                .withBean(MaintenancePeerServiceUrlResolver.class, () -> resolver)
                .withBean(ApiClientRequestFilter.class, ApiClientRequestFilter::new)
                .withUserConfiguration(MaintenanceServiceEndpointConfig.class, MaintenanceResourceOwnerClientConfig.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(FreeIpaApiClientParams.class);
                    assertThat(context).getBeanNames(FreeIpaApiClientParams.class).containsExactly("freeIpaApiClientParams");
                    // The single surviving definition is MaintenanceServiceEndpointConfig's (cert.ignorePreValidation
                    // defaults to true there, false in the removed one), so client settings no longer depend on which wins.
                    assertThat(context.getBean(FreeIpaApiClientParams.class).isIgnorePreValidation()).isTrue();
                });
    }
}
