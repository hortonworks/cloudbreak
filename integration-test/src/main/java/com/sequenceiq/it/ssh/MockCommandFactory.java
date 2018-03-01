package com.sequenceiq.it.ssh;

import org.apache.sshd.server.Command;
import org.apache.sshd.server.CommandFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MockCommandFactory implements CommandFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(MockCommandFactory.class);

    @Override
    public Command createCommand(String command) {
        return new EverythingOkCommand();
    }

}