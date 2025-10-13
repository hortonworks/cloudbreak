package com.sequenceiq.it.cloudbreak.testcase.e2e.sdx;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.tuple.Pair;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.cloud.HostGroupType;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;
import com.sequenceiq.it.cloudbreak.util.ssh.action.SshJClientActions;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

public class SdxSecurityTests extends PreconditionSdxE2ETest {

    private static final String HOST_CERT_VALIDITY_CMD =
            "sudo openssl x509 -text -noout -in /var/lib/cloudera-scm-agent/agent-cert/cm-auto-host_cert_chain.pem | grep -A 2 Validity";

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private SshJClientActions sshJClientActions;

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "there is a running Cloudbreak, and an SDX cluster in available state",
            when = "autotls cert rotation is called on the SDX cluster",
            then = "host certificates' validity should be changed on all hosts, the cluster should be up and running"
    )
    public void testSDXAutoTlsCertRotation(TestContext testContext) {
        String sdx = resourcePropertyProvider().getName();

        List<String> originalCertValidityOutput = new ArrayList<>();
        List<String> renewedCertValidityOutput = new ArrayList<>();

        testContext
                .given(sdx, SdxTestDto.class)
                .withCloudStorage()
                .when(sdxTestClient.create(), key(sdx))
                .await(SdxClusterStatusResponse.RUNNING, key(sdx))
                .awaitForHealthyInstances()
                .then((tc, testDto, client) -> {
                    Map<String, Pair<Integer, String>> certValidityCmdResultByIpsMap =
                            sshJClientActions.executeSshCommandOnHost(testDto.getResponse().getStackV4Response().getInstanceGroups(),
                                    List.of(HostGroupType.MASTER.getName(), HostGroupType.IDBROKER.getName()), HOST_CERT_VALIDITY_CMD, false);
                    originalCertValidityOutput.addAll(certValidityCmdResultByIpsMap.values().stream()
                            .map(Pair::getValue).collect(Collectors.toList()));
                    return testDto;
                })
                .when(sdxTestClient.rotateAutotlsCertificates(), key(sdx))
                .await(SdxClusterStatusResponse.CERT_ROTATION_IN_PROGRESS, key(sdx).withWaitForFlow(false))
                .await(SdxClusterStatusResponse.RUNNING, key(sdx))
                .awaitForHealthyInstances()
                .then((tc, testDto, client) -> {
                    Map<String, Pair<Integer, String>> certValidityCmdResultByIpsMap =
                            sshJClientActions.executeSshCommandOnHost(testDto.getResponse().getStackV4Response().getInstanceGroups(),
                                    List.of(HostGroupType.MASTER.getName(), HostGroupType.IDBROKER.getName()), HOST_CERT_VALIDITY_CMD, false);
                    renewedCertValidityOutput.addAll(certValidityCmdResultByIpsMap.entrySet().stream()
                            .map(e -> e.getValue().getValue()).collect(Collectors.toList()));
                    return testDto;
                })
                .then((tc, testDto, client) -> compareCertValidityOutputs(testDto, originalCertValidityOutput, renewedCertValidityOutput))
                // Currently audit endpoint is not configured for e2e tests, so audit test is commented out
                //.then(datalakeAuditGrpcServiceAssertion::rotateAutotlsCertificates)
                .validate();
    }

    private SdxTestDto compareCertValidityOutputs(SdxTestDto sdxTestDto, List<String> originalCertValidityOutput, List<String> renewedCertValidityOutput) {
        if (originalCertValidityOutput.equals(renewedCertValidityOutput)) {
            throw new TestFailException("Unsuccessful autotls certificates rotation: The certificate validity on hosts haven't changed!");
        }
        return sdxTestDto;
    }
}
