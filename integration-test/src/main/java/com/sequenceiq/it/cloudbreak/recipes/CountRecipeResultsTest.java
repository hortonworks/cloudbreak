package com.sequenceiq.it.cloudbreak.recipes;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.testng.Assert;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v3.StackV3Endpoint;
import com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceGroupResponse;
import com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceMetaDataJson;
import com.sequenceiq.cloudbreak.client.CloudbreakClient;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.AbstractCloudbreakIntegrationTest;
import com.sequenceiq.it.cloudbreak.CloudbreakITContextConstants;
import com.sequenceiq.it.cloudbreak.v2.CloudbreakV2Constants;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.IOUtils;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.connection.channel.direct.Session.Command;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;

public class CountRecipeResultsTest extends AbstractCloudbreakIntegrationTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(CountRecipeResultsTest.class);

    @Value("${integrationtest.defaultPrivateKeyFile}")
    private String defaultPrivateKeyFile;

    @Test
    @Parameters({"searchRecipesOnHosts", "lookingFor", "require"})
    public void testFetchRecipeResult(String searchRecipesOnHosts, String lookingFor, Integer require) throws Exception {
        //GIVEN
        Assert.assertEquals(new File(defaultPrivateKeyFile).exists(), true, "Private cert file not found: " + defaultPrivateKeyFile);
        Assert.assertFalse(lookingFor.isEmpty());

        IntegrationTestContext itContext = getItContext();

        String stackName = itContext.getContextParam(CloudbreakV2Constants.STACK_NAME);

        StackV3Endpoint stackV1Endpoint = itContext.getContextParam(CloudbreakITContextConstants.CLOUDBREAK_CLIENT, CloudbreakClient.class).stackV3Endpoint();
        Long workspaceId = itContext.getContextParam(CloudbreakITContextConstants.WORKSPACE_ID, Long.class);
        List<InstanceGroupResponse> instanceGroups = stackV1Endpoint.getByNameInWorkspace(workspaceId, stackName, new HashSet<>()).getInstanceGroups();
        String[] files = lookingFor.split(",");
        List<String> publicIps = getPublicIps(instanceGroups, Arrays.asList(searchRecipesOnHosts.split(",")));
        Collection<Future<Boolean>> futures = new ArrayList<>(publicIps.size() * files.length);
        ExecutorService executorService = Executors.newFixedThreadPool(publicIps.size());
        AtomicInteger count = new AtomicInteger(0);
        //WHEN
        try {
            for (String file : files) {
                for (String ip : publicIps) {
                    futures.add((Future<Boolean>) executorService.submit(() -> {
                        if (findFile(ip, file)) {
                            count.incrementAndGet();
                        }
                    }));
                }
            }

            for (Future<Boolean> future : futures) {
                future.get();
            }
        } finally {
            executorService.shutdown();
        }
        //THEN
        Assert.assertEquals(count.get(), require.intValue(), "The number of existing files is different than required.");
    }

    private List<String> getPublicIps(Iterable<InstanceGroupResponse> instanceGroups, Collection<String> hostGroupsWithRecipe) {
        List<String> ips = new ArrayList<>();
        for (InstanceGroupResponse instanceGroup : instanceGroups) {
            if (hostGroupsWithRecipe.contains(instanceGroup.getGroup())) {
                for (InstanceMetaDataJson metaData : instanceGroup.getMetadata()) {
                    ips.add(metaData.getPublicIp());
                }
            }
        }
        return ips;
    }

    private Boolean findFile(String host, String file) {
        SSHClient sshClient = null;
        boolean result = false;
        try {
            sshClient = createSSHClient(host, 22, "cloudbreak", defaultPrivateKeyFile);
            Pair<Integer, String> cmdOut = executeSshCommand(sshClient, "sudo ls " + file);
            result = cmdOut.getLeft() == 0 && cmdOut.getRight().startsWith(file);
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
        try (Session session = startSshSession(ssh)) {
            try (Command cmd = session.exec(command)) {
                try (ByteArrayOutputStream content = IOUtils.readFully(cmd.getInputStream())) {
                    String stdout = content.toString();
                    cmd.join(10, TimeUnit.SECONDS);
                    return Pair.of(cmd.getExitStatus(), stdout);
                }
            }
        }
    }
}
