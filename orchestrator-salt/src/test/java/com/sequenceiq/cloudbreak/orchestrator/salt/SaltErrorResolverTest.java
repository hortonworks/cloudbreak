package com.sequenceiq.cloudbreak.orchestrator.salt;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

class SaltErrorResolverTest {

    @Test
    void getResolvedErrorMessageTest() {
        SaltErrorResolver saltErrorResolver = new SaltErrorResolver();
        saltErrorResolver.init();

        Map<String, String> error1 = Collections.singletonMap("Error", "Error1");
        Map<String, String> error2 = Collections.singletonMap("Name", "/usr/local/bin/freeipa_backup");
        Map<String, String> error3 = Collections.singletonMap("Error", "Error3");
        Map<String, String> error4 = Collections.singletonMap("Error", "Error4");
        Map<String, String> error5 = Collections.singletonMap("Error", "Error5");
        Map<String, String> error6 = Collections.singletonMap("Error", "Error6");
        Map<String, String> error7 = Collections.singletonMap("Name", "sh /etc/td-agent/check_fluent_plugins.sh");

        Multimap<String, Map<String, String>> originalErrors = ArrayListMultimap.create();
        originalErrors.put("node1", error1);
        originalErrors.put("node1", error2);
        originalErrors.put("node1", error3);
        originalErrors.put("node1", error4);
        originalErrors.put("node2", error5);
        originalErrors.put("node2", error6);
        originalErrors.put("node2", error7);

        Multimap<String, String> resolvedMessages = saltErrorResolver.resolveErrorMessages(originalErrors);
        assertThat(resolvedMessages.get("node1"), containsInAnyOrder("Error1", "Failed to upload freeipa backup to object storage, " +
                "please check the assigned permissions to the instance", "Error3", "Error4"));
        assertThat(resolvedMessages.get("node2"), containsInAnyOrder("Error5", "Error6", "Connection has failed for " +
                "\"https://repository.cloudera.com/cloudera/api/gems/cloudera-gems/\" or \"https://api.rubygems.org\". " +
                "If servers are not down or no network: please check your firewall or use a newer OS image with newer fluent plugin versions!"));
    }

    @Test
    void getErrorMessageFromStderrTest() {
        SaltErrorResolver saltErrorResolver = new SaltErrorResolver();
        saltErrorResolver.init();
        Map<String, String> error1 = new HashMap<>();
        error1.put("Name", "/opt/salt/scripts/backup_db.sh AWS");
        error1.put("Comment", "Command /opt/salt/scripts/backup_db.sh AWS");
        error1.put("Stdout", "Logs at /var/log/dl_postgres_backup.log");
        error1.put("Stderr", "Could not create backup");
        Map<String, String> error2 = new HashMap<>();
        error2.put("Name", "/opt/salt/scripts/restore_db.sh AWS");
        error2.put("Comment", "Command /opt/salt/scripts/restore_db.sh AWS");
        error2.put("Stdout", "Logs at /var/log/dl_postgres_backup.log");
        error2.put("Stderr", "Restore operation failed");

        Multimap<String, Map<String, String>> originalErrors = ArrayListMultimap.create();
        originalErrors.put("node1", error1);
        originalErrors.put("node2", error2);

        Multimap<String, String> resolvedMessages = saltErrorResolver.resolveErrorMessages(originalErrors);
        assertThat(resolvedMessages.get("node1"), containsInAnyOrder("Could not create backup"));
        assertThat(resolvedMessages.get("node2"), containsInAnyOrder("Restore operation failed"));
    }

}