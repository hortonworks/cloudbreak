package com.sequenceiq.cloudbreak.service.stack.flow;

import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.StackContext;

import net.schmizz.sshj.transport.verification.HostKeyVerifier;

public class SshCheckerTaskContext extends StackContext {

    private HostKeyVerifier hostKeyVerifier;
    private String publicIp;
    private int sshPort;
    private String user;
    private String sshPrivateFileLocation;

    public SshCheckerTaskContext(Stack stack, HostKeyVerifier hostKeyVerifier, String publicIp, int sshPort, String user, String sshPrivateFileLocation) {
        super(stack);
        this.hostKeyVerifier = hostKeyVerifier;
        this.publicIp = publicIp;
        this.sshPort = sshPort;
        this.user = user;
        this.sshPrivateFileLocation = sshPrivateFileLocation;
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

    public String getSshPrivateFileLocation() {
        return sshPrivateFileLocation;
    }

    public String getUser() {
        return user;
    }
}
