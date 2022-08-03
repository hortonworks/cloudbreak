package com.sequenceiq.it.cloudbreak.util.ssh.client;

import static java.lang.String.format;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.IOUtils;
import net.schmizz.sshj.connection.ConnectionException;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.connection.channel.direct.Session.Command;
import net.schmizz.sshj.sftp.FileAttributes;
import net.schmizz.sshj.sftp.FileMode;
import net.schmizz.sshj.sftp.Response;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.sftp.SFTPException;
import net.schmizz.sshj.transport.TransportException;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.xfer.FileSystemFile;
import net.schmizz.sshj.xfer.scp.SCPDownloadClient;
import net.schmizz.sshj.xfer.scp.SCPFileTransfer;
import net.schmizz.sshj.xfer.scp.SCPUploadClient;

@Service
public class SshJClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(SshJClient.class);

    @Value("${integrationtest.defaultPrivateKeyFile}")
    private String defaultPrivateKeyFilePath;

    public SshJClient() {
    }

    @PostConstruct
    private void logSetup() {
        if (StringUtils.isEmpty(defaultPrivateKeyFilePath)) {
            LOGGER.info("Private key is not set");
        } else {
            if (Files.exists(Path.of(defaultPrivateKeyFilePath))) {
                LOGGER.info("Private key is configured properly: {}", defaultPrivateKeyFilePath);
            } else {
                LOGGER.info("Private key is set but not exists: {}", defaultPrivateKeyFilePath);
            }
        }
    }

    protected SSHClient createSshClient(String host, String user, String password, String privateKeyFilePath) throws IOException {
        SSHClient client = new SSHClient();

        client.addHostKeyVerifier(new PromiscuousVerifier());
        client.connect(host, 22);
        client.setConnectTimeout(120000);
        if (StringUtils.isBlank(user) && StringUtils.isBlank(privateKeyFilePath)) {
            LOGGER.info("Creating SSH client on '{}' host with 'cloudbreak' user and defaultPrivateKeyFile from application.yml.", host);
            client.authPublickey("cloudbreak", defaultPrivateKeyFilePath);
            Log.log(LOGGER, format(" SSH client has been authenticated with 'cloudbreak' user and key file: [%s] at [%s] host. ", client.isAuthenticated(),
                    client.getRemoteHostname()));
        } else if (StringUtils.isNotBlank(user) && StringUtils.isNotBlank(privateKeyFilePath)) {
            LOGGER.info("Creating SSH client on '{}' host with user: '{}' and key file: '{}'.", host, user, privateKeyFilePath);
            client.authPublickey(user, privateKeyFilePath);
            Log.log(LOGGER, format(" SSH client has been authenticated with user (%s) and key file: [%s] at [%s] host. ", user, client.isAuthenticated(),
                    client.getRemoteHostname()));
        } else if (StringUtils.isNotBlank(user) && StringUtils.isNotBlank(password)) {
            LOGGER.info("Creating SSH client on '{}' host with user: '{}' and password: '{}'.", host, user, password);
            client.authPassword(user, password);
            Log.log(LOGGER, format(" SSH client has been authenticated with user (%s) and password: [%s] at [%s] host. ", user, client.isAuthenticated(),
                    client.getRemoteHostname()));
        } else {
            LOGGER.error("Creating SSH client is not possible, because of host: '{}', user: '{}', password: '{}' and privateKey: '{}' are missing!",
                    host, user, password, privateKeyFilePath);
            throw new TestFailException(format("Creating SSH client is not possible, because of host: '%s', user: '%s', password: '%s'" +
                            " and privateKey: '%s' are missing!", host, user, password, privateKeyFilePath));
        }
        return client;
    }

    protected Pair<Integer, String> execute(SSHClient ssh, String command) throws IOException {
        LOGGER.info("Waiting to SSH command to be executed...");
        try (Session session = startSshSession(ssh);
            Command cmd = session.exec(command);
            OutputStream os = IOUtils.readFully(cmd.getInputStream())) {
            Log.log(LOGGER, format("The following SSH command [%s] is going to be executed on host [%s]", command,
                    ssh.getConnection().getTransport().getRemoteHost()));
            cmd.join(10L, TimeUnit.SECONDS);
            return Pair.of(cmd.getExitStatus(), os.toString());
        }
    }

    protected Pair<Integer, String> executeCommand(String instanceIP, String command) {
        try (SSHClient sshClient = createSshClient(instanceIP, null, null, null)) {
            Pair<Integer, String> cmdOut = execute(sshClient, command);
            Log.log(LOGGER, format("Command exit status [%s] and result [%s].", cmdOut.getKey(), cmdOut.getValue()));
            return cmdOut;
        } catch (Exception e) {
            LOGGER.error("SSH fail on [{}] while executing command [{}]. {}", instanceIP, command, e.getMessage());
            throw new TestFailException(" SSH fail on [" + instanceIP + "] while executing command [" + command + "].", e);
        }
    }

    private Session startSshSession(SSHClient ssh) throws ConnectionException, TransportException {
        Session sshSession = ssh.startSession();
        sshSession.allocateDefaultPTY();
        return sshSession;
    }

    private SCPUploadClient scpUploadClient(SSHClient ssh) {
        SCPFileTransfer scpFileTransfer = ssh.newSCPFileTransfer();
        return scpFileTransfer.newSCPUploadClient();
    }

    private SCPDownloadClient scpDownloadClient(SSHClient ssh) {
        SCPFileTransfer scpFileTransfer = ssh.newSCPFileTransfer();
        return scpFileTransfer.newSCPDownloadClient();
    }

    public void upload(SSHClient ssh, String localPath, String remotePath) {
//        String localFile = "src/main/resources/sample.txt";
//        String remoteDir = "remote_sftp_test/jschFile.txt";
        FileSystemFile uploadFile = new FileSystemFile(localPath);

        if (uploadFile.isFile()) {
            try (SFTPClient sftpClient = ssh.newSFTPClient()) {
                FileAttributes fileAttributes = sftpClient.stat(remotePath);
                sftpClient.put(uploadFile, remotePath);
            } catch (ConnectionException | TransportException e) {
                Log.error(LOGGER, "Upload [\" + localPath + \"] to [\" + remoteDirectory + \"] encountered connection error");
                throw new TestFailException(format("Upload [\" + localPath + \"] to [\" + remoteDirectory + \"] encountered connection error"));
            } catch (IOException e) {
                throw new TestFailException("Fatal error happened while trying to upload", e);
            } catch (SFTPException e) {

            } finally {
                ssh.disconnect();
                ssh.close();
            }
        } else {

        }
    }

    private boolean makeDirIfNotExists(SFTPClient sftpClient, String remote) throws IOException {
        try {
            FileAttributes attrs = sftpClient.stat(remote);
            if (attrs.getMode().getType() != FileMode.Type.DIRECTORY) {
                throw new IOException(remote + " exists and should be a directory, but was a " + attrs.getMode().getType());
            }
            // Was not created, but existed.
            return false;
        } catch (SFTPException e) {
            if (e.getStatusCode() == Response.StatusCode.NO_SUCH_FILE) {
                log.debug("makeDir: {} does not exist, creating", remote);
                sftpClient.mkdir(remote);
                return true;
            } else {
                throw e;
            }
        }
    }
}
