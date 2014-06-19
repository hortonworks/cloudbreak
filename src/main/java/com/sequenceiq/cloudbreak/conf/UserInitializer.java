package com.sequenceiq.cloudbreak.conf;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.model.InstanceType;
import com.sequenceiq.cloudbreak.domain.AwsCredential;
import com.sequenceiq.cloudbreak.domain.AwsTemplate;
import com.sequenceiq.cloudbreak.domain.User;
import com.sequenceiq.cloudbreak.repository.UserRepository;

@Component
public class UserInitializer implements InitializingBean {

    @Value("${HBM2DDL_STRATEGY}")
    private String hbm2ddlStrategy;

    @Autowired
    private UserRepository userRepository;

    @Override
    public void afterPropertiesSet() throws Exception {
        if ("create".equals(hbm2ddlStrategy) || "create-drop".equals(hbm2ddlStrategy)) {
            User user2 = new User();
            user2.setEmail("cbuser@sequenceiq.com");
            user2.setFirstName("seq");
            user2.setLastName("test");
            user2.setPassword("test123");

            // AzureCredential azureCredential = new AzureCredential();
            // azureCredential.setSubscriptionId("1234-45567-123213-12312");
            // azureCredential.setJks("test123");
            // azureCredential.setAzureCredentialOwner(user2);
            // azureCredential.setBlueprintName("azure_credential");

            AwsCredential awsCredential = new AwsCredential();
            awsCredential.setRoleArn("arn:aws:iam::755047402263:role/seq-self-cf");
            awsCredential.setInstanceProfileRoleArn("arn:aws:iam::755047402263:instance-profile/readonly-role");
            awsCredential.setAwsCredentialOwner(user2);
            awsCredential.setName("aws_credential");

            user2.getAwsCredentials().add(awsCredential);
            // user2.getAzureCredentials().add(azureCredential);

            AwsTemplate awsTemplate = new AwsTemplate();
            awsTemplate.setKeyName("sequence-eu");
            awsTemplate.setDescription("sample description");
            awsTemplate.setName("Aws development environment");
            awsTemplate.setRegion(Regions.EU_WEST_1);
            awsTemplate.setAmiId("ami-f39f5684");
            awsTemplate.setInstanceType(InstanceType.M1Small);
            awsTemplate.setSshLocation("0.0.0.0/0");
            awsTemplate.setUser(user2);
            /*
             * 
             * Stack awsStack = new Stack(); awsStack.setTemplate(awsTemplate);
             * awsStack.setNodeCount(NODE_COUNT); awsStack.setBlueprintName("coreos");
             * awsStack.setUser(user2); awsStack.setCredential(awsCredential);
             * awsStack.setAmbariIp("12.23.35.45");
             * awsStack.setStatus(Status.CREATE_COMPLETED);
             */

            user2.getAwsTemplates().add(awsTemplate);
            // user2.getStacks().add(awsStack);

            // AzureTemplate azureTemplate = new AzureTemplate();
            // azureTemplate.setDeploymentSlot("slot");
            // azureTemplate.setDescription("azure desc");
            // azureTemplate.setImageName("image");
            // azureTemplate.setLocation("location");
            // azureTemplate.setBlueprintName("azurename");
            // azureTemplate.setUserName("username");
            // azureTemplate.setPassword("pass");
            // azureTemplate.setSubnetAddressPrefix("prefix");
            // azureTemplate.setVmType("small");
            // Port port = new Port();
            // port.setLocalPort("8080");
            // port.setBlueprintName("local");
            // port.setProtocol("TCP");
            // port.setPort("8080");
            // port.setAzureTemplate(azureTemplate);
            // azureTemplate.getPorts().add(port);
            // azureTemplate.setUser(user2);

            // Stack azureStack = new Stack();
            // azureStack.setTemplate(azureTemplate);
            // azureStack.setNodeCount(NODE_COUNT);
            // azureStack.setUser(user2);
            // azureStack.setCredential(azureCredential);
            // azureStack.setBlueprintName("azure stack");
            // awsStack.setAmbariIp("12.23.35.45");
            // awsStack.setStatus(Status.CREATE_COMPLETED);

            /*
             * Blueprint blueprint1 = new Blueprint();
             * blueprint1.setBlueprintName("single-node-hdfs-yarn");
             * blueprint1.setBlueprintText(
             * "{\"host_groups\":[{\"name\":\"host_group_1\",\"components\":[{\"name\":\"NAMENODE\"},"
             * + "{\"name\":\"SECONDARY_NAMENODE\"}," +
             * "{\"name\":\"DATANODE\"},{\"name\":\"HDFS_CLIENT\"},{\"name\":\"RESOURCEMANAGER\"},{\"name\":\"NODEMANAGER\"},"
             * +
             * "{\"name\":\"YARN_CLIENT\"},{\"name\":\"HISTORYSERVER\"},{\"name\":\"MAPREDUCE2_CLIENT\"},"
             * +
             * "{\"name\":\"ZOOKEEPER_SERVER\"},{\"name\":\"ZOOKEEPER_CLIENT\"}],\"cardinality\":\"1\"}],\"Blueprints\":"
             * +
             * "{\"blueprint_name\":\"single-node-hdfs-yarn\",\"stack_name\":\"HDP\",\"stack_version\":\"2.0\"}}"
             * ); blueprint1.setUser(user2);
             */

            /*
             * Blueprint blueprint2 = new Blueprint();
             * blueprint2.setBlueprintName("sample blueprint 1");
             * blueprint2.setBlueprintText("{\"data\": {}}");
             * blueprint2.setUser(user2);
             */

            // user2.getBlueprints().add(blueprint1);
            // user2.getBlueprints().add(blueprint2);
            // user2.getAzureTemplates().add(azureTemplate);
            // user2.getStacks().add(azureStack);
            // user2.getStacks().add(awsStack);

            userRepository.save(user2);
        }
    }
}
