package com.sequenceiq.cloudbreak.shell.commands;

import java.io.File;
import java.util.Map;

public interface CredentialCommands {

    String create(String name, File sshKeyPath, String sshKeyUrl, String sshKeyString,
            String description, boolean publicInAccount, Long platformId, Map<String, Object> parameters, String platform);

    boolean createCredentialAvailable(String platform);
}
