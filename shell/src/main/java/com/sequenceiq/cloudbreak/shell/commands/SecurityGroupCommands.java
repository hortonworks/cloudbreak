package com.sequenceiq.cloudbreak.shell.commands;

import com.sequenceiq.cloudbreak.shell.completion.SecurityRules;

public interface SecurityGroupCommands {

    String create(String name, String description, String existingSecurityGroupId, String platform,
            SecurityRules rules, Boolean publicInAccount);

    boolean createSecurityGroupAvailable(String platform);
}
