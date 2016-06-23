package com.sequenceiq.it.ssh

import org.apache.sshd.server.Command
import org.apache.sshd.server.CommandFactory
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class MockCommandFactory : CommandFactory {

    override fun createCommand(command: String): Command {
        return EverythingOkCommand()
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(MockCommandFactory::class.java)
    }

}