package com.sequenceiq.it.cloudbreak.testcase.e2e.aws;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import javax.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.it.cloudbreak.client.FreeIPATestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIPATestDto;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;

public class AwsFreeIPATests extends AbstractE2ETest {

    protected static final Status FREEIPA_AVAILABLE = Status.AVAILABLE;

    protected static final Status FREEIPA_DELETE_COMPLETED = Status.DELETE_COMPLETED;

    @Inject
    private FreeIPATestClient freeIPATestClient;

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a running cloudbreak",
            when = "a valid stack create request is sent AND the stack is stopped AND the stack is started",
            then = "the stack should be available AND deletable")
    public void testCreateFreeIPA(TestContext testContext) {
        String freeIpa = resourcePropertyProvider().getName();

        testContext
                .given(freeIpa, FreeIPATestDto.class)
                .when(freeIPATestClient.create(), key(freeIpa))
                .await(FREEIPA_AVAILABLE)
                .then((tc, testDto, client) -> freeIPATestClient.delete().action(tc, testDto, client))
                .await(FREEIPA_DELETE_COMPLETED)
                .validate();
    }
}
