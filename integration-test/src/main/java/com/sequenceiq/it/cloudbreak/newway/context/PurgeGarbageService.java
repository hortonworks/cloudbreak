package com.sequenceiq.it.cloudbreak.newway.context;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.Prototype;

@Prototype
public class PurgeGarbageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PurgeGarbageService.class);

    @Inject
    private ApplicationContext applicationContext;

    @Inject
    private List<Purgable> purgables;

    @Value("${integrationtest.cleanup.cleanupBeforeStart:false}")
    private boolean cleanupBeforeStart;

    public <T> void purge() {
        MDC.put("testlabel", "purge");
        LOGGER.info("purge starts");
        TestContext testContext = applicationContext.getBean(TestContext.class);
        try {
            testContext.as();
            purge(testContext);
            MDC.put("testlabel", null);
        } catch (Exception e) {
            LOGGER.error("Error happended during purging the test data. Some entities might have been left over", e);
        } finally {
            testContext.shutdown();
        }
    }

    private List<Purgable> orderedPurgables() {
        return purgables.stream().sorted(new CompareByOrder()).collect(Collectors.toList());
    }

    private <T> void purge(TestContext testContext) {
        CloudbreakClient cloudbreakClient = testContext.getCloudbreakClient();
        orderedPurgables().forEach(purgable -> {
            Collection<Object> all = purgable.getAll(cloudbreakClient);
            all = all.stream()
                    .filter(purgable::deletable)
                    .collect(Collectors.toList());
            LOGGER.info("Purge all {}, count: {}", purgable.getClass().getSimpleName(), all.size());
            if (cleanupBeforeStart) {
                all.forEach(e -> purgable.delete(testContext, e, cloudbreakClient));
            } else {
                all.forEach(e -> LOGGER.info("Created resource remained in the Database: {}", e));
            }
        });
    }
}
