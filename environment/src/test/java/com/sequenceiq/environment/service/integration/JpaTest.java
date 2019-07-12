package com.sequenceiq.environment.service.integration;


import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.sequenceiq.environment.proxy.repository.ProxyConfigRepository;
import com.sequenceiq.environment.proxy.v1.ProxyTestSource;

@DataJpaTest
@EntityScan(basePackages = {"com.sequenceiq.flow.domain",
        "com.sequenceiq.environment",
        "com.sequenceiq.cloudbreak.ha.domain"})
@Tag("outofscope")
public class JpaTest {
    @Inject
    private ProxyConfigRepository proxyConfigRepository;

    @Test
    void testProxyConfigRepository() {
        proxyConfigRepository.save(ProxyTestSource.getProxyConfig());
        Assertions.assertEquals(ProxyTestSource.ID, proxyConfigRepository
                .findByResourceCrnInAccount(ProxyTestSource.RESCRN,
                        ProxyTestSource.ACCOUNT_ID).get().getId());
    }
}
