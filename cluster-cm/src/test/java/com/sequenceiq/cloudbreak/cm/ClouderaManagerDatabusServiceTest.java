package com.sequenceiq.cloudbreak.cm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.auth.altus.model.AltusCredential;
import com.sequenceiq.cloudbreak.auth.altus.service.AltusIAMService;
import com.sequenceiq.cloudbreak.auth.altus.service.RoleCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareCrnGenerator;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.workspace.model.User;

class ClouderaManagerDatabusServiceTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:accountId:user:name";

    private static final String SDX_STACK_CRN = "crn:cdp:sdx:us-west-1:1234:sdxcluster:mystack";

    private static final String INTERNAL_ACTOR_CRN = "crn:cdp:iam:us-west-1:altus:user:__internal__actor__";

    @InjectMocks
    private ClouderaManagerDatabusService underTest;

    @Mock
    private AltusIAMService iamService;

    @Mock
    private RoleCrnGenerator roleCrnGenerator;

    @Mock
    private RegionAwareCrnGenerator regionAwareCrnGenerator;

    private Stack stack;

    @BeforeEach
    void init() {
        MockitoAnnotations.initMocks(this);
        stack = new Stack();
        User creator = new User();
        creator.setUserCrn(USER_CRN);
        stack.setCreator(creator);
        stack.setResourceCrn(USER_CRN);
        Cluster cluster = new Cluster();
        cluster.setId(1L);
        stack.setCluster(cluster);
    }

    @Test
    void testGetAltusCredential() {
        // GIVEN
        AltusCredential credential = new AltusCredential("accessKey", "secretKey".toCharArray());
        when(iamService.generateMachineUserWithAccessKeyForLegacyCm(any(), any(), any(), any())).thenReturn(credential);
        when(roleCrnGenerator.getBuiltInWXMClusterAdminResourceRoleCrn(any())).thenReturn("resourceRoleCrn");
        when(regionAwareCrnGenerator.generateCrnString(any(), any(), any())).thenReturn("resourceCrn");
        // WHEN
        AltusCredential result = underTest.getAltusCredential(stack, SDX_STACK_CRN);
        // THEN
        assertEquals("secretKey", new String(result.getPrivateKey()));
    }

    @Test
    void testTrimAndReplace() {
        // GIVEN
        String rawPrivateKey = "BEGIN\nline1\nline2\nlastline";
        // WHEN
        String result = underTest.trimAndReplacePrivateKey(rawPrivateKey.toCharArray());
        // THEN
        assertEquals("BEGIN\\nline1\\nline2\\nlastline", result);
    }
}
