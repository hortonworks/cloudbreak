package com.sequenceiq.freeipa.events;

import static com.sequenceiq.cloudbreak.structuredevent.rest.urlparser.CDPRestUrlParser.RESOURCE_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.CrnTestUtil;
import com.sequenceiq.cloudbreak.structuredevent.event.rest.RestCallDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.rest.RestRequestDetails;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
public class FreeIpaCustomCrnOrNameProviderTest {

    @InjectMocks
    private FreeIpaCustomCrnOrNameProvider underTest;

    @Mock
    private StackService stackService;

    @Test
    public void testProviderWhenNoQueryParams() {
        RestCallDetails restCallDetails = new RestCallDetails();
        RestRequestDetails restRequestDetails = new RestRequestDetails();
        restCallDetails.setRestRequest(restRequestDetails);
        restRequestDetails.setRequestUri("http://localhost/any/path");
        Map<String, String> restParams = new HashMap<>();
        underTest.provide(restCallDetails, null, restParams, "name", "crn");
        assertFalse(restParams.containsKey("name"));
        assertFalse(restParams.containsKey("crn"));
    }

    @Test
    public void testProviderWhenHasQueryParamsButNotContainsEnvironmentCrn() {
        RestCallDetails restCallDetails = new RestCallDetails();
        RestRequestDetails restRequestDetails = new RestRequestDetails();
        restCallDetails.setRestRequest(restRequestDetails);
        restRequestDetails.setRequestUri("http://localhost/any/path?asdda=asdasd");
        Map<String, String> restParams = new HashMap<>();
        underTest.provide(restCallDetails, null, restParams, "name", "crn");
        assertFalse(restParams.containsKey("name"));
        assertFalse(restParams.containsKey("crn"));
    }

    @Test
    public void testProviderWhenHasQueryParamsAndContainsEnvironmentCrn() {
        RestCallDetails restCallDetails = new RestCallDetails();
        RestRequestDetails restRequestDetails = new RestRequestDetails();
        restCallDetails.setRestRequest(restRequestDetails);
        restRequestDetails.setRequestUri("http://localhost/any/path?environment=env-crn");

        String userCrn = CrnTestUtil.getUserCrnBuilder()
                .setAccountId("acc")
                .setResource("res")
                .build().toString();

        Stack stack = new Stack();
        stack.setName("stackName");
        stack.setResourceCrn("stackCrn");
        stack.setId(2922L);

        when(stackService.findMultipleByEnvironmentCrnAndAccountIdEvenIfTerminated("env-crn", "acc")).thenReturn(List.of(stack));

        Map<String, String> restParams = new HashMap<>();
        Map<String, String> expected = ThreadBasedUserCrnProvider.doAs(userCrn, () -> underTest.provide(restCallDetails, null, restParams, "name", "crn"));
        assertEquals("stackName", expected.get("name"));
        assertEquals("stackCrn", expected.get("crn"));
        assertEquals("2922", expected.get(RESOURCE_ID));
    }
}
