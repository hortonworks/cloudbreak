package com.sequenceiq.cloudbreak.service.stack.flow;

import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.StackContext;

import net.schmizz.sshj.transport.verification.HostKeyVerifier;

public class SshCheckerTaskContext extends StackContext {

    private final HostKeyVerifier hostKeyVerifier;

    private final String publicIp;

    private final int sshPort;

    private final String user;

    private final String sshPrivateKey;

    public SshCheckerTaskContext(Stack stack, HostKeyVerifier hostKeyVerifier, String publicIp, int sshPort, String user, String sshPrivateKey) {
        super(stack);
        this.hostKeyVerifier = hostKeyVerifier;
        this.publicIp = publicIp;
        this.sshPort = sshPort;
        this.user = user;
        this.sshPrivateKey = sshPrivateKey;
    }

    public HostKeyVerifier getHostKeyVerifier() {
        return hostKeyVerifier;
    }

    public String getPublicIp() {
        return publicIp;
    }

    public int getSshPort() {
        return sshPort;
    }

    public String getSshPrivateKey() {
        return sshPrivateKey;
    }

    public String getUser() {
        return user;
    }
}
