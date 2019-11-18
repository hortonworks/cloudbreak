package com.sequenceiq.it.cloudbreak;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;

import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.config.IntegrationTestConfiguration;

@ContextConfiguration(classes = {IntegrationTestConfiguration.class}, initializers = ConfigFileApplicationContextInitializer.class)
public class GherkinTest extends AbstractTestNGSpringContextTests {
    public static final String RESULT = "RESULT";

    public static final String EMPTY_MESSAGE = "";

    private static final Logger LOGGER = LoggerFactory.getLogger(GherkinTest.class);

    private final IntegrationTestContext itContext = new IntegrationTestContext();

    protected IntegrationTestContext getItContext() {
        return itContext;
    }

    protected void given(Entity entity, String message) throws Exception {
        if (entity != null) {
            Log.log("Given " + message);
            entity.create(itContext);
            itContext.putContextParam(entity.getEntityId(), entity);
        }
    }

    protected void given(Entity entity) throws Exception {
        if (entity != null) {
            given(entity, entity.getEntityId());
        }
    }

    protected void when(ResourceAction<?> resourceAction, String message) throws Exception {
        Log.log("When " + message);
        itContext.putContextParam(RESULT, resourceAction.action(itContext));
    }

    protected void when(ResourceAction<?> resourceAction) throws Exception {
        when(resourceAction, EMPTY_MESSAGE);
    }

    protected void then(Assertion<?> assertion, String message) {
        Log.log("Then " + message);
        assertion.doAssertion(itContext);
    }

    protected void then(Assertion<?> assertion) {
        then(assertion, EMPTY_MESSAGE);
    }
}
