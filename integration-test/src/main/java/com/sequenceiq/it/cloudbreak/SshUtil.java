package com.sequenceiq.it.cloudbreak;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.IOUtils;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;


public class SshUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(com.sequenceiq.it.cloudbreak.SshUtil.class);

    public Boolean ssh(String host, String defaultPrivateKeyFile, String sshCommand, String checkType, String value) {
        SSHClient sshClient = null;
        boolean result = false;
        try {
            sshClient = createSSHClient(host, 22, "cloudbreak", defaultPrivateKeyFile);
            Pair<Integer, String> cmdOut = executeSshCommand(sshClient, sshCommand);
            LOGGER.info("Ssh command status code and output: " + cmdOut.toString());
            result = cmdOut.getLeft() == 0 && checkCommandOutput(cmdOut, checkType, value);
        } catch (Exception ex) {
            LOGGER.error("Error during remote command execution", ex);
        } finally {
            try {
                if (sshClient != null) {
                    sshClient.disconnect();
                }
            } catch (Exception ex) {
                LOGGER.error("Error during ssh disconnect", ex);
            }
        }
        return result;
    }

    private SSHClient createSSHClient(String host, int port, String user, String privateKeyFile) throws IOException {
        SSHClient sshClient = new SSHClient();
        sshClient.addHostKeyVerifier(new PromiscuousVerifier());
        sshClient.connect(host, port);
        sshClient.authPublickey(user, privateKeyFile);
        return sshClient;
    }

    private Session startSshSession(SSHClient ssh) throws IOException {
        Session sshSession = ssh.startSession();
        sshSession.allocateDefaultPTY();
        return sshSession;
    }

    private Pair<Integer, String> executeSshCommand(SSHClient ssh, String command) throws IOException {
        Session session = null;
        Session.Command cmd = null;
        LOGGER.info("Waiting to SSH command to be executed...");
        try {
            session = startSshSession(ssh);
            cmd = session.exec(command);
            String stdout = IOUtils.readFully(cmd.getInputStream()).toString();
            cmd.join(10, TimeUnit.SECONDS);
            return Pair.of(cmd.getExitStatus(), stdout);
        } finally {
            if (cmd != null) {
                cmd.close();
            }
            if (session != null) {
                session.close();
            }
        }
    }

    private Boolean checkCommandOutput(Pair<Integer, String> cmdOut, String checkType, String value) throws IOException {
        switch (checkType) {
            case "contains":
                if (cmdOut.getRight().contains(value)) {
                    return Boolean.TRUE;
                }
                break;
            case "notContains":
                if (!(cmdOut.getRight().contains(value))) {
                    return Boolean.TRUE;
                }
                break;
            case "beginsWith":
                if (cmdOut.getRight().startsWith(value)) {
                    return Boolean.TRUE;
                }
                break;
            default:
                LOGGER.info("Check type {} is not exist!", checkType);
                break;
        }
    return false;
    }
}
