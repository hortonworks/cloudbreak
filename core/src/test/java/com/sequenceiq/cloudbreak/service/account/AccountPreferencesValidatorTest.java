package com.sequenceiq.cloudbreak.service.account;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.common.service.user.UserFilterField;
import com.sequenceiq.cloudbreak.domain.AccountPreferences;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.user.UserDetailsService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Set;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AccountPreferencesValidatorTest {

    private static final String EMPTY_STRING = "";

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

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
        when(accountPreferencesService.getByAccount("")).thenReturn(preferences);
        when(preferences.getMaxNumberOfNodesPerCluster()).thenReturn(0L);
        when(preferences.getMaxNumberOfClusters()).thenReturn(0L);
        when(preferences.getMaxNumberOfClustersPerUser()).thenReturn(0L);
        when(preferences.getUserTimeToLive()).thenReturn(0L);
        when(preferences.getAllowedInstanceTypes()).thenReturn(new ArrayList<>());
    }

    @Test
    public void testValidateShouldNotThrowExceptionWhenPreferencesShouldNotBeValidated() throws AccountPreferencesValidationException {
        underTest.validate(stack, EMPTY_STRING, EMPTY_STRING);
    }

    @Test
    public void testValidateShouldThrowExceptionWhenTheStackNodeCountIsGreaterThanTheAccountMaximum() throws AccountPreferencesValidationException {
        when(preferences.getMaxNumberOfNodesPerCluster()).thenReturn(4L);
        when(stack.getFullNodeCount()).thenReturn(5);
        thrown.expect(AccountPreferencesValidationException.class);
        thrown.expectMessage("Cluster with maximum '4' instances could be created within this account!");

        underTest.validate(stack, EMPTY_STRING, EMPTY_STRING);
    }

    @Test
    public void testValidateShouldNotThrowExceptionWhenTheStackNodeCountIsLessThanTheAccountMaximum() throws AccountPreferencesValidationException {
        when(preferences.getMaxNumberOfNodesPerCluster()).thenReturn(4L);
        when(stack.getFullNodeCount()).thenReturn(3);

        underTest.validate(stack, EMPTY_STRING, EMPTY_STRING);
    }

    @Test
    public void testValidateShouldThrowExceptionWhenTheNumberOfClusterInAccountIsGreaterOrEqualThanTheAccountMaximum()
            throws AccountPreferencesValidationException {
        when(preferences.getMaxNumberOfClusters()).thenReturn(400L);
        Set stacks = Mockito.mock(Set.class);
        when(stackService.retrieveAccountStacks(anyString())).thenReturn(stacks);
        when(stacks.size()).thenReturn(400);
        thrown.expect(AccountPreferencesValidationException.class);
        thrown.expectMessage("No more cluster could be created! The number of clusters exceeded the account's limit(400)!");

        underTest.validate(stack, EMPTY_STRING, EMPTY_STRING);
    }

    @Test
    public void testValidateShouldNotThrowExceptionWhenTheNumberOfClusterInAccountIsLessThanTheAccountMaximum() throws AccountPreferencesValidationException {
        when(preferences.getMaxNumberOfClusters()).thenReturn(400L);
        Set stacks = Mockito.mock(Set.class);
        when(stackService.retrieveAccountStacks(anyString())).thenReturn(stacks);
        when(stacks.size()).thenReturn(200);

        underTest.validate(stack, EMPTY_STRING, EMPTY_STRING);
    }

    @Test
    public void testValidateShouldThrowExceptionWhenTheNumberOfClusterInAccountForAUserIsGreaterOrEqualThanTheAccountMaximum()
            throws AccountPreferencesValidationException {
        when(preferences.getMaxNumberOfClustersPerUser()).thenReturn(4L);
        Set stacks = Mockito.mock(Set.class);
        when(stackService.retrieveOwnerStacks(anyString())).thenReturn(stacks);
        when(stacks.size()).thenReturn(4);
        thrown.expect(AccountPreferencesValidationException.class);
        thrown.expectMessage("No more cluster could be created! The number of clusters exceeded the user's limit(4)!");

        underTest.validate(stack, EMPTY_STRING, EMPTY_STRING);
    }

    @Test
    public void testValidateShouldNotThrowExceptionWhenTheNumberOfClusterInAccountForAUserIsLessThanTheAccountMaximum()
            throws AccountPreferencesValidationException {
        when(preferences.getMaxNumberOfClustersPerUser()).thenReturn(4L);
        Set stacks = Mockito.mock(Set.class);
        when(stackService.retrieveOwnerStacks(anyString())).thenReturn(stacks);
        when(stacks.size()).thenReturn(2);

        underTest.validate(stack, EMPTY_STRING, EMPTY_STRING);
    }

    @Test
    public void testValidateShouldThrowExceptionWhenTheUserDemoTimeExpired() throws AccountPreferencesValidationException {
        Calendar calendar = Calendar.getInstance();
        calendar.roll(Calendar.HOUR_OF_DAY, -1);
        when(preferences.getUserTimeToLive()).thenReturn(40000L);
        IdentityUser identityUser = Mockito.mock(IdentityUser.class);
        when(userDetailsService.getDetails(anyString(), any(UserFilterField.class))).thenReturn(identityUser);
        when(identityUser.getCreated()).thenReturn(calendar.getTime());
        thrown.expect(AccountPreferencesValidationException.class);
        thrown.expectMessage("The user demo time is expired!");

        underTest.validate(stack, EMPTY_STRING, EMPTY_STRING);
    }

    @Test
    public void testValidateShouldNotThrowExceptionWhenTheUserDemoTimeHasNotExpiredYet() throws AccountPreferencesValidationException {
        Calendar calendar = Calendar.getInstance();
        calendar.roll(Calendar.MINUTE, -1);
        when(preferences.getUserTimeToLive()).thenReturn(65000L);
        IdentityUser identityUser = Mockito.mock(IdentityUser.class);
        when(userDetailsService.getDetails(anyString(), any(UserFilterField.class))).thenReturn(identityUser);
        when(identityUser.getCreated()).thenReturn(calendar.getTime());

        underTest.validate(stack, EMPTY_STRING, EMPTY_STRING);
    }

    @Test
    public void testValidateShouldThrowExceptionWhenTheStackContainsNotAllowedInstanceTypes() throws AccountPreferencesValidationException {
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
        thrown.expect(AccountPreferencesValidationException.class);
        thrown.expectMessage("The 'n1-standard-32' instance type isn't allowed within the account!");

        underTest.validate(stack, EMPTY_STRING, EMPTY_STRING);
    }

    @Test
    public void testValidateShouldNotThrowExceptionWhenTheStackContainsOnlyAllowedInstanceTypes() throws AccountPreferencesValidationException {
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