package com.sequenceiq.it.cloudbreak.util.ssh.action;

import static java.lang.String.format;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
import com.sequenceiq.it.cloudbreak.SdxClient;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.util.ssh.client.SshJClient;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.IOUtils;
import net.schmizz.sshj.connection.ConnectionException;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.transport.TransportException;

@Component
public class SshJClientActions extends SshJClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(SshJClientActions.class);

    private static Pair<Integer, String> execute(SSHClient ssh, String command) throws IOException {
        LOGGER.info("Waiting to SSH command to be executed...");
        try (Session session = startSshSession(ssh);
                Session.Command cmd = session.exec(command);
                OutputStream os = IOUtils.readFully(cmd.getInputStream())) {
            Log.log(LOGGER, format("The following SSH command [%s] is going to be executed on host [%s]", ssh.getConnection().getTransport().getRemoteHost(),
                    command));
            cmd.join(10L, TimeUnit.SECONDS);
            return Pair.of(cmd.getExitStatus(), os.toString());
        }
    }

    private static Session startSshSession(SSHClient ssh) throws ConnectionException, TransportException {
        Session sshSession = ssh.startSession();
        sshSession.allocateDefaultPTY();
        return sshSession;
    }

    private List<String> getSdxInstanceGroupIps(SdxInternalTestDto testDto, SdxClient sdxClient, List<String> hostGroupNames) {
        List<String> instanceIPs = new ArrayList<>();

        /**
         * Right now only the Private IP is available for an Instance.
         */
        hostGroupNames.forEach(hostGroupName -> {
            InstanceMetaDataV4Response instanceMetaDataV4Response = Objects.requireNonNull(sdxClient.getSdxClient().sdxEndpoint().getDetail(testDto.getName(),
                    new HashSet<>()).getStackV4Response().getInstanceGroups().stream().filter(instanceGroup -> instanceGroup.getName().equals(hostGroupName))
                    .findFirst().orElse(null)).getMetadata().stream().findFirst().orElse(null);
            assert instanceMetaDataV4Response != null;
            LOGGER.info("The selected Instance Group [{}] and the available Private IP [{}]", instanceMetaDataV4Response.getInstanceGroup(),
                    instanceMetaDataV4Response.getPrivateIp());
            instanceIPs.add(instanceMetaDataV4Response.getPrivateIp());
        });

        return instanceIPs;
    }

    public SdxInternalTestDto checkFilesByNameAndPath(SdxInternalTestDto testDto, SdxClient sdxClient,
            List<String> hostGroupNames, String filePath, String fileName, long requiredNumberOfFiles) {
        String fileListCommand = String.format("find %s -type f -name %s", filePath, fileName);
        AtomicLong quantity = new AtomicLong(0);

        getSdxInstanceGroupIps(testDto, sdxClient, hostGroupNames).forEach(instanceIP -> {
            try (SSHClient sshClient = createSshClient(instanceIP)) {
                Pair<Integer, String> cmdOut = execute(sshClient, fileListCommand);
                Log.log(LOGGER, format("Command exit status [%s] and result [%s].", String.valueOf(cmdOut.getKey()), cmdOut.getValue()));

                List<String> cmdOutputValues = List.of(cmdOut.getValue().split("[\\r\\n\\t]"))
                        .stream().filter(Objects::nonNull).collect(Collectors.toList());
                boolean fileFound = cmdOutputValues.stream()
                        .anyMatch(outputValue -> outputValue.strip().startsWith("/"));
                String foundFilePath = cmdOutputValues.stream()
                        .filter(outputValue -> outputValue.strip().startsWith("/")).findFirst().orElse(null);
                Log.log(LOGGER, format("The file is present [%s] at [%s] path.", fileFound, foundFilePath));

                quantity.set(cmdOutputValues.stream()
                        .filter(outputValue -> outputValue.strip().startsWith("/")).count());
            } catch (Exception e) {
                LOGGER.error("SSH fail on [{}] while getting info for [{}] file", instanceIP, filePath);
                throw new TestFailException(" SSH fail on [" + instanceIP + "] while getting info for [" + filePath + "] file.");
            }
        });

        if (requiredNumberOfFiles == quantity.get()) {
            Log.log(LOGGER, format(" File [%s] is available at [%s] host group(s). ", filePath, hostGroupNames.toString()));
        } else {
            LOGGER.error("File [{}] is NOT available at [{}] host group(s).", filePath, hostGroupNames.toString());
            throw new TestFailException(" File at: " + filePath + " path is NOT available at: " + hostGroupNames.toString() + " host group(s).");
        }
        return testDto;
    }
}
