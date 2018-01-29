package com.sequenceiq.it.cloudbreak.newway;

import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.config.IntegrationTestConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;

@ContextConfiguration(classes = IntegrationTestConfiguration.class, initializers = ConfigFileApplicationContextInitializer.class)
public class GherkinTest extends AbstractTestNGSpringContextTests {
    public static final String RESULT = "RESULT";

    private static final Logger LOGGER = LoggerFactory.getLogger(GherkinTest.class);

    private IntegrationTestContext itContext = new IntegrationTestContext();

    protected IntegrationTestContext getItContext() {
        return itContext;
    }

    protected void given(Entity entity) throws Exception {
        entity.create(getItContext());
        getItContext().putContextParam(entity.getEntityId(), entity);
    }

    protected void when(Action action) throws Exception {
        getItContext().putContextParam(RESULT, action.action(getItContext()));
    }

    protected void then(Assertion assertion) {
        assertion.doAssertion(getItContext());
    }
}
