package com.sequenceiq.cloudbreak.shell.commands;

import java.util.Map;

public interface NetworkCommands {

    String create(String name, String subnet, Boolean publicInAccount, String description, Long platformId, Map<String, Object> parameters, String platform);

    boolean createNetworkAvailable(String platform);
}
