package com.sequenceiq.cloudbreak.cm;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.auth.altus.model.AltusCredential;
import com.sequenceiq.cloudbreak.auth.altus.service.AltusIAMService;
import com.sequenceiq.cloudbreak.auth.altus.service.RoleCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.workspace.model.User;

public class ClouderaManagerDatabusServiceTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:accountId:user:name";

    private static final String SDX_STACK_CRN = "crn:cdp:sdx:us-west-1:1234:sdxcluster:mystack";

    private static final String INTERNAL_ACTOR_CRN = "crn:cdp:iam:us-west-1:altus:user:__internal__actor__";

    @InjectMocks
    private ClouderaManagerDatabusService underTest;

    @Mock
    private AltusIAMService iamService;

    @Mock
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Mock
    private RegionAwareInternalCrnGenerator regionAwareInternalCrnGenerator;

    @Mock
    private RoleCrnGenerator roleCrnGenerator;

    @Mock
    private RegionAwareCrnGenerator regionAwareCrnGenerator;

    private Stack stack;

    @Before
    public void setUp() {
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
    public void testGetAltusCredential() {
        // GIVEN
        AltusCredential credential = new AltusCredential("accessKey", "secretKey".toCharArray());
        when(iamService.generateMachineUserWithAccessKeyForLegacyCm(any(), any(), any(), any())).thenReturn(credential);
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString()).thenReturn(INTERNAL_ACTOR_CRN);
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        when(roleCrnGenerator.getBuiltInWXMClusterAdminResourceRoleCrn(any())).thenReturn("resourceRoleCrn");
        when(regionAwareCrnGenerator.generateCrnString(any(), any(), any())).thenReturn("resourceCrn");
        // WHEN
        AltusCredential result = underTest.getAltusCredential(stack, SDX_STACK_CRN);
        // THEN
        assertEquals("secretKey", new String(result.getPrivateKey()));
    }

    @Test
    public void testTrimAndReplace() {
        // GIVEN
        String rawPrivateKey = "BEGIN\nline1\nline2\nlastline";
        // WHEN
        String result = underTest.trimAndReplacePrivateKey(rawPrivateKey.toCharArray());
        // THEN
        assertEquals("BEGIN\\nline1\\nline2\\nlastline", result);
    }
}
