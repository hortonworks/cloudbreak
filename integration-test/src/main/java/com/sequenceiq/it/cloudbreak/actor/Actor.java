package com.sequenceiq.it.cloudbreak.actor;

public interface Actor {

    CloudbreakUser defaultUser();

    CloudbreakUser secondUser();

    CloudbreakUser create(String tenantName, String username);

    CloudbreakUser useRealUmsUser(String key);
}