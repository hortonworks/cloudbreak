package com.sequenceiq.it.cloudbreak.newway;

import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.log.Log;
import com.sequenceiq.it.config.IntegrationTestConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;

@ContextConfiguration(classes = IntegrationTestConfiguration.class, initializers = ConfigFileApplicationContextInitializer.class)
public class GherkinTest extends AbstractTestNGSpringContextTests {
    public static final String RESULT = "RESULT";

    public static final String EMPTY_MESSAGE = "";

    private static final Logger LOGGER = LoggerFactory.getLogger(GherkinTest.class);

    private IntegrationTestContext itContext = new IntegrationTestContext();

    protected IntegrationTestContext getItContext() {
        return itContext;
    }

    protected void given(Entity entity, String message) throws Exception {
        Log.log("Given " + message);
        entity.create(getItContext());
        getItContext().putContextParam(entity.getEntityId(), entity);
    }

    protected void given(Entity entity) throws Exception {
        given(entity, entity.getEntityId());
    }

    protected void when(Action action, String message) throws Exception {
        Log.log("When " + message);
        getItContext().putContextParam(RESULT, action.action(getItContext()));
    }

    protected void when(Action action) throws Exception {
        when(action, EMPTY_MESSAGE);
    }

    protected void then(Assertion assertion, String message) {
        Log.log("Then " + message);
        assertion.doAssertion(getItContext());
    }

    protected void then(Assertion assertion) {
        then(assertion, EMPTY_MESSAGE);
    }
}
