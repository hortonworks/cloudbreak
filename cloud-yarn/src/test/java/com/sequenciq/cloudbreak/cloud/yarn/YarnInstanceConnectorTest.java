package com.sequenciq.cloudbreak.cloud.yarn;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.InstanceAuthentication;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.Volume;
import com.sequenceiq.cloudbreak.cloud.yarn.YarnInstanceConnector;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class YarnInstanceConnectorTest {

    private final YarnInstanceConnector underTest = new YarnInstanceConnector();

    @Mock
    private AuthenticatedContext authenticatedContext;

    @Mock
    private List< CloudResource > resources;

    @Mock
    private List< CloudInstance > vms;

    @Test
    public void startInstances() {
        String name = "master";
        List<Volume> volumes = Arrays.asList(new Volume("/hadoop/fs1", "HDD", 1), new Volume("/hadoop/fs2", "HDD", 1));
        Map<String, Object> params = new HashMap<>();
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");
        InstanceTemplate instanceTemplate = new InstanceTemplate("m1.medium", name, 0L, volumes, InstanceStatus.CREATE_REQUESTED,
                new HashMap<>(), 0L);

        CloudInstance instance = new CloudInstance("SOME_ID", instanceTemplate, instanceAuthentication, params);

        List<CloudInstance> vms = Arrays.asList(instance);

        underTest.start(authenticatedContext,  resources, vms);
    }

}
