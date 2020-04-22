package com.sequenceiq.cloudbreak.orchestrator.salt;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

class SaltErrorResolverTest {

    @Test
    void getResolvedErrorMessageTest() {
        SaltErrorResolver saltErrorResolver = new SaltErrorResolver();
        saltErrorResolver.init();
        Multimap<String, String> originalErrors = ArrayListMultimap.create();
        originalErrors.put("node1", "Error1");
        originalErrors.put("node1", "Name: /usr/local/bin/freeipa_backup");
        originalErrors.put("node1", "Error3");
        originalErrors.put("node1", "Error4");
        originalErrors.put("node2", "Error5");
        originalErrors.put("node2", "Error6");
        originalErrors.put("node2", "Name: /usr/local/bin/freeipa_backup");

        Multimap<String, String> resolvedMessages = saltErrorResolver.resolveErrorMessages(originalErrors);
        assertThat(resolvedMessages.get("node1"), containsInAnyOrder("Error1", "Failed to upload freeipa backup to object storage, " +
                "please check the assigned permissions to the instance", "Error3", "Error4"));
        assertThat(resolvedMessages.get("node2"), containsInAnyOrder("Error5", "Error6", "Failed to upload freeipa backup to object storage, " +
                "please check the assigned permissions to the instance"));
    }

}