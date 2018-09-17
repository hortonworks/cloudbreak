package com.sequenceiq.it.cloudbreak.newway.context;

import java.util.Collection;
import java.util.List;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;

import com.sequenceiq.it.cloudbreak.newway.CredentialEntity;
import com.sequenceiq.it.cloudbreak.newway.ImageCatalogEntity;
import com.sequenceiq.it.cloudbreak.newway.StackEntity;

@Service
public class PurgeGarbageService implements ApplicationContextAware {

    private static final Logger LOGGER = LoggerFactory.getLogger(PurgeGarbageService.class);

    private ApplicationContext applicationContext;

    public <T> void purge() {
        LOGGER.info("purge starts");
        TestContext testContext = applicationContext.getBean(TestContext.class);
        testContext.as();
        purge(testContext);
        testContext.shutdown();
    }

    private <T> void purge(TestContext testContext) {
        testContext.when(new StackEntity(), (testContext1, entity, cloudbreakClient) -> {
            purgables().forEach(purgable -> {
                Collection<Object> all = purgable.getAll(cloudbreakClient);
                LOGGER.info("Purge all {}, count: {}", purgable.getClass().getSimpleName(), all.size());
                all.stream()
                        .filter(purgable::deletable)
                        .forEach(e -> purgable.delete(e, cloudbreakClient));
            });
            return entity;
        });
    }

    private <T, P extends Purgable<T>> List<P> purgables() {
        return List.of((P) new StackEntity(), (P) new CredentialEntity(), (P) new ImageCatalogEntity());
    }

    @Override
    public void setApplicationContext(@Nonnull ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }
}
