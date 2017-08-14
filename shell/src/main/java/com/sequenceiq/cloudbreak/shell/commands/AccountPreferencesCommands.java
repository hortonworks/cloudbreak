package com.sequenceiq.cloudbreak.shell.commands;

import org.springframework.shell.core.CommandMarker;

public interface AccountPreferencesCommands extends CommandMarker {

    String setDefaultTags(String defaultTags);

}
