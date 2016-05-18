package com.sequenceiq.it.ssh;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.apache.sshd.common.keyprovider.AbstractFileKeyPairProvider;
import org.apache.sshd.common.util.SecurityUtils;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.scp.ScpCommandFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

@Service
public class MockSshServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(MockSshServer.class);

    private Map<Integer, SshServer> sshServerMap = new HashMap<>();

    @Inject
    private ResourceLoader resourceLoader;

    public void start(int port) throws IOException {
        if (sshServerMap.get(port) == null) {
            SshServer sshServer = SshServer.setUpDefaultServer();
            AbstractFileKeyPairProvider fileKeyPairProvider = SecurityUtils.createFileKeyPairProvider();
            fileKeyPairProvider.setFiles(Collections.singleton(getHostkey()));
            fileKeyPairProvider.setPasswordFinder(resourceKey -> "cloudbreak");
            sshServer.setKeyPairProvider(fileKeyPairProvider);
            sshServer.setPublickeyAuthenticator((username, key, session) -> true);
            setCommandFactory(sshServer);
            sshServer.setFileSystemFactory(new MockFileSystemFactory());
            sshServer.setPort(port);
            sshServer.start();
            sshServerMap.put(port, sshServer);
        }
    }

    private void setCommandFactory(SshServer sshServer) {
        ScpCommandFactory scpCommandFactory = new ScpCommandFactory();
        scpCommandFactory.setDelegateCommandFactory(new MockCommandFactory());
        sshServer.setCommandFactory(scpCommandFactory);
    }

    private File getHostkey() {
        try {
            InputStream sshPemInputStream = resourceLoader.getResource("classpath:ssh.pem").getInputStream();
            File tempFile = new File("ssh.pem");
            try (OutputStream outputStream = new FileOutputStream(tempFile)) {
                IOUtils.copy(sshPemInputStream, outputStream);
            } catch (IOException e) {
                LOGGER.error("can't write ssh.pem", e);
            }
            return tempFile;
        } catch (IOException e) {
            throw new RuntimeException("hostkey not found", e);
        }
    }

    public void stop(int port) throws IOException {
        SshServer sshServer = sshServerMap.get(port);
        if (sshServer != null) {
            sshServer.stop();
            sshServerMap.remove(port);
        }
    }
}
