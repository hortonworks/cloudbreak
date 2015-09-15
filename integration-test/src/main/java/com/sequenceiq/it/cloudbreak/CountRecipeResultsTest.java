package com.sequenceiq.it.cloudbreak;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.sequenceiq.cloudbreak.client.CloudbreakClient;
import com.sequenceiq.it.IntegrationTestContext;
import org.springframework.beans.factory.annotation.Value;
import org.testng.Assert;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

public class CountRecipeResultsTest extends AbstractCloudbreakIntegrationTest {

    @Value("${integrationtest.defaultPrivateKeyFile}")
    private String defaultPrivateKeyFile;

    @Test
    @Parameters({ "searchRecipesOnHosts", "lookingFor", "require" })
    public void testFetchRecipeResult(String searchRecipesOnHosts, String lookingFor, Integer require) throws Exception {
        Assert.assertTrue(new File(defaultPrivateKeyFile).exists());
        Assert.assertFalse(lookingFor.isEmpty());

        CloudbreakClient client = getClient();
        IntegrationTestContext itContext = getItContext();
        String stackId = itContext.getContextParam(CloudbreakITContextConstants.STACK_ID);
        List<Map> instanceGroups = (List<Map>) ((Map) client.getStack(stackId)).get("instanceGroups");
        String[] files = lookingFor.split(",");
        List<String> publicIps = getPublicIps(instanceGroups, Arrays.asList(searchRecipesOnHosts.split(",")));
        List<Future> futures = new ArrayList<>(publicIps.size() * files.length);
        ExecutorService executorService = Executors.newFixedThreadPool(publicIps.size());
        final AtomicInteger count = new AtomicInteger(0);

        try {
            for (final String file : files) {
                for (final String ip : publicIps) {
                    futures.add(executorService.submit(new Runnable() {
                        @Override
                        public void run() {
                            if (findFile(ip, file)) {
                                count.incrementAndGet();
                            }
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

    private List<String> getPublicIps(List<Map> instanceGroups, List<String> hostGroupsWithRecipe) {
        List<String> ips = new ArrayList<>();
        for (Map instanceGroup : instanceGroups) {
            if (hostGroupsWithRecipe.contains(instanceGroup.get("group"))) {
                for (Map metaData : (List<Map>) instanceGroup.get("metadata")) {
                    ips.add((String) metaData.get("publicIp"));
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
            session.connect(1000);

            executor = (ChannelExec) session.openChannel("exec");
            executor.setCommand("sudo docker exec -it $(sudo docker ps |grep ambari-warmup |cut -d\" \" -f1) file " + file);
            executor.setPty(true);
            executor.connect(1000);

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
