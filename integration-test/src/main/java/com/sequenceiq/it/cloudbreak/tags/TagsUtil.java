package com.sequenceiq.it.cloudbreak.tags;

import java.io.ByteArrayInputStream;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.junit.Assert;
import org.openstack4j.api.OSClient;
import org.openstack4j.openstack.OSFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.Tag;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.Base64;
import com.google.api.client.util.SecurityUtils;
import com.google.api.services.compute.Compute;
import com.google.api.services.compute.Compute.Builder;
import com.google.api.services.compute.Compute.Instances;
import com.google.api.services.compute.Compute.Instances.Get;
import com.google.api.services.compute.ComputeScopes;
import com.google.api.services.compute.model.Tags;
import com.microsoft.azure.PagedList;
import com.microsoft.azure.credentials.ApplicationTokenCredentials;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.sequenceiq.cloudbreak.api.model.InstanceGroupResponse;
import com.sequenceiq.cloudbreak.api.model.InstanceMetaDataJson;
import com.sequenceiq.cloudbreak.api.model.StackResponse;
import com.sequenceiq.it.util.ResourceUtil;

public class TagsUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(TagsUtil.class);

    private TagsUtil() {
    }

    public static Map<String, String> getTagsToCheck(String tags) {
        List<String> tagsToCheckList = Arrays.asList(tags.split(";"));
        Map<String, String> tagsToCheckMap = new HashMap<>();
        for (String elem : tagsToCheckList) {
            String[] tmpList = elem.split(":");
            Assert.assertTrue(tmpList.length > 1);
            tagsToCheckMap.put(tmpList[0], tmpList[1]);
        }
        return tagsToCheckMap;
    }

    protected static List<String> getInstancesList(StackResponse stackResponse) {
        List<InstanceGroupResponse> instanceGroups = stackResponse.getInstanceGroups();
        List<String> instanceIdList = new ArrayList<>();
        for (InstanceGroupResponse instanceGroup : instanceGroups) {
            Set<InstanceMetaDataJson> instanceMetaData = instanceGroup.getMetadata();
            for (InstanceMetaDataJson metaData : instanceMetaData) {
                instanceIdList.add(metaData.getInstanceId());
            }
        }
        return instanceIdList;
    }

    protected static void checkTags(Map<String, String> tagsToCheck, Map<String, String> extractedTagsToCheck) {
        for (Entry<String, String> entry : tagsToCheck.entrySet()) {
            Assert.assertTrue(extractedTagsToCheck.keySet().contains(entry.getKey()));
            Assert.assertEquals(extractedTagsToCheck.get(entry.getKey()), entry.getValue());
        }
    }

    protected static Map<String, String> checkTagsStack(StackResponse stackResponse) {
        Map<String, String> userDefinedTagsList = stackResponse.getUserDefinedTags();
        Assert.assertNotNull(userDefinedTagsList);
        return userDefinedTagsList;
    }

    protected static void checkTagsWithProvider(String stackName, Map<String, String> cloudProviderParams, ApplicationContext applicationcontext,
            List<String> instanceIds, Map<String, String> tagsToCheckMap) throws
            Exception {
        switch (cloudProviderParams.get("cloudProvider")) {
            case "AWS":
                checkTagsAws(Regions.fromName(cloudProviderParams.get("region")), instanceIds, tagsToCheckMap);
                break;
            case "AZURE":
                checkTagsAzure(cloudProviderParams.get("accesKey"), cloudProviderParams.get("tenantId"), cloudProviderParams.get("secretKey"),
                        cloudProviderParams.get("subscriptionId"), stackName, tagsToCheckMap);
                break;
            case "GCP":
                checkTagsGcp(applicationcontext, cloudProviderParams.get("applicationName"), cloudProviderParams.get("projectId"),
                        cloudProviderParams.get("serviceAccountId"), cloudProviderParams.get("p12File"), cloudProviderParams.get("availabilityZone"),
                        instanceIds, tagsToCheckMap);
                break;
            case "OPENSTACK":
                checkTagsOpenstack(cloudProviderParams.get("endpoint"), cloudProviderParams.get("userName"), cloudProviderParams.get("password"),
                        cloudProviderParams.get("tenantName"), instanceIds, tagsToCheckMap);
                break;
            default:
                LOGGER.info("CloudProvider {} is not supported!", cloudProviderParams.get("cloudProvider"));
                break;
        }
    }

    protected static void checkTagsAws(Regions region, List<String> instanceIdList, Map<String, String> tagsToCheckMap) {
        Map<String, String> extractedTagsToCheck = new HashMap<>();
        List<Tag> extractedTags;
        AmazonEC2 ec2 = AmazonEC2ClientBuilder.standard().withRegion(region).build();
        DescribeInstancesRequest describeInstancesRequest = new DescribeInstancesRequest();
        describeInstancesRequest.withInstanceIds(instanceIdList);
        DescribeInstancesResult describeInstancesResultAll = ec2.describeInstances(describeInstancesRequest);
        List<Reservation> reservationsAll = describeInstancesResultAll.getReservations();
        for (Reservation reservation : reservationsAll) {
            for (Instance instance : reservation.getInstances()) {
                extractedTags = instance.getTags();
                Assert.assertNotNull(extractedTags);
                for (Tag tag : extractedTags) {
                    extractedTagsToCheck.put(tag.getKey(), tag.getValue());
                }
                checkTags(tagsToCheckMap, extractedTagsToCheck);
                extractedTags.clear();
            }
        }
    }

    protected static void checkTagsAzure(String accesKey, String tenantId, String secretKey, String subscriptionId,
            String stackName, Map<String, String> tagsToCheckMap) {
        ApplicationTokenCredentials serviceClientCredentials = new ApplicationTokenCredentials(accesKey, tenantId, secretKey, null);
        Azure azure = Azure.authenticate(serviceClientCredentials).withSubscription(subscriptionId);
        PagedList<VirtualMachine> virtualMachinesList = azure.virtualMachines().list();
        for (VirtualMachine vm : virtualMachinesList) {
            if (vm.name().contains(stackName)) {
                Map<String, String> extractedTags = vm.tags();
                checkTags(tagsToCheckMap, extractedTags);
            }
        }
    }

    protected static void checkTagsGcp(ApplicationContext applicationContext, String applicationName, String projectId, String serviceAccountId, String p12File,
            String availabilityZone, List<String> instanceIdList, Map<String, String> tagsToCheckMap) throws Exception {
        String serviceAccountPrivateKey = ResourceUtil.readBase64EncodedContentFromResource(applicationContext, p12File);
        HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        PrivateKey privateKey = SecurityUtils.loadPrivateKeyFromKeyStore(SecurityUtils.getPkcs12KeyStore(),
                new ByteArrayInputStream(Base64.decodeBase64(serviceAccountPrivateKey)), "notasecret", "privatekey", "notasecret");
        JacksonFactory jsonFactory = JacksonFactory.getDefaultInstance();

        GoogleCredential googleCredential = new GoogleCredential.Builder().setTransport(httpTransport)
                .setJsonFactory(jsonFactory)
                .setServiceAccountId(serviceAccountId)
                .setServiceAccountScopes(Collections.singletonList(ComputeScopes.COMPUTE))
                .setServiceAccountPrivateKey(privateKey)
                .build();

        Compute compute = new Builder(httpTransport, jsonFactory, null)
                .setApplicationName(applicationName)
                .setHttpRequestInitializer(googleCredential)
                .build();

        Instances instances = compute.instances();

        for (String id : instanceIdList) {
            Get response = instances.get(projectId, availabilityZone, id);
            com.google.api.services.compute.model.Instance instance = response.execute();
            Tags gcpTags = instance.getTags();
            Map<String, String> extractedTags = new HashMap<>();
            List<String> tagList = gcpTags.getItems();

            for (String i : tagList) {
                String[] tmpTagList = i.split("-");
                if (tmpTagList.length > 1) {
                    extractedTags.put(tmpTagList[0], tmpTagList[1]);
                }
            }
            checkTags(tagsToCheckMap, extractedTags);
            extractedTags.clear();
        }

    }

    protected static void checkTagsOpenstack(String endpoint, String userName, String password, String tenantName, List<String> instanceIdList,
            Map<String, String> tagsToCheckMap) {
        OSClient os = OSFactory.builderV2()
                .endpoint(endpoint)
                .credentials(userName, password)
                .tenantName(tenantName)
                .authenticate();

        for (String instanceId : instanceIdList) {
            Map<String, String> serverMetadata = os.compute().servers().getMetadata(instanceId);
            checkTags(tagsToCheckMap, serverMetadata);
        }
    }
}
