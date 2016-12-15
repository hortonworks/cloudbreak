package com.sequenceiq.it.cloudbreak.recipes;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.testng.Assert;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.sequenceiq.cloudbreak.api.endpoint.StackEndpoint;
import com.sequenceiq.cloudbreak.api.model.InstanceGroupResponse;
import com.sequenceiq.cloudbreak.api.model.InstanceMetaDataJson;
import com.sequenceiq.cloudbreak.client.CloudbreakClient;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.AbstractCloudbreakIntegrationTest;
import com.sequenceiq.it.cloudbreak.CloudbreakITContextConstants;
import com.sequenceiq.it.cloudbreak.StopSshServerTest;

public class CountRecipeResultsTest extends AbstractCloudbreakIntegrationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(StopSshServerTest.class);

    private List<String> activeIPs = Collections.synchronizedList(new ArrayList<String>());

    @Value("${integrationtest.defaultPrivateKeyFile}")
    private String defaultPrivateKeyFile;

    @Test
    @Parameters({ "searchRecipesOnHosts", "lookingFor", "require" })
    public void testFetchRecipeResult(String searchRecipesOnHosts, String lookingFor, Integer require) throws Exception {
        //GIVEN
        Assert.assertEquals(new File(defaultPrivateKeyFile).exists(), true, "Private cert file not found: " + defaultPrivateKeyFile);
        Assert.assertFalse(lookingFor.isEmpty());

        IntegrationTestContext itContext = getItContext();

        String stackId = itContext.getContextParam(CloudbreakITContextConstants.STACK_ID);

        StackEndpoint stackEndpoint = itContext.getContextParam(CloudbreakITContextConstants.CLOUDBREAK_CLIENT, CloudbreakClient.class).stackEndpoint();
        List<InstanceGroupResponse> instanceGroups = stackEndpoint.get(Long.valueOf(stackId)).getInstanceGroups();
        String[] files = lookingFor.split(",");
        List<String> publicIps = getPublicIps(instanceGroups, Arrays.asList(searchRecipesOnHosts.split(",")));
        List<Future> futures = new ArrayList<>(publicIps.size() * files.length);
        ExecutorService executorService = Executors.newFixedThreadPool(publicIps.size());
        final AtomicInteger count = new AtomicInteger(0);
        //WHEN
        try {
            for (final String file : files) {
                for (final String ip : publicIps) {
                    futures.add(executorService.submit(() -> {
                        try {
                            while (activeIPs.contains(ip)) {
                                Thread.sleep(1000);
                            }
                            if (findFile(ip, file)) {
                                count.incrementAndGet();
                            }
                        } catch (InterruptedException exception) {
                            LOGGER.info("Error in thread sleep.");
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

    private List<String> getPublicIps(List<InstanceGroupResponse> instanceGroups, List<String> hostGroupsWithRecipe) {
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

    private Boolean findFile(String host, String file) throws InterruptedException {
        Boolean resp = false;
        Session session = null;
        ChannelExec executor = null;
        String readLine;
        activeIPs.add(host);
        try {
            JSch jsch = new JSch();
            jsch.addIdentity(defaultPrivateKeyFile);

            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");

            session = jsch.getSession("cloudbreak", host, 22);
            session.setConfig(config);
            session.connect(10000);

            executor = (ChannelExec) session.openChannel("exec");
            executor.setCommand("sudo ls " + file);
            executor.setPty(true);
            InputStream in = executor.getInputStream();
            OutputStream out = executor.getOutputStream();
            executor.connect(10000);
            out.write("\n".getBytes());
            out.flush();
            Thread.sleep(1000);
            readLine = "";
            byte[] tmp = new byte[1024];
            while (true) {
                while  (in.available() > 0) {
                    int i = in.read(tmp, 0, 1024);
                    if (i < 0) {
                        break;
                    }
                    readLine += new String(tmp, 0, i);
                }
                if (executor.isClosed()) {
                    break;
                }
                Thread.sleep(1000);
            }
            if (readLine.startsWith(file)) {
                resp = true;
            }

        } catch (JSchException | IOException e) {
            Assert.fail(e.getMessage());
        } finally {
            if (executor != null) {
                executor.disconnect();
            }
            if (session != null) {
                session.disconnect();
            }
            activeIPs.remove(host);
        }
        return resp;
    }
}
