package com.sequenceiq.it.cloudbreak.v2.mock;

import java.util.Map;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.model.KerberosRequest;
import com.sequenceiq.cloudbreak.api.model.v2.AmbariV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.InstanceGroupV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.StackV2Request;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.CloudbreakITContextConstants;
import com.sequenceiq.it.cloudbreak.v2.AbstractStackCreationV2Test;
import com.sequenceiq.it.cloudbreak.v2.CloudbreakV2Constants;

public class MockStackCreationV2Test extends AbstractStackCreationV2Test {
    @BeforeClass
    @Parameters({"stackName", "mockPort", "sshPort"})
    public void configMockServer(String stackName, @Optional("9443") int mockPort, @Optional("2020") int sshPort) {
        IntegrationTestContext itContext = getItContext();
        Map<String, InstanceGroupV2Request> instanceGroupV2RequestMap = itContext.getContextParam(CloudbreakV2Constants.INSTANCEGROUP_MAP, Map.class);
        int numberOfServers = 0;
        for (InstanceGroupV2Request igr : instanceGroupV2RequestMap.values()) {
            numberOfServers += igr.getNodeCount();
        }
        StackCreationMock stackCreationMock = (StackCreationMock) applicationContext.getBean(
                StackCreationMock.NAME, mockPort, sshPort, numberOfServers);
        stackCreationMock.addSPIEndpoints();
        stackCreationMock.mockImageCatalogResponse(itContext);
        stackCreationMock.addSaltMappings();
        stackCreationMock.addAmbariMappings(stackName);
        itContext.putContextParam(CloudbreakV2Constants.MOCK_SERVER, stackCreationMock);
        itContext.putContextParam(CloudbreakITContextConstants.MOCK_INSTANCE_MAP, stackCreationMock.getInstanceMap());
    }

    @BeforeMethod
    @Parameters({"subnetCidr"})
    public void initNetwork(String subnetCidr) {
        createNetworkRequest(getItContext(), subnetCidr);
    }

    @Test
    public void testStackCreation() throws Exception {
        // GIVEN
        // WHEN
        super.testStackCreation();
        // THEN
        StackCreationMock stackCreationMock = getItContext().getContextParam(CloudbreakV2Constants.MOCK_SERVER, StackCreationMock.class);
        String stackName = getItContext().getContextParam(CloudbreakV2Constants.STACK_NAME);
        stackCreationMock.verifyCalls(stackName);
        StackV2Request stackV2Request = getItContext().getContextParam(CloudbreakV2Constants.STACK_CREATION_REQUEST, StackV2Request.class);
        AmbariV2Request ambariV2Request = stackV2Request.getCluster().getAmbari();
        if (ambariV2Request.getEnableSecurity()) {
            KerberosRequest kerberosRequest = ambariV2Request.getKerberos();
            stackCreationMock.verifyKerberosCalls(stackName, kerberosRequest.getAdmin(), kerberosRequest.getPassword());
        }
    }

    @AfterClass
    public void breakDown() {
        StackCreationMock stackCreationMock = getItContext().getContextParam(CloudbreakV2Constants.MOCK_SERVER, StackCreationMock.class);
        stackCreationMock.stop();
    }
}
