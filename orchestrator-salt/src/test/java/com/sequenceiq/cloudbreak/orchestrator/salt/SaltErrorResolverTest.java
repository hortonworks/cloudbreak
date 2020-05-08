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
        originalErrors.put("node2", "Name: sh /etc/td-agent/check_fluent_plugins.sh");

        Multimap<String, String> resolvedMessages = saltErrorResolver.resolveErrorMessages(originalErrors);
        assertThat(resolvedMessages.get("node1"), containsInAnyOrder("Error1", "Failed to upload freeipa backup to object storage, " +
                "please check the assigned permissions to the instance", "Error3", "Error4"));
        assertThat(resolvedMessages.get("node2"), containsInAnyOrder("Error5", "Error6", "Connection has failed for " +
                "\"https://repository.cloudera.com/cloudera/api/gems/cloudera-gems/\" or \"https://api.rubygems.org\". " +
                "If servers are not down or no network: please check your firewall or use a newer OS image with newer fluent plugin versions!"));
    }

}