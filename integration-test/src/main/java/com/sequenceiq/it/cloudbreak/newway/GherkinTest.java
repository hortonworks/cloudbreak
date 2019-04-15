package com.sequenceiq.it.cloudbreak.newway;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;

import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.AbstractCloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.newway.log.Log;
import com.sequenceiq.it.cloudbreak.newway.logsearch.LogSearchUtil;
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
            if (entity instanceof AbstractCloudbreakTestDto) {
                String resourceName = ((AbstractCloudbreakTestDto) entity).getName();
                if (StringUtils.isNotBlank(resourceName)) {
                    LogSearchUtil.addQueryModelForLogSearchUrlToContext(itContext,
                            LogSearchUtil.LOG_SEARCH_CBNAME_ID, LogSearchUtil.LOG_SEARCH_CBNAME_QUERY_TYPE, resourceName);
                }
            }
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
