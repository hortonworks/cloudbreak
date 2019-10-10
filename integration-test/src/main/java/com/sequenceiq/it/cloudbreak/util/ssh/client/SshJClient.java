package com.sequenceiq.it.cloudbreak.util.ssh.client;

import static java.lang.String.format;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.it.cloudbreak.log.Log;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;

@Service
public class SshJClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(SshJClient.class);

    @Value("${integrationtest.defaultPrivateKeyFile}")
    private String defaultPrivateKeyFile;

    public SshJClient() {
    }

    protected SSHClient createSshClient(String host) throws IOException {
        SSHClient client = new SSHClient();

        client.addHostKeyVerifier(new PromiscuousVerifier());
        client.connect(host, 22);
        client.setConnectTimeout(120000);
        client.authPublickey("cloudbreak", defaultPrivateKeyFile);
        Log.log(LOGGER, format("SSH client has been authenticated [%s] with at [%s]", client.isAuthenticated(), client.getRemoteHostname()));

        return client;
    }
}
