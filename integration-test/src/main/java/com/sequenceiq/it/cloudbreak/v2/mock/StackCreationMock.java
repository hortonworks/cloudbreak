package com.sequenceiq.it.cloudbreak.v2.mock;

import static com.sequenceiq.it.spark.ITResponse.AMBARI_API_ROOT;
import static com.sequenceiq.it.spark.ITResponse.IMAGE_CATALOG;
import static com.sequenceiq.it.spark.ITResponse.MOCK_ROOT;
import static com.sequenceiq.it.spark.ITResponse.SALT_API_ROOT;
import static com.sequenceiq.it.spark.ITResponse.SALT_BOOT_ROOT;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.client.RestClientUtil;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.cloudbreak.orchestrator.model.GenericResponse;
import com.sequenceiq.cloudbreak.orchestrator.model.GenericResponses;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.CloudbreakITContextConstants;
import com.sequenceiq.it.cloudbreak.mock.json.CBVersion;
import com.sequenceiq.it.spark.ITResponse;
import com.sequenceiq.it.spark.ambari.AmbariCheckResponse;
import com.sequenceiq.it.spark.ambari.AmbariClusterRequestsResponse;
import com.sequenceiq.it.spark.ambari.AmbariClusterResponse;
import com.sequenceiq.it.spark.ambari.AmbariHostsResponse;
import com.sequenceiq.it.spark.ambari.AmbariServicesComponentsResponse;
import com.sequenceiq.it.spark.ambari.AmbariStatusResponse;
import com.sequenceiq.it.spark.ambari.AmbariVersionDefinitionResponse;
import com.sequenceiq.it.spark.ambari.EmptyAmbariClusterResponse;
import com.sequenceiq.it.spark.ambari.EmptyAmbariResponse;
import com.sequenceiq.it.spark.ambari.v2.AmbariCategorizedHostComponentStateResponse;
import com.sequenceiq.it.spark.salt.SaltApiRunPostResponse;
import com.sequenceiq.it.spark.spi.CloudMetaDataStatuses;
import com.sequenceiq.it.util.HostNameUtil;
import com.sequenceiq.it.verification.Verification;

import spark.Service;

@Component(StackCreationMock.NAME)
@Scope("prototype")
public class StackCreationMock extends MockServer {

    public static final String NAME = "StackCreationMock";

    private static final Logger LOGGER = LoggerFactory.getLogger(StackCreationMock.class);

    private static final String DEFAULT_KERBEROS_PRINCIPAL = "/admin";

    private boolean clusterCreated;

    public StackCreationMock(int mockPort, int sshPort, int numberOfServers) {
        super(mockPort, sshPort, numberOfServers);
    }

    public StackCreationMock(int mockPort, int sshPort, Map<String, CloudVmMetaDataStatus> instanceMap) {
        super(mockPort, sshPort, instanceMap);
    }

    public void addSPIEndpoints() {
        Map<String, CloudVmMetaDataStatus> instanceMap = getInstanceMap();
        getSparkService().post(MOCK_ROOT + "/cloud_metadata_statuses", new CloudMetaDataStatuses(instanceMap), gson()::toJson);
    }

    public void mockImageCatalogResponse(IntegrationTestContext itContext) {
        getSparkService().get(IMAGE_CATALOG, (request, response) -> {
            String cbServerRoot = itContext.getContextParam(CloudbreakITContextConstants.CLOUDBREAK_SERVER_ROOT);
            Client client = RestClientUtil.get();
            WebTarget target = client.target(cbServerRoot + "/info");
            CBVersion cbVersion = target.request().get().readEntity(CBVersion.class);
            return responseFromJsonFile("imagecatalog/catalog.json").replace("CB_VERSION", cbVersion.getApp().getVersion());
        });
    }

    public void addAmbariMappings(String clusterName) {
        Map<String, CloudVmMetaDataStatus> instanceMap = getInstanceMap();
        Service sparkService = getSparkService();
        sparkService.get(AMBARI_API_ROOT + "/clusters/:cluster/requests/:request", new AmbariStatusResponse());
        sparkService.post(AMBARI_API_ROOT + "/views/:view/versions/1.0.0/instances/*", new EmptyAmbariResponse());
        sparkService.get(AMBARI_API_ROOT + "/views/", (request, response) -> List.of(clusterName));
        sparkService.get(AMBARI_API_ROOT + "/clusters", (req, resp) -> {
            ITResponse itResp = clusterCreated ? new AmbariClusterResponse(instanceMap, clusterName) : new EmptyAmbariClusterResponse();
            return itResp.handle(req, resp);
        });
        sparkService.post(AMBARI_API_ROOT + "/clusters/:cluster/requests", new AmbariClusterRequestsResponse());
        sparkService.post(AMBARI_API_ROOT + "/clusters/:cluster", (req, resp) -> {
            clusterCreated = true;
            return new EmptyAmbariResponse().handle(req, resp);
        }, gson()::toJson);
        sparkService.get(AMBARI_API_ROOT + "/clusters", new AmbariClusterResponse(instanceMap, clusterName));
        sparkService.post(AMBARI_API_ROOT + "/clusters/:cluster/requests", new AmbariClusterRequestsResponse());
        sparkService.post(AMBARI_API_ROOT + "/clusters/:cluster", new EmptyAmbariResponse());
        sparkService.get(AMBARI_API_ROOT + "/services/AMBARI/components/AMBARI_SERVER", new AmbariServicesComponentsResponse(), gson()::toJson);
        sparkService.get(AMBARI_API_ROOT + "/hosts", new AmbariHostsResponse(instanceMap), gson()::toJson);
        sparkService.get(AMBARI_API_ROOT + "/blueprints/:blueprintname", (request, response) -> {
            response.type("text/plain");
            return responseFromJsonFile("blueprint/" + request.params("blueprintname") + ".bp");
        });
        sparkService.post(AMBARI_API_ROOT + "/blueprints/*", new EmptyAmbariResponse());
        sparkService.put(AMBARI_API_ROOT + "/users/admin", new EmptyAmbariResponse());
        sparkService.get(AMBARI_API_ROOT + "/check", new AmbariCheckResponse());
        sparkService.post(AMBARI_API_ROOT + "/users", new EmptyAmbariResponse());
        sparkService.get(AMBARI_API_ROOT + "/clusters/:cluster/hosts", new AmbariCategorizedHostComponentStateResponse(instanceMap));
        sparkService.put(AMBARI_API_ROOT + "/stacks/HDP/versions/:version/operating_systems/:os/repositories/:hdpversion",
                new AmbariVersionDefinitionResponse());
        sparkService.get(AMBARI_API_ROOT + "/version_definitions", new AmbariVersionDefinitionResponse());
        sparkService.post(AMBARI_API_ROOT + "/version_definitions", new EmptyAmbariResponse());
    }

    public void addSaltMappings() {
        Map<String, CloudVmMetaDataStatus> instanceMap = getInstanceMap();
        ObjectMapper objectMapper = new ObjectMapper();
        Service sparkService = getSparkService();
        sparkService.get(SALT_BOOT_ROOT + "/health", (request, response) -> {
            GenericResponse genericResponse = new GenericResponse();
            genericResponse.setStatusCode(HttpStatus.OK.value());
            return genericResponse;
        }, gson()::toJson);
        objectMapper.setVisibility(objectMapper.getVisibilityChecker().withGetterVisibility(Visibility.NONE));
        sparkService.post(SALT_API_ROOT + "/run", new SaltApiRunPostResponse(instanceMap));
        sparkService.post(SALT_BOOT_ROOT + "/file", (request, response) -> {
            response.status(HttpStatus.CREATED.value());
            return response;
        });
        sparkService.post(SALT_BOOT_ROOT + "/salt/server/pillar", (request, response) -> {
            GenericResponse genericResponse = new GenericResponse();
            genericResponse.setStatusCode(HttpStatus.OK.value());
            return genericResponse;
        }, gson()::toJson);
        sparkService.post(SALT_BOOT_ROOT + "/salt/action/distribute", (request, response) -> {
            GenericResponses genericResponses = new GenericResponses();
            genericResponses.setResponses(new ArrayList<>());
            return genericResponses;
        }, gson()::toJson);
        sparkService.post(SALT_BOOT_ROOT + "/hostname/distribute", (request, response) -> {
            GenericResponses genericResponses = new GenericResponses();
            List<GenericResponse> responses = new ArrayList<>();

            for (CloudVmMetaDataStatus status : instanceMap.values()) {
                GenericResponse genericResponse = new GenericResponse();
                genericResponse.setAddress(status.getMetaData().getPrivateIp());
                genericResponse.setStatus(HostNameUtil.generateHostNameByIp(status.getMetaData().getPrivateIp()));
                genericResponse.setStatusCode(HttpStatus.OK.value());
                responses.add(genericResponse);
            }
            genericResponses.setResponses(responses);
            return genericResponses;
        }, gson()::toJson);
        sparkService.post(SALT_BOOT_ROOT + "/file/distribute", (request, response) -> {
            GenericResponses genericResponses = new GenericResponses();
            GenericResponse genericResponse = new GenericResponse();
            genericResponse.setStatusCode(HttpStatus.CREATED.value());
            genericResponses.setResponses(Collections.singletonList(genericResponse));
            return genericResponses;
        }, gson()::toJson);
        sparkService.post(SALT_BOOT_ROOT + "/salt/server/pillar/distribute", (request, response) -> {
            GenericResponses genericResponses = new GenericResponses();
            GenericResponse genericResponse = new GenericResponse();
            genericResponse.setStatusCode(HttpStatus.OK.value());
            genericResponses.setResponses(Collections.singletonList(genericResponse));
            return genericResponses;
        }, gson()::toJson);
    }

    public void verifyCalls(String clusterName) {
        verify(SALT_BOOT_ROOT + "/health", "GET").exactTimes(1).verify();
        Verification distributeVerification = verify(SALT_BOOT_ROOT + "/salt/action/distribute", "POST").exactTimes(1);

        for (CloudVmMetaDataStatus status : getInstanceMap().values()) {
            distributeVerification.bodyContains("address\":\"" + status.getMetaData().getPrivateIp());
        }
        distributeVerification.verify();

        verify(AMBARI_API_ROOT + "/services/AMBARI/components/AMBARI_SERVER", "GET").exactTimes(1).verify();
        verify(AMBARI_API_ROOT + "/clusters", "GET").exactTimes(2).verify();
        verify(AMBARI_API_ROOT + "/check", "GET").atLeast(1).verify();
        verify(AMBARI_API_ROOT + "/users", "POST").exactTimes(2).verify();
        verify(AMBARI_API_ROOT + "/blueprints/bp", "POST").exactTimes(1)
                .bodyContains("blueprint_name").bodyContains("stack_name").bodyContains("stack_version").bodyContains("host_groups")
                .exactTimes(1).verify();
        verify(AMBARI_API_ROOT + "/clusters/" + clusterName, "POST").exactTimes(1).bodyContains("blueprint").bodyContains("default_password")
                .bodyContains("host_groups").verify();
        verify(AMBARI_API_ROOT + "/clusters/" + clusterName + "/requests/1", "GET").atLeast(1).verify();

        verify(SALT_API_ROOT + "/run", "POST").bodyContains("fun=saltutil.sync_all").atLeast(1).verify();
        verify(SALT_API_ROOT + "/run", "POST").bodyContains("fun=mine.update").atLeast(1).verify();
        verify(SALT_API_ROOT + "/run", "POST").bodyContains("fun=state.highstate").atLeast(2).verify();
        verify(SALT_API_ROOT + "/run", "POST").bodyContains("fun=jobs.lookup_jid").bodyContains("jid=1").atLeast(2).verify();
        verify(SALT_API_ROOT + "/run", "POST").bodyContains("fun=grains.append").bodyContains("ambari_agent_install").exactTimes(1).verify();
        verify(SALT_API_ROOT + "/run", "POST").bodyContains("fun=grains.append").bodyContains("ambari_agent").exactTimes(2).verify();
        verify(SALT_API_ROOT + "/run", "POST").bodyContains("fun=grains.append").bodyContains("ambari_server_install").exactTimes(1).verify();
        verify(SALT_API_ROOT + "/run", "POST").bodyContains("fun=grains.append").bodyContains("ambari_server").exactTimes(2).verify();
        verify(SALT_API_ROOT + "/run", "POST").bodyContains("fun=grains.append").bodyContains("recipes").exactTimes(2).verify();
        verify(SALT_API_ROOT + "/run", "POST").bodyContains("fun=grains.remove").bodyContains("recipes").exactTimes(2).verify();
        verify(SALT_API_ROOT + "/run", "POST").bodyContains("fun=jobs.active").atLeast(2).verify();

        verify(SALT_BOOT_ROOT + "/file", "POST").exactTimes(0).verify();
        verify(SALT_BOOT_ROOT + "/file/distribute", "POST").exactTimes(7).verify();
    }

    public void verifyGatewayCalls() {
        verify(SALT_BOOT_ROOT + "/salt/server/pillar/distribute", "POST")
                .bodyContains("\"path\":\"/gateway/init.sls\"")
                .bodyContains("\"ssoprovider\":\"/gateway-path/sso/api/v1/websso\"")
                .bodyContains("\"ports\":{\"SPARKHISTORYUI\":18080,\"HDFSUI\":50070,\"YARNUI\":8088,\"AMBARI\":8080,\"JOBHISTORYUI\":19888,"
                        + "\"HIVE_INTERACTIVE\":10501,\"BEACON\":25968,\"ATLAS\":21000,\"HIVE_CONFIG_NAME\":10001,\"RANGERUI\":6080,"
                        + "\"PROFILER-AGENT\":21900,\"ZEPPELIN\":9995,\"WEBHDFS\":50070}")
                .bodyContains("{\"name\":\"topology1\",\"exposed\":[\"AMBARI\"]}")
                .bodyContains("{\"name\":\"topology2\",\"exposed\":[\"AMBARI\",\"WEBHDFS\",\"HDFSUI\",\"YARNUI\",\"JOBHISTORYUI\""
                        + ",\"HIVE_CONFIG_NAME\",\"HIVE_INTERACTIVE\",\"ATLAS\",\"SPARKHISTORYUI\",\"ZEPPELIN\",\"RANGERUI\",\"PROFILER-AGENT\",\"BEACON\"]}")
                .verify();
    }

    public void verifyKerberosCalls(String clusterName, String kerberosAdmin, String kerberosPassword) {
        verify(SALT_API_ROOT + "/run", "POST")
                .bodyContains("fun=grains.append")
                .bodyContains("kerberos_server_master")
                .exactTimes(1)
                .verify();

        String principalKV = String.format("\"principal\": \"%s%s\"", kerberosAdmin, DEFAULT_KERBEROS_PRINCIPAL);
        String passwordKV = String.format("\"key\": \"%s\"", kerberosPassword);
        verify(AMBARI_API_ROOT + "/clusters/" + clusterName, "POST")
                .exactTimes(1)
                .bodyContains("\"alias\": \"kdc.admin.credential\"")
                .bodyContains(principalKV)
                .bodyContains(passwordKV)
                .verify();
    }
}
