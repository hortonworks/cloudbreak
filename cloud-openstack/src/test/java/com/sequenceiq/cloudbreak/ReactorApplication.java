package com.sequenceiq.cloudbreak;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.event.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.resource.LaunchStackRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.LaunchStackResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Security;
import com.sequenceiq.cloudbreak.cloud.model.SecurityRule;
import com.sequenceiq.cloudbreak.cloud.model.Subnet;
import com.sequenceiq.cloudbreak.cloud.model.Volume;
import com.sequenceiq.cloudbreak.common.type.InstanceGroupType;

import reactor.Environment;
import reactor.bus.Event;
import reactor.bus.EventBus;
import reactor.fn.timer.Timer;
import reactor.rx.Promise;
import reactor.rx.Promises;

@EnableAutoConfiguration
@ComponentScan
public class ReactorApplication implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReactorApplication.class);
    private static final int DEFAULT_DELAY = 5;

    @Inject
    private Environment env;

    @Inject
    private EventBus eventBus;

    @Value("${userName:admin}")
    private String userName;

    //@Value("${password}")
    private String password;

    @Value("${tenantName:demo}")
    private String tenantName;

    //@Value("${endpoint}")
    private String endpoint;


    @Inject
    private Timer timer;

    public static void main(String[] args) throws InterruptedException {
        ApplicationContext app = SpringApplication.run(ReactorApplication.class, args);
        //Thread.sleep(60000);
        app.getBean(Environment.class).shutdown();
    }

    @Override
    public void run(String... args) throws Exception {
        //promiseTest();
//        errorHandlerTest();

    }

    private void asyncNotify(Promise<LaunchStackResult> promise) {
        LaunchStackRequest sr = request();
        eventBus.notify(CloudPlatformRequest.selector(LaunchStackRequest.class), Event.wrap(sr));
    }

    private void promiseTest() {
        try {
            Promise<LaunchStackResult> promise = Promises.prepare();
            asyncNotify(promise);
            LaunchStackResult result = promise.await(1, TimeUnit.HOURS);
            LOGGER.info("########### {}", result);
        } catch (InterruptedException e) {
            LOGGER.error("Exception:", e);
        }
    }

    private LaunchStackRequest request() {
        CloudCredential c = new CloudCredential("opencred", "somekey", "cloudbreak");
        c.putParameter("userName", userName);
        c.putParameter("password", password);
        c.putParameter("tenantName", tenantName);
        c.putParameter("endpoint", endpoint);

        List<Group> groups = new ArrayList<>();
        Group g = new Group("master", InstanceGroupType.CORE);
        groups.add(g);
        InstanceTemplate instance = new InstanceTemplate("m1.medium", g.getName(), 0L, InstanceStatus.CREATE_REQUESTED, new HashMap<String, Object>());
        Volume v = new Volume("/hadoop/fs1", "HDD", 1);
        instance.addVolume(v);
        v = new Volume("/hadoop/fs2", "HDD", 1);
        instance.addVolume(v);

        g.addInstance(instance);

        Image image = new Image("cb-centos66-amb200-2015-05-25");
        image.putUserData(InstanceGroupType.CORE, "CORE");
        image.putUserData(InstanceGroupType.GATEWAY, "GATEWAY");

        Subnet subnet = new Subnet("10.0.0.0/24");
        Network network = new Network(subnet);
        network.putParameter("publicNetId", "028ffc0c-63c5-4ca0-802a-3ac753eaf76c");

        List<SecurityRule> rules = Arrays.asList(new SecurityRule("0.0.0.0/0", new String[]{"22", "443"}, "tcp"));
        Security security = new Security(rules);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String ts = sdf.format(new Date());

        CloudContext cloudContext = new CloudContext(0L, "stack-name_" + ts, "OPENSTACK", "owner");

        CloudStack cs = new CloudStack(groups, network, security, image, "local", new HashMap<String, String>());
        LaunchStackRequest lr = new LaunchStackRequest(cloudContext, c, cs, null, null);
        LOGGER.debug("Launchrequest: {}", lr);
        return lr;

    }

    private void errorHandlerTest() {
        eventBus.notify("selector-no-consumer", Event.wrap("no consumer for me"));
    }


}
