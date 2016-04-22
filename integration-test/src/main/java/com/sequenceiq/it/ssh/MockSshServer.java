package com.sequenceiq.it.ssh;

import java.io.File;
import java.io.IOException;
import java.security.PublicKey;
import java.util.Collections;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.sshd.common.config.keys.FilePasswordProvider;
import org.apache.sshd.common.keyprovider.AbstractFileKeyPairProvider;
import org.apache.sshd.common.util.SecurityUtils;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.pubkey.PublickeyAuthenticator;
import org.apache.sshd.server.scp.ScpCommandFactory;
import org.apache.sshd.server.session.ServerSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

@Service
public class MockSshServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(MockSshServer.class);

    private SshServer sshServer;
    private boolean started = false;

    @Inject
    private ResourceLoader resourceLoader;

    @PostConstruct
    public void init() {
        sshServer = SshServer.setUpDefaultServer();
        AbstractFileKeyPairProvider fileKeyPairProvider = SecurityUtils.createFileKeyPairProvider();
        fileKeyPairProvider.setFiles(Collections.singleton(getHostkey()));
        fileKeyPairProvider.setPasswordFinder(new FilePasswordProvider() {
            @Override
            public String getPassword(String resourceKey) throws IOException {
                return "cloudbreak";
            }
        });
        sshServer.setKeyPairProvider(fileKeyPairProvider);
        sshServer.setPublickeyAuthenticator(createMockAuthenticator());
        setCommandFactory();
        sshServer.setFileSystemFactory(new MockFileSystemFactory());
    }

    private void setCommandFactory() {
        ScpCommandFactory scpCommandFactory = new ScpCommandFactory();
        scpCommandFactory.setDelegateCommandFactory(new MockCommandFactory());
        sshServer.setCommandFactory(scpCommandFactory);
    }

    private PublickeyAuthenticator createMockAuthenticator() {
        return new PublickeyAuthenticator() {
            @Override
            public boolean authenticate(String username, PublicKey key, ServerSession session) {
                return true;
            }
        };
    }

    private File getHostkey() {
        File hostkey;
        try {
            hostkey = resourceLoader.getResource("classpath:ssh.pem").getFile();
            return hostkey;
        } catch (IOException e) {
            throw new RuntimeException("hostkey not found", e);
        }
    }

    public void start() throws IOException {
        if (!started) {
            sshServer.start();
            started = true;
        }
    }

    public void setPort(int port) {
        sshServer.setPort(port);
    }
}
