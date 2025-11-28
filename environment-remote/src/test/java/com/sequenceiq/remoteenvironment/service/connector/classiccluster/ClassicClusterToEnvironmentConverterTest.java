package com.sequenceiq.remoteenvironment.service.connector.classiccluster;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import com.cloudera.thunderhead.service.environments2api.model.Application;
import com.cloudera.thunderhead.service.environments2api.model.Environment;
import com.cloudera.thunderhead.service.environments2api.model.Instance;
import com.cloudera.thunderhead.service.environments2api.model.KerberosInfo;
import com.cloudera.thunderhead.service.environments2api.model.PrivateDatalakeDetails;
import com.cloudera.thunderhead.service.environments2api.model.PvcEnvironmentDetails;
import com.cloudera.thunderhead.service.environments2api.model.Service;
import com.cloudera.thunderhead.service.environments2api.model.ServiceEndPoint;
import com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto;

class ClassicClusterToEnvironmentConverterTest {
    private final ClassicClusterToEnvironmentConverter underTest = new ClassicClusterToEnvironmentConverter();

    @Test
    void testCreateEnvironmentConvertsAllFieldsCorrectly() {
        // Kerberos
        OnPremisesApiProto.KerberosInfo kerberosProto = OnPremisesApiProto.KerberosInfo.newBuilder()
                .setKerberized(true)
                .setKdcType("MIT")
                .setKdcHost("kdc.example.com")
                .setKdcHostIp("10.0.0.1")
                .setKerberosRealm("EXAMPLE.COM")
                .build();

        // Service endpoint
        OnPremisesApiProto.ServiceEndpoint endpoint = OnPremisesApiProto.ServiceEndpoint.newBuilder()
                .setUri("http://svc")
                .setHost("svc-host")
                .setPort(9876)
                .build();

        // Service
        OnPremisesApiProto.Service serviceProto = OnPremisesApiProto.Service.newBuilder()
                .setType("HDFS")
                .putConfig("fs.defaultFS", "hdfs://n")
                .addEndpoints(endpoint)
                .build();

        // Application
        OnPremisesApiProto.Application applicationProto = OnPremisesApiProto.Application.newBuilder()
                .setName("APP")
                .putConfig("key", "value")
                .putServices("svc", serviceProto)
                .build();

        // Datalake details with one instance
        OnPremisesApiProto.OnPremDatalakeDetails datalakeProto = OnPremisesApiProto.OnPremDatalakeDetails.newBuilder()
                .setDatalakeName("datalake1")
                .setCreationTimeEpochMillis(123456L)
                .setCmFqdn("cm.example.com")
                .setCmIp("1.2.3.4")
                .setCmServerId("server-id")
                .setEnableRangerRaz(true)
                .setStatus(OnPremisesApiProto.DatalakeStatus.Value.AVAILABLE)
                .setKerberosInfo(kerberosProto)
                .addInstances(OnPremisesApiProto.Instance.newBuilder()
                        .setFqdn("host1")
                        .setId("id1")
                        .setPrivateIp("10.0.0.2")
                        .build())
                .build();

        // Environment details with applications map
        OnPremisesApiProto.OnPremEnvironmentDetails envDetails = OnPremisesApiProto.OnPremEnvironmentDetails.newBuilder()
                .setCmHost("cmHost")
                .setKnoxGatewayUrl("https://knox")
                .setClouderaRuntimeVersion("runtimeVersion")
                .setCmVersion("cmVersion")
                .putApplications("app1", applicationProto)
                .setOnPremDatalakeDetails(datalakeProto)
                .build();

        long lastCreateTime = 1600000000000L;
        OnPremisesApiProto.Cluster cluster = OnPremisesApiProto.Cluster.newBuilder()
                .setName("clusterName")
                .setClusterCrn("clusterCrn")
                .setLastCreateTime(lastCreateTime)
                .setCmClusterUuid("cm-uuid")
                .setOnPremEnvironmentDetails(envDetails)
                .build();

        Environment env = underTest.createEnvironment(cluster);

        // Basic fields
        assertThat(env.getEnvironmentName()).isEqualTo("clusterName");
        assertThat(env.getCrn()).isEqualTo("clusterCrn");
        assertThat(env.getCreated()).isEqualTo(Instant.ofEpochMilli(lastCreateTime).atOffset(ZoneOffset.UTC));
        assertThat(env.getStatus()).isEqualTo("AVAILABLE");
        assertThat(env.getClouderaManagerClusterUuid()).isEqualTo("cm-uuid");
        assertThat(env.getClouderaManagerVersion()).isEqualTo("cmVersion");
        assertThat(env.getCdpRuntimeVersion()).isEqualTo("runtimeVersion");

        // PVC / applications
        PvcEnvironmentDetails pvc = env.getPvcEnvironmentDetails();
        assertThat(pvc.getCmHost()).isEqualTo("cmHost");
        assertThat(pvc.getKnoxGatewayUrl()).isEqualTo("https://knox");
        assertThat(pvc.getApplications()).containsKey("app1");

        Application app = pvc.getApplications().get("app1");
        assertThat(app.getName()).isEqualTo("APP");
        assertThat(app.getConfig()).containsEntry("key", "value");
        assertThat(app.getServices()).containsKey("svc");

        Service svc = app.getServices().get("svc");
        assertThat(svc.getType()).isEqualTo("HDFS");
        assertThat(svc.getConfig()).containsEntry("fs.defaultFS", "hdfs://n");
        assertThat(svc.getEndpoints()).hasSize(1);
        ServiceEndPoint ep = svc.getEndpoints().get(0);
        assertThat(ep.getUri()).isEqualTo("http://svc");
        assertThat(ep.getHost()).isEqualTo("svc-host");
        assertThat(ep.getPort()).isEqualTo(9876);

        // Private datalake details and kerberos
        PrivateDatalakeDetails pdl = pvc.getPrivateDatalakeDetails();
        assertThat(pdl.getDatalakeName()).isEqualTo("datalake1");
        assertThat(pdl.getCreationTimeEpochMillis()).isEqualTo(123456L);
        assertThat(pdl.getCmFQDN()).isEqualTo("cm.example.com");
        assertThat(pdl.getCmIP()).isEqualTo("1.2.3.4");
        assertThat(pdl.getCmServerId()).isEqualTo("server-id");
        assertThat(pdl.getEnableRangerRaz()).isTrue();
        assertThat(pdl.getStatus()).isEqualTo(PrivateDatalakeDetails.StatusEnum.AVAILABLE);

        KerberosInfo kerberos = pdl.getKerberosInfo();
        assertThat(kerberos.getKerberized()).isTrue();
        assertThat(kerberos.getKdcType()).isEqualTo("MIT");
        assertThat(kerberos.getKdcHost()).isEqualTo("kdc.example.com");
        assertThat(kerberos.getKdcHostIp()).isEqualTo("10.0.0.1");
        assertThat(kerberos.getKerberosRealm()).isEqualTo("EXAMPLE.COM");

        // Instances
        assertThat(pdl.getInstances()).hasSize(1);
        Instance inst = pdl.getInstances().get(0);
        assertThat(inst.getDiscoveryFQDN()).isEqualTo("host1");
        assertThat(inst.getInstanceId()).isEqualTo("id1");
        assertThat(inst.getPrivateIp()).isEqualTo("10.0.0.2");
    }
}
