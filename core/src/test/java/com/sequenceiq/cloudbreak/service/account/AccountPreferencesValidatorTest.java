package com.sequenceiq.cloudbreak.service.account;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.domain.AccountPreferences;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.user.UserDetailsService;
import com.sequenceiq.cloudbreak.service.user.UserFilterField;

@RunWith(MockitoJUnitRunner.class)
public class AccountPreferencesValidatorTest {

    public static final String EMPTY_STRING = "";

    @Mock
    private Stack stack;

    @Mock
    private AccountPreferences preferences;

    @Mock
    private AccountPreferencesService accountPreferencesService;

    @Mock
    private StackService stackService;

    @Mock
    private UserDetailsService userDetailsService;

    @InjectMocks
    private AccountPreferencesValidator underTest;

    @Before
    public void setUp() {
        when(stack.getAccount()).thenReturn("");
        when(accountPreferencesService.getByAccount("")).thenReturn(preferences);
        when(preferences.getMaxNumberOfNodesPerCluster()).thenReturn(0L);
        when(preferences.getMaxNumberOfClusters()).thenReturn(0L);
        when(preferences.getMaxNumberOfClustersPerUser()).thenReturn(0L);
        when(preferences.getClusterTimeToLive()).thenReturn(0L);
        when(preferences.getUserTimeToLive()).thenReturn(0L);
        when(preferences.getAllowedInstanceTypes()).thenReturn(new ArrayList<>());
    }

    @Test
    public void testValidateShouldNotThrowExceptionWhenPreferencesShouldNotBeValidated() throws Exception {
        underTest.validate(stack, EMPTY_STRING, EMPTY_STRING);
    }

    @Test(expected = AccountPreferencesValidationFailed.class)
    public void testValidateShouldThrowExceptionWhenTheStackNodeCountIsGreaterThanTheAccountMaximum() throws Exception {
        when(preferences.getMaxNumberOfNodesPerCluster()).thenReturn(4L);
        when(stack.getFullNodeCount()).thenReturn(5);

        underTest.validate(stack, EMPTY_STRING, EMPTY_STRING);
    }

    @Test
    public void testValidateShouldNotThrowExceptionWhenTheStackNodeCountIsLessThanTheAccountMaximum() throws Exception {
        when(preferences.getMaxNumberOfNodesPerCluster()).thenReturn(4L);
        when(stack.getFullNodeCount()).thenReturn(3);

        underTest.validate(stack, EMPTY_STRING, EMPTY_STRING);
    }

    @Test(expected = AccountPreferencesValidationFailed.class)
    public void testValidateShouldThrowExceptionWhenTheNumberOfClusterInAccountIsGreaterOrEqualThanTheAccountMaximum() throws Exception {
        when(preferences.getMaxNumberOfClusters()).thenReturn(400L);
        Set stacks = Mockito.mock(Set.class);
        when(stackService.retrieveAccountStacks(anyString())).thenReturn(stacks);
        when(stacks.size()).thenReturn(400);

        underTest.validate(stack, EMPTY_STRING, EMPTY_STRING);
    }

    @Test
    public void testValidateShouldNotThrowExceptionWhenTheNumberOfClusterInAccountIsLessThanTheAccountMaximum() throws Exception {
        when(preferences.getMaxNumberOfClusters()).thenReturn(400L);
        Set stacks = Mockito.mock(Set.class);
        when(stackService.retrieveAccountStacks(anyString())).thenReturn(stacks);
        when(stacks.size()).thenReturn(200);

        underTest.validate(stack, EMPTY_STRING, EMPTY_STRING);
    }

    @Test(expected = AccountPreferencesValidationFailed.class)
    public void testValidateShouldThrowExceptionWhenTheNumberOfClusterInAccountForAUserIsGreaterOrEqualThanTheAccountMaximum() throws Exception {
        when(preferences.getMaxNumberOfClustersPerUser()).thenReturn(4L);
        Set stacks = Mockito.mock(Set.class);
        when(stackService.retrieveOwnerStacks(anyString())).thenReturn(stacks);
        when(stacks.size()).thenReturn(4);

        underTest.validate(stack, EMPTY_STRING, EMPTY_STRING);
    }

    @Test
    public void testValidateShouldNotThrowExceptionWhenTheNumberOfClusterInAccountForAUserIsLessThanTheAccountMaximum() throws Exception {
        when(preferences.getMaxNumberOfClustersPerUser()).thenReturn(4L);
        Set stacks = Mockito.mock(Set.class);
        when(stackService.retrieveOwnerStacks(anyString())).thenReturn(stacks);
        when(stacks.size()).thenReturn(2);

        underTest.validate(stack, EMPTY_STRING, EMPTY_STRING);
    }

    @Test(expected = AccountPreferencesValidationFailed.class)
    public void testValidateShouldThrowExceptionWhenTheUserDemoTimeExpired() throws Exception {
        Calendar calendar = Calendar.getInstance();
        calendar.roll(Calendar.HOUR_OF_DAY, -1);
        when(preferences.getUserTimeToLive()).thenReturn(40000L);
        CbUser cbUser = Mockito.mock(CbUser.class);
        when(userDetailsService.getDetails(anyString(), any(UserFilterField.class))).thenReturn(cbUser);
        when(cbUser.getCreated()).thenReturn(calendar.getTime());

        underTest.validate(stack, EMPTY_STRING, EMPTY_STRING);
    }

    @Test
    public void testValidateShouldNotThrowExceptionWhenTheUserDemoTimeHasNotExpiredYet() throws Exception {
        Calendar calendar = Calendar.getInstance();
        calendar.roll(Calendar.MINUTE, -1);
        when(preferences.getUserTimeToLive()).thenReturn(65000L);
        CbUser cbUser = Mockito.mock(CbUser.class);
        when(userDetailsService.getDetails(anyString(), any(UserFilterField.class))).thenReturn(cbUser);
        when(cbUser.getCreated()).thenReturn(calendar.getTime());

        underTest.validate(stack, EMPTY_STRING, EMPTY_STRING);
    }

    @Test(expected = AccountPreferencesValidationFailed.class)
    public void testValidateShouldThrowExceptionWhenTheStackContainsNotAllowedInstanceTypes() throws Exception {
        String n1St4Type = "n1-standard-4";
        List<String> allowedInstanceTypes = Arrays.asList(n1St4Type, "n1-standard-8", "n1-standard-16");
        when(preferences.getAllowedInstanceTypes()).thenReturn(allowedInstanceTypes);
        InstanceGroup cbgateway = Mockito.mock(InstanceGroup.class, Mockito.RETURNS_DEEP_STUBS);
        InstanceGroup master = Mockito.mock(InstanceGroup.class, Mockito.RETURNS_DEEP_STUBS);
        InstanceGroup slave = Mockito.mock(InstanceGroup.class, Mockito.RETURNS_DEEP_STUBS);
        when(cbgateway.getTemplate().getInstanceType()).thenReturn(n1St4Type);
        when(master.getTemplate().getInstanceType()).thenReturn(n1St4Type);
        when(slave.getTemplate().getInstanceType()).thenReturn("n1-standard-32");
        when(stack.getInstanceGroups()).thenReturn(Sets.newHashSet(cbgateway, master, slave));

        underTest.validate(stack, EMPTY_STRING, EMPTY_STRING);
    }

    @Test
    public void testValidateShouldNotThrowExceptionWhenTheStackContainsOnlyAllowedInstanceTypes() throws Exception {
        String n1St4Type = "n1-standard-4";
        String n1St6Type = "n1-standard-8";
        List<String> allowedInstanceTypes = Arrays.asList(n1St4Type, n1St6Type, "n1-standard-16");
        when(preferences.getAllowedInstanceTypes()).thenReturn(allowedInstanceTypes);
        InstanceGroup cbgateway = Mockito.mock(InstanceGroup.class, Mockito.RETURNS_DEEP_STUBS);
        InstanceGroup master = Mockito.mock(InstanceGroup.class, Mockito.RETURNS_DEEP_STUBS);
        InstanceGroup slave = Mockito.mock(InstanceGroup.class, Mockito.RETURNS_DEEP_STUBS);
        when(cbgateway.getTemplate().getInstanceType()).thenReturn(n1St4Type);
        when(master.getTemplate().getInstanceType()).thenReturn(n1St4Type);
        when(slave.getTemplate().getInstanceType()).thenReturn(n1St6Type);
        when(stack.getInstanceGroups()).thenReturn(Sets.newHashSet(cbgateway, master, slave));

        underTest.validate(stack, EMPTY_STRING, EMPTY_STRING);
    }
}