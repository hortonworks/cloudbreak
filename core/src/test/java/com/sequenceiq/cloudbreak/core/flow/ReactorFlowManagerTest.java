package com.sequenceiq.cloudbreak.core.flow;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.sequenceiq.cloudbreak.conf.TestConfig;
import com.sequenceiq.cloudbreak.core.flow.context.ProvisioningContext;
import com.sequenceiq.cloudbreak.core.flow.handlers.ProvisioningSetupHandler;
import com.sequenceiq.cloudbreak.core.flow.service.ReactorFlowManager;

@Ignore
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { TestConfig.class })
@ComponentScan("com.sequenceiq.cloudbreak.core.flow")
public class ReactorFlowManagerTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReactorFlowManagerTest.class);

    @Inject
    private ReactorFlowManager flowManager;

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void shouldReturnTheNextSuccessTransition() throws Exception {
        flowManager.triggerNext(ProvisioningSetupHandler.class, new ProvisioningContext.Builder().build(), true);
    }

    @Test
    public void shouldReturnTheNextFailureTransition() throws Exception {
        flowManager.triggerNext(ProvisioningSetupHandler.class, new ProvisioningContext.Builder().build(), true);
    }

    @Test
    public void shouldTriggerProvisioning() {
        flowManager.triggerProvisioning(new ProvisioningContext.Builder().build());
    }

}