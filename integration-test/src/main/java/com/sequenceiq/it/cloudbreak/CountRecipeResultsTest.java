package com.sequenceiq.it.cloudbreak;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Value;
import org.testng.Assert;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.sequenceiq.cloudbreak.api.endpoint.StackEndpoint;
import com.sequenceiq.cloudbreak.api.model.InstanceGroupJson;
import com.sequenceiq.cloudbreak.api.model.InstanceMetaDataJson;
import com.sequenceiq.cloudbreak.client.CloudbreakClient;
import com.sequenceiq.it.IntegrationTestContext;

public class CountRecipeResultsTest extends AbstractCloudbreakIntegrationTest {

    @Value("${integrationtest.defaultPrivateKeyFile}")
    private String defaultPrivateKeyFile;

    @Value("${integrationtest.ambariContainer}")
    private String ambariContainer;

    @Test
    @Parameters({ "searchRecipesOnHosts", "lookingFor", "require" })
    public void testFetchRecipeResult(String searchRecipesOnHosts, String lookingFor, Integer require) throws Exception {
        Assert.assertEquals(new File(defaultPrivateKeyFile).exists(), true, "Private cert file not found: " + defaultPrivateKeyFile);
        Assert.assertFalse(lookingFor.isEmpty());

        IntegrationTestContext itContext = getItContext();
        String stackId = itContext.getContextParam(CloudbreakITContextConstants.STACK_ID);
        StackEndpoint stackEndpoint = itContext.getContextParam(CloudbreakITContextConstants.CLOUDBREAK_CLIENT, CloudbreakClient.class).stackEndpoint();
        List<InstanceGroupJson> instanceGroups = stackEndpoint.get(Long.valueOf(stackId)).getInstanceGroups();
        String[] files = lookingFor.split(",");
        List<String> publicIps = getPublicIps(instanceGroups, Arrays.asList(searchRecipesOnHosts.split(",")));
        List<Future> futures = new ArrayList<>(publicIps.size() * files.length);
        ExecutorService executorService = Executors.newFixedThreadPool(publicIps.size());
        final AtomicInteger count = new AtomicInteger(0);

        try {
            for (final String file : files) {
                for (final String ip : publicIps) {
                    futures.add(executorService.submit(() -> {
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

        Assert.assertEquals(count.get(), require.intValue(), "The number of existing files is different than required.");
    }

    private List<String> getPublicIps(List<InstanceGroupJson> instanceGroups, List<String> hostGroupsWithRecipe) {
        List<String> ips = new ArrayList<>();
        for (InstanceGroupJson instanceGroup : instanceGroups) {
            if (hostGroupsWithRecipe.contains(instanceGroup.getGroup())) {
                for (InstanceMetaDataJson metaData : instanceGroup.getMetadata()) {
                    ips.add(metaData.getPublicIp());
                }
            }
        }
        return ips;
    }

    private Boolean findFile(String host, String file) {
        Boolean resp = false;
        Session session = null;
        ChannelExec executor = null;
        try {
            JSch jsch = new JSch();
            jsch.addIdentity(defaultPrivateKeyFile);

            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");

            session = jsch.getSession("cloudbreak", host, 22);
            session.setConfig(config);
            session.connect(10000);

            executor = (ChannelExec) session.openChannel("exec");
            executor.setCommand("sudo docker exec -it $(sudo docker ps |grep " + ambariContainer + " |cut -d\" \" -f1) file " + file);
            executor.setPty(true);
            executor.connect(10000);

            BufferedReader reader = new BufferedReader(new InputStreamReader(executor.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith(file + ": empty")) {
                    resp = true;
                }
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
        }
        return resp;
    }
}
