package com.sequenceiq.freeipa.service.freeipa.user;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.sequenceiq.cloudbreak.auth.crn.CrnTestUtil;
import com.sequenceiq.freeipa.service.freeipa.user.conversion.UserMetadataConverter;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.googlecode.jsonrpc4j.JsonRpcClientException;
import com.sequenceiq.cloudbreak.client.RPCResponse;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.FreeIpaErrorCodes;
import com.sequenceiq.freeipa.client.model.Group;
import com.sequenceiq.freeipa.client.model.User;
import com.sequenceiq.freeipa.configuration.BatchPartitionSizeProperties;
import com.sequenceiq.freeipa.service.freeipa.user.model.FmsGroup;
import com.sequenceiq.freeipa.service.freeipa.user.model.FmsUser;
import com.sequenceiq.freeipa.util.ThreadInterruptChecker;

@ExtendWith(MockitoExtension.class)
class UserSyncOperationsTest {

    private static final String ACCOUNT_ID = UUID.randomUUID().toString();

    private static final int MAX_SUBJECTS_PER_REQUEST = 10;

    @Mock
    private FreeIpaClient freeIpaClient;

    @Mock
    private BatchPartitionSizeProperties batchPartitionSizeProperties;

    @Mock
    private ThreadInterruptChecker interruptChecker;

    @Mock
    private UserMetadataConverter userMetadataConverter;

    @InjectMocks
    private UserSyncOperations underTest;

    @BeforeEach
    public void init() {
        ReflectionTestUtils.setField(underTest, "maxSubjectsPerRequest", MAX_SUBJECTS_PER_REQUEST);
    }

    @Test
    public void testAddUsersToGroupsPartitionsRequests() throws Exception {
        Multimap<String, String> groupMapping = setupGroupMapping(5, MAX_SUBJECTS_PER_REQUEST * 2);
        when(batchPartitionSizeProperties.getByOperation("group_add_member")).thenReturn(3);
        Multimap<String, String> warnings = ArrayListMultimap.create();

        underTest.addUsersToGroups(true, freeIpaClient, groupMapping, warnings::put);

        ArgumentCaptor<List<Object>> operationsCaptor = ArgumentCaptor.forClass(List.class);
        verify(freeIpaClient).callBatch(any(), operationsCaptor.capture(), eq(3), eq(Set.of()), any());
        assertTrue(warnings.isEmpty());
        List<Object> operations = operationsCaptor.getValue();
        assertEquals(10, operations.size());
        operations.forEach(op -> {
            Map<String, Object> operation = (Map<String, Object>) op;
            assertEquals("group_add_member", operation.get("method"));
            List<Object> params = (List<Object>) operation.get("params");
            Map<String, Object> asdf = (Map<String, Object>) params.get(1);
            assertTrue(asdf.containsKey("user"));
            Collection<String> users = (Collection<String>) asdf.get("user");
            assertEquals(MAX_SUBJECTS_PER_REQUEST, users.size());
        });
        verify(interruptChecker, times(10)).throwTimeoutExIfInterrupted();
    }

    @Test
    public void testRemoveUsersFromGroupsPartitionsRequests() throws Exception {
        Multimap<String, String> groupMapping = setupGroupMapping(5, MAX_SUBJECTS_PER_REQUEST * 2);
        when(batchPartitionSizeProperties.getByOperation("group_remove_member")).thenReturn(3);
        Multimap<String, String> warnings = ArrayListMultimap.create();

        underTest.removeUsersFromGroups(true, freeIpaClient, groupMapping, warnings::put);

        assertTrue(warnings.isEmpty());
        ArgumentCaptor<List<Object>> operationsCaptor = ArgumentCaptor.forClass(List.class);
        verify(freeIpaClient).callBatch(any(), operationsCaptor.capture(), eq(3), eq(Set.of()), any());
        assertTrue(warnings.isEmpty());
        List<Object> operations = operationsCaptor.getValue();
        assertEquals(10, operations.size());
        operations.forEach(op -> {
            Map<String, Object> operation = (Map<String, Object>) op;
            assertEquals("group_remove_member", operation.get("method"));
            List<Object> params = (List<Object>) operation.get("params");
            Map<String, Object> asdf = (Map<String, Object>) params.get(1);
            assertTrue(asdf.containsKey("user"));
            Collection<String> users = (Collection<String>) asdf.get("user");
            assertEquals(MAX_SUBJECTS_PER_REQUEST, users.size());
        });
        verify(interruptChecker, times(10)).throwTimeoutExIfInterrupted();
    }

    @Test
    public void testRemoveUsersFromGroupsNullMembersInResponse() throws Exception {
        Multimap<String, String> groupMapping = setupGroupMapping(1, 1);
        when(batchPartitionSizeProperties.getByOperation("group_remove_member")).thenReturn(3);
        Multimap<String, String> warnings = ArrayListMultimap.create();

        underTest.removeUsersFromGroups(true, freeIpaClient, groupMapping, warnings::put);

        assertTrue(warnings.isEmpty());
        ArgumentCaptor<List<Object>> operationsCaptor = ArgumentCaptor.forClass(List.class);
        verify(freeIpaClient).callBatch(any(), operationsCaptor.capture(), eq(3), eq(Set.of()), any());
        List<Object> operations = operationsCaptor.getValue();
        assertEquals(1, operations.size());
        operations.forEach(op -> {
            Map<String, Object> operation = (Map<String, Object>) op;
            assertEquals("group_remove_member", operation.get("method"));
            List<Object> params = (List<Object>) operation.get("params");
            Map<String, Object> asdf = (Map<String, Object>) params.get(1);
            assertTrue(asdf.containsKey("user"));
            Collection<String> users = (Collection<String>) asdf.get("user");
            assertEquals(1, users.size());
        });
        verify(interruptChecker, times(1)).throwTimeoutExIfInterrupted();
    }

    @Test
    public void testAddGroupsBatch() throws FreeIpaClientException, TimeoutException {
        FmsGroup g1 = new FmsGroup().withName(UserSyncConstants.CDP_USERSYNC_INTERNAL_GROUP);
        FmsGroup g2 = new FmsGroup().withName("group_1");
        FmsGroup g3 = new FmsGroup().withName("group_2");
        Set<FmsGroup> groups = Set.of(g1, g2, g3);
        Multimap<String, String> warnings = ArrayListMultimap.create();
        when(batchPartitionSizeProperties.getByOperation("group_add")).thenReturn(3);

        underTest.addGroups(true, freeIpaClient, groups, warnings::put);

        ArgumentCaptor<List<Object>> operationsCaptor = ArgumentCaptor.forClass(List.class);
        verify(freeIpaClient, times(2))
                .callBatch(any(), operationsCaptor.capture(), eq(3), eq(Set.of(FreeIpaErrorCodes.DUPLICATE_ENTRY)), any());
        List<List<Object>> captorAllValues = operationsCaptor.getAllValues();
        List<Object> posixOps = captorAllValues.get(0);
        assertEquals(2, posixOps.size());
        posixOps.forEach(op -> {
            Map<String, Object> operation = (Map<String, Object>) op;
            assertEquals("group_add", operation.get("method"));
            List<Object> params = (List<Object>) operation.get("params");
            List<String> groupName = (List<String>) params.get(0);
            assertTrue(groupName.get(0).startsWith("group_"));
            assertTrue(((Map<?, ?>) params.get(1)).isEmpty());
        });
        List<Object> nonPosixOps = captorAllValues.get(1);
        assertEquals(1, nonPosixOps.size());
        nonPosixOps.forEach(op -> {
            Map<String, Object> operation = (Map<String, Object>) op;
            assertEquals("group_add", operation.get("method"));
            List<Object> params = (List<Object>) operation.get("params");
            List<String> groupName = (List<String>) params.get(0);
            assertEquals(UserSyncConstants.CDP_USERSYNC_INTERNAL_GROUP, groupName.get(0));
            assertTrue(((Map<String, Boolean>) params.get(1)).get("nonposix"));
        });
        verify(interruptChecker, times(3)).throwTimeoutExIfInterrupted();
    }

    @Test
    public void testAddUsersBatch() throws FreeIpaClientException, TimeoutException {
        Multimap<String, String> warnings = ArrayListMultimap.create();
        when(userMetadataConverter.toUserMetadataJson(anyString(), anyLong())).thenCallRealMethod();
        FmsUser user1 = new FmsUser().withName("user_1").withFirstName("User_1").withLastName("Test1")
                .withState(FmsUser.State.ENABLED).withCrn(createUserCrn());
        FmsUser user2 = new FmsUser().withName("user_2").withFirstName("User_2").withLastName("Test2")
                .withState(FmsUser.State.DISABLED).withCrn(createUserCrn());
        Set<FmsUser> users = Set.of(user1, user2);
        when(batchPartitionSizeProperties.getByOperation("user_add")).thenReturn(3);

        underTest.addUsers(true, freeIpaClient, users, warnings::put);

        assertTrue(warnings.isEmpty());
        ArgumentCaptor<List<Object>> operationsCaptor = ArgumentCaptor.forClass(List.class);
        verify(freeIpaClient).callBatch(any(), operationsCaptor.capture(), eq(3), eq(Set.of(FreeIpaErrorCodes.DUPLICATE_ENTRY)), any());
        List<Object> operations = operationsCaptor.getValue();
        assertEquals(2, operations.size());
        Object user1oper = operations.stream().filter(op -> {
            Map<String, Object> operation = (Map<String, Object>) op;
            List<Object> params = (List<Object>) operation.get("params");
            return "user_1".equals(((List<String>) params.get(0)).get(0));
        }).findFirst().get();
        Map<String, Object> operation1 = (Map<String, Object>) user1oper;
        List<Object> params1 = (List<Object>) operation1.get("params");
        Map<String, Object> attribs1 = (Map<String, Object>) params1.get(1);
        assertEquals("User_1", attribs1.get("givenname"));
        assertEquals("Test1", attribs1.get("sn"));
        assertTrue(((String) attribs1.get("title")).contains(user1.getCrn()));
        assertTrue(((List<String>) attribs1.get("setattr")).contains("nsAccountLock=false"));

        Object user2oper = operations.stream().filter(op -> {
            Map<String, Object> operation = (Map<String, Object>) op;
            List<Object> params = (List<Object>) operation.get("params");
            return "user_2".equals(((List<String>) params.get(0)).get(0));
        }).findFirst().get();
        Map<String, Object> operation2 = (Map<String, Object>) user2oper;
        List<Object> params2 = (List<Object>) operation2.get("params");
        Map<String, Object> attribs2 = (Map<String, Object>) params2.get(1);
        assertEquals("User_2", attribs2.get("givenname"));
        assertEquals("Test2", attribs2.get("sn"));
        assertTrue(((String) attribs2.get("title")).contains(user2.getCrn()));
        assertTrue(((List<String>) attribs2.get("setattr")).contains("nsAccountLock=true"));
        verify(interruptChecker, times(2)).throwTimeoutExIfInterrupted();
    }

    @Test
    public void testDisableUsersBatch() throws FreeIpaClientException, TimeoutException {
        Multimap<String, String> warnings = ArrayListMultimap.create();
        Set<String> users = Set.of("user1", "user2");
        when(batchPartitionSizeProperties.getByOperation("user_disable")).thenReturn(3);

        underTest.disableUsers(true, freeIpaClient, users, warnings::put);

        assertTrue(warnings.isEmpty());
        ArgumentCaptor<List<Object>> operationsCaptor = ArgumentCaptor.forClass(List.class);
        verify(freeIpaClient).callBatch(any(), operationsCaptor.capture(), eq(3), eq(Set.of(FreeIpaErrorCodes.ALREADY_INACTIVE)), any());
        List<Object> operations = operationsCaptor.getValue();
        assertEquals(2, operations.size());
        operations.forEach(op -> {
            Map<String, Object> operation = (Map<String, Object>) op;
            assertEquals("user_disable", operation.get("method"));
            List<Object> params = (List<Object>) operation.get("params");
            assertTrue(((List<String>) params.get(0)).get(0).startsWith("user"));
            assertTrue(((Map<String, Object>) params.get(1)).isEmpty());
        });
        verify(interruptChecker, times(2)).throwTimeoutExIfInterrupted();
    }

    @Test
    public void testEnableUsersBatch() throws FreeIpaClientException, TimeoutException {
        Multimap<String, String> warnings = ArrayListMultimap.create();
        Set<String> users = Set.of("user1", "user2");
        when(batchPartitionSizeProperties.getByOperation("user_enable")).thenReturn(3);

        underTest.enableUsers(true, freeIpaClient, users, warnings::put);

        assertTrue(warnings.isEmpty());
        ArgumentCaptor<List<Object>> operationsCaptor = ArgumentCaptor.forClass(List.class);
        verify(freeIpaClient).callBatch(any(), operationsCaptor.capture(), eq(3), eq(Set.of(FreeIpaErrorCodes.ALREADY_ACTIVE)), any());
        List<Object> operations = operationsCaptor.getValue();
        assertEquals(2, operations.size());
        operations.forEach(op -> {
            Map<String, Object> operation = (Map<String, Object>) op;
            assertEquals("user_enable", operation.get("method"));
            List<Object> params = (List<Object>) operation.get("params");
            assertTrue(((List<String>) params.get(0)).get(0).startsWith("user"));
            assertTrue(((Map<String, Object>) params.get(1)).isEmpty());
        });
        verify(interruptChecker, times(2)).throwTimeoutExIfInterrupted();
    }

    @Test
    public void testRemoveUsersBatch() throws FreeIpaClientException, TimeoutException {
        Multimap<String, String> warnings = ArrayListMultimap.create();
        Set<String> users = Set.of("user1", "user2");
        when(batchPartitionSizeProperties.getByOperation("user_del")).thenReturn(3);

        underTest.removeUsers(true, freeIpaClient, users, warnings::put);

        assertTrue(warnings.isEmpty());
        ArgumentCaptor<List<Object>> operationsCaptor = ArgumentCaptor.forClass(List.class);
        verify(freeIpaClient).callBatch(any(), operationsCaptor.capture(), eq(3), eq(Set.of(FreeIpaErrorCodes.NOT_FOUND)), any());
        List<Object> operations = operationsCaptor.getValue();
        assertEquals(2, operations.size());
        operations.forEach(op -> {
            Map<String, Object> operation = (Map<String, Object>) op;
            assertEquals("user_del", operation.get("method"));
            List<Object> params = (List<Object>) operation.get("params");
            assertTrue(((List<String>) params.get(0)).get(0).startsWith("user"));
            assertTrue(((Map<String, Object>) params.get(1)).isEmpty());
        });
        verify(interruptChecker, times(2)).throwTimeoutExIfInterrupted();
    }

    @Test
    public void testRemoveGroupsBatch() throws FreeIpaClientException, TimeoutException {
        Multimap<String, String> warnings = ArrayListMultimap.create();
        Set<FmsGroup> groups = Set.of(new FmsGroup().withName("group1"), new FmsGroup().withName("group2"));
        when(batchPartitionSizeProperties.getByOperation("group_del")).thenReturn(3);

        underTest.removeGroups(true, freeIpaClient, groups, warnings::put);

        assertTrue(warnings.isEmpty());
        ArgumentCaptor<List<Object>> operationsCaptor = ArgumentCaptor.forClass(List.class);
        verify(freeIpaClient).callBatch(any(), operationsCaptor.capture(), eq(3), eq(Set.of(FreeIpaErrorCodes.NOT_FOUND)), any());
        List<Object> operations = operationsCaptor.getValue();
        assertEquals(2, operations.size());
        operations.forEach(op -> {
            Map<String, Object> operation = (Map<String, Object>) op;
            assertEquals("group_del", operation.get("method"));
            List<Object> params = (List<Object>) operation.get("params");
            assertTrue(((List<String>) params.get(0)).get(0).startsWith("group"));
            assertTrue(((Map<String, Object>) params.get(1)).isEmpty());
        });
        verify(interruptChecker, times(2)).throwTimeoutExIfInterrupted();
    }

    @Test
    public void testRemoveGroupsSingle() throws FreeIpaClientException, TimeoutException {
        Multimap<String, String> warnings = ArrayListMultimap.create();
        Set<FmsGroup> groups = Set.of(new FmsGroup().withName("group1"), new FmsGroup().withName("group2"));
        ArgumentCaptor<List<Object>> flagsCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
        RPCResponse<Object> response1 = new RPCResponse<>();
        response1.setResult(new Group());
        RPCResponse<Object> response2 = new RPCResponse<>();
        response2.setResult(new Group());
        when(freeIpaClient.invoke(eq("group_del"), flagsCaptor.capture(), paramsCaptor.capture(), eq(Group.class)))
                .thenReturn(response1, response2);

        underTest.removeGroups(false, freeIpaClient, groups, warnings::put);

        assertTrue(warnings.isEmpty());
        verifyNoInteractions(batchPartitionSizeProperties);
        verify(freeIpaClient, never()).callBatch(any(), any(), any(), any(), any());
        List<List<Object>> flagsList = flagsCaptor.getAllValues();
        assertThat(flagsList, allOf(
                hasItem(hasItem("group1")),
                hasItem(hasItem("group2"))
        ));
        List<Map<String, Object>> paramsList = paramsCaptor.getAllValues();
        assertThat(paramsList, everyItem(aMapWithSize(0)));
        verify(interruptChecker, times(4)).throwTimeoutExIfInterrupted();
    }

    @Test
    public void testRemoveUsersSingle() throws FreeIpaClientException, TimeoutException {
        Multimap<String, String> warnings = ArrayListMultimap.create();
        Set<String> users = Set.of("user1", "user2");
        ArgumentCaptor<List<Object>> flagsCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
        RPCResponse<Object> response1 = new RPCResponse<>();
        response1.setResult(new User());
        RPCResponse<Object> response2 = new RPCResponse<>();
        response2.setResult(new User());
        when(freeIpaClient.invoke(eq("user_del"), flagsCaptor.capture(), paramsCaptor.capture(), eq(User.class)))
                .thenReturn(response1, response2);

        underTest.removeUsers(false, freeIpaClient, users, warnings::put);

        assertTrue(warnings.isEmpty());
        verifyNoInteractions(batchPartitionSizeProperties);
        verify(freeIpaClient, never()).callBatch(any(), any(), any(), any(), any());
        List<List<Object>> flagsList = flagsCaptor.getAllValues();
        assertThat(flagsList, allOf(
                hasItem(hasItem("user1")),
                hasItem(hasItem("user2"))
        ));
        List<Map<String, Object>> paramsList = paramsCaptor.getAllValues();
        assertThat(paramsList, everyItem(aMapWithSize(0)));
        verify(interruptChecker, times(4)).throwTimeoutExIfInterrupted();
    }

    @Test
    public void testEnableUsersSingle() throws FreeIpaClientException, TimeoutException {
        Multimap<String, String> warnings = ArrayListMultimap.create();
        Set<String> users = Set.of("user1", "user2");
        ArgumentCaptor<List<Object>> flagsCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
        RPCResponse<Object> response1 = new RPCResponse<>();
        response1.setResult(new User());
        RPCResponse<Object> response2 = new RPCResponse<>();
        response2.setResult(new User());
        when(freeIpaClient.invoke(eq("user_enable"), flagsCaptor.capture(), paramsCaptor.capture(), eq(Object.class)))
                .thenReturn(response1, response2);

        underTest.enableUsers(false, freeIpaClient, users, warnings::put);

        assertTrue(warnings.isEmpty());
        verifyNoInteractions(batchPartitionSizeProperties);
        verify(freeIpaClient, never()).callBatch(any(), any(), any(), any(), any());
        List<List<Object>> flagsList = flagsCaptor.getAllValues();
        assertThat(flagsList, allOf(
                hasItem(hasItem("user1")),
                hasItem(hasItem("user2"))
        ));
        List<Map<String, Object>> paramsList = paramsCaptor.getAllValues();
        assertThat(paramsList, everyItem(aMapWithSize(0)));
        verify(interruptChecker, times(4)).throwTimeoutExIfInterrupted();
    }

    @Test
    public void testDisableUsersSingle() throws FreeIpaClientException, TimeoutException {
        Multimap<String, String> warnings = ArrayListMultimap.create();
        Set<String> users = Set.of("user1", "user2");
        ArgumentCaptor<List<Object>> flagsCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
        RPCResponse<Object> response1 = new RPCResponse<>();
        response1.setResult(new User());
        RPCResponse<Object> response2 = new RPCResponse<>();
        response2.setResult(new User());
        when(freeIpaClient.invoke(eq("user_disable"), flagsCaptor.capture(), paramsCaptor.capture(), eq(Object.class)))
                .thenReturn(response1, response2);

        underTest.disableUsers(false, freeIpaClient, users, warnings::put);

        assertTrue(warnings.isEmpty());
        verifyNoInteractions(batchPartitionSizeProperties);
        verify(freeIpaClient, never()).callBatch(any(), any(), any(), any(), any());
        List<List<Object>> flagsList = flagsCaptor.getAllValues();
        assertThat(flagsList, allOf(
                hasItem(hasItem("user1")),
                hasItem(hasItem("user2"))
        ));
        List<Map<String, Object>> paramsList = paramsCaptor.getAllValues();
        assertThat(paramsList, everyItem(aMapWithSize(0)));
        verify(interruptChecker, times(4)).throwTimeoutExIfInterrupted();
    }

    @Test
    public void testAddUsersSingle() throws FreeIpaClientException, TimeoutException {
        Multimap<String, String> warnings = ArrayListMultimap.create();
        when(userMetadataConverter.toUserMetadataJson(anyString(), anyLong())).thenCallRealMethod();
        FmsUser user1 = new FmsUser().withName("user1").withFirstName("User_1").withLastName("Test1")
                .withState(FmsUser.State.ENABLED).withCrn(createUserCrn());
        FmsUser user2 = new FmsUser().withName("user2").withFirstName("User_2").withLastName("Test2")
                .withState(FmsUser.State.DISABLED).withCrn(createUserCrn());
        Set<FmsUser> users = Set.of(user1, user2);
        ArgumentCaptor<List<Object>> flagsCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
        RPCResponse<Object> response1 = new RPCResponse<>();
        response1.setResult(new User());
        RPCResponse<Object> response2 = new RPCResponse<>();
        response2.setResult(new User());
        when(freeIpaClient.invoke(eq("user_add"), flagsCaptor.capture(), paramsCaptor.capture(), eq(User.class)))
                .thenReturn(response1, response2);

        underTest.addUsers(false, freeIpaClient, users, warnings::put);

        assertTrue(warnings.isEmpty());
        verifyNoInteractions(batchPartitionSizeProperties);
        verify(freeIpaClient, never()).callBatch(any(), any(), any(), any(), any());
        List<List<Object>> flagsList = flagsCaptor.getAllValues();
        assertThat(flagsList, allOf(
                hasItem(hasItem("user1")),
                hasItem(hasItem("user2"))
        ));
        List<Map<String, Object>> paramsList = paramsCaptor.getAllValues();
        assertThat(paramsList, everyItem(aMapWithSize(6)));
        assertThat(paramsList, allOf(
                Matchers.<Map<String, Object>>hasItem(allOf(
                        hasEntry("givenname", "User_1"),
                        hasEntry("sn", "Test1"),
                        hasEntry(Matchers.equalTo("title"), new ObjectSubstringMatcher(user1.getCrn()))
                )),
                Matchers.<Map<String, Object>>hasItem(allOf(
                        hasEntry("givenname", "User_2"),
                        hasEntry("sn", "Test2"),
                        hasEntry(Matchers.equalTo("title"), new ObjectSubstringMatcher(user2.getCrn()))
                ))
        ));
        verify(interruptChecker, times(4)).throwTimeoutExIfInterrupted();
    }

    @Test
    public void testAddGroupsSingle() throws FreeIpaClientException, TimeoutException {
        Multimap<String, String> warnings = ArrayListMultimap.create();
        FmsGroup group1 = new FmsGroup().withName(UserSyncConstants.CDP_USERSYNC_INTERNAL_GROUP);
        FmsGroup group2 = new FmsGroup().withName("group2");
        Set<FmsGroup> groups = Set.of(group1, group2);
        ArgumentCaptor<List<Object>> flagsCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
        RPCResponse<Object> response1 = new RPCResponse<>();
        response1.setResult(new Group());
        RPCResponse<Object> response2 = new RPCResponse<>();
        response2.setResult(new Group());
        when(freeIpaClient.invoke(eq("group_add"), flagsCaptor.capture(), paramsCaptor.capture(), eq(Group.class)))
                .thenReturn(response1, response2);

        underTest.addGroups(false, freeIpaClient, groups, warnings::put);

        assertTrue(warnings.isEmpty());
        verifyNoInteractions(batchPartitionSizeProperties);
        verify(freeIpaClient, never()).callBatch(any(), any(), any(), any(), any());
        List<List<Object>> flagsList = flagsCaptor.getAllValues();
        assertThat(flagsList, allOf(
                hasItem(hasItem(UserSyncConstants.CDP_USERSYNC_INTERNAL_GROUP)),
                hasItem(hasItem("group2"))
        ));
        List<Map<String, Object>> paramsList = paramsCaptor.getAllValues();
        assertTrue(paramsList.get(0).isEmpty());
        assertEquals(true, paramsList.get(1).get("nonposix"));
        verify(interruptChecker, times(4)).throwTimeoutExIfInterrupted();
    }

    @Test
    public void testRemoveUsersFromGroupsSingle() throws Exception {
        Multimap<String, String> groupMapping = setupGroupMapping(5, MAX_SUBJECTS_PER_REQUEST * 2);
        Multimap<String, String> warnings = ArrayListMultimap.create();
        ArgumentCaptor<List<Object>> flagsCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
        RPCResponse<Object> response1 = new RPCResponse<>();
        response1.setResult(new Group());
        RPCResponse<Object> response2 = new RPCResponse<>();
        response2.setResult(new Group());
        when(freeIpaClient.invoke(eq("group_remove_member"), flagsCaptor.capture(), paramsCaptor.capture(), eq(Group.class)))
                .thenReturn(response1, response2);

        underTest.removeUsersFromGroups(false, freeIpaClient, groupMapping, warnings::put);

        assertTrue(warnings.isEmpty());
        verifyNoInteractions(batchPartitionSizeProperties);
        verify(freeIpaClient, never()).callBatch(any(), any(), any(), any(), any());
        List<List<Object>> flagsList = flagsCaptor.getAllValues();
        Map<String, Long> flagCount = flagsList.stream()
                .flatMap(Collection::stream)
                .map(o -> (String) o)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        assertEquals(5, flagCount.size());
        assertTrue(flagCount.keySet().containsAll(Set.of("group1", "group2", "group3", "group4", "group0")));
        flagCount.forEach((flag, count) -> assertEquals(2, count, flag + " size should be 2"));
        List<Map<String, Object>> paramsList = paramsCaptor.getAllValues();
        assertEquals(10, paramsList.size());
        paramsList.forEach(map -> {
            assertEquals(10, ((List<String>) map.get("user")).size());
            assertTrue(((List<String>) map.get("user")).stream().allMatch(user -> user.startsWith("user")));
        });
        verify(interruptChecker, times(20)).throwTimeoutExIfInterrupted();
    }

    @Test
    public void testAddUsersToGroupsSingle() throws Exception {
        Multimap<String, String> groupMapping = setupGroupMapping(5, MAX_SUBJECTS_PER_REQUEST * 2);
        Multimap<String, String> warnings = ArrayListMultimap.create();
        ArgumentCaptor<List<Object>> flagsCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
        RPCResponse<Object> response = new RPCResponse<>();
        Group result = new Group();
        result.setMemberUser(List.copyOf(groupMapping.values()));
        response.setResult(result);
        when(freeIpaClient.invoke(eq("group_add_member"), flagsCaptor.capture(), paramsCaptor.capture(), eq(Group.class)))
                .thenReturn(response);

        underTest.addUsersToGroups(false, freeIpaClient, groupMapping, warnings::put);

        assertTrue(warnings.isEmpty());
        verifyNoInteractions(batchPartitionSizeProperties);
        verify(freeIpaClient, never()).callBatch(any(), any(), any(), any(), any());
        List<List<Object>> flagsList = flagsCaptor.getAllValues();
        Map<String, Long> flagCount = flagsList.stream()
                .flatMap(Collection::stream)
                .map(o -> (String) o)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        assertEquals(5, flagCount.size());
        assertTrue(flagCount.keySet().containsAll(Set.of("group1", "group2", "group3", "group4", "group0")));
        flagCount.forEach((flag, count) -> assertEquals(2, count, flag + " size should be 2"));
        List<Map<String, Object>> paramsList = paramsCaptor.getAllValues();
        assertEquals(10, paramsList.size());
        paramsList.forEach(map -> {
            assertEquals(10, ((List<String>) map.get("user")).size());
            assertTrue(((List<String>) map.get("user")).stream().allMatch(user -> user.startsWith("user")));
        });
        verify(interruptChecker, times(20)).throwTimeoutExIfInterrupted();
    }

    @Test
    public void testAddUsersToGroupsSingleThrowsException() throws Exception {
        Multimap<String, String> groupMapping = setupGroupMapping(5, MAX_SUBJECTS_PER_REQUEST * 2);
        Multimap<String, String> warnings = ArrayListMultimap.create();
        when(freeIpaClient.invoke(eq("group_add_member"), any(), any(), eq(Group.class)))
                .thenThrow(new FreeIpaClientException("asdf"));
        doThrow(new FreeIpaClientException("asdf")).when(freeIpaClient).checkIfClientStillUsable(any());

        assertThrows(FreeIpaClientException.class, () -> underTest.addUsersToGroups(false, freeIpaClient, groupMapping, warnings::put));

        assertFalse(warnings.isEmpty());
        verifyNoInteractions(batchPartitionSizeProperties);
        verify(freeIpaClient, never()).callBatch(any(), any(), any(), any(), any());
        verify(interruptChecker, times(11)).throwTimeoutExIfInterrupted();
    }

    @Test
    public void testSingleErrorHandlingAcceptableErrorCode() throws FreeIpaClientException, TimeoutException {
        Multimap<String, String> warnings = ArrayListMultimap.create();
        Set<String> users = Set.of("user1", "user2");
        ArgumentCaptor<List<Object>> flagsCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
        when(freeIpaClient.invoke(eq("user_enable"), flagsCaptor.capture(), paramsCaptor.capture(), eq(Object.class)))
                .thenThrow(new FreeIpaClientException("Asdf", new JsonRpcClientException(FreeIpaErrorCodes.ALREADY_ACTIVE.getValue(), "fdsa", null)));

        underTest.enableUsers(false, freeIpaClient, users, warnings::put);

        assertTrue(warnings.isEmpty());
        verifyNoInteractions(batchPartitionSizeProperties);
        verify(freeIpaClient, never()).callBatch(any(), any(), any(), any(), any());
        List<List<Object>> flagsList = flagsCaptor.getAllValues();
        assertThat(flagsList, allOf(
                hasItem(hasItem("user1")),
                hasItem(hasItem("user2"))
        ));
        List<Map<String, Object>> paramsList = paramsCaptor.getAllValues();
        assertThat(paramsList, everyItem(aMapWithSize(0)));
        verify(interruptChecker, times(4)).throwTimeoutExIfInterrupted();
    }

    @Test
    public void testSingleErrorHandlingNotAcceptableErrorCode() throws FreeIpaClientException, TimeoutException {
        Multimap<String, String> warnings = ArrayListMultimap.create();
        Set<String> users = Set.of("user1", "user2");
        ArgumentCaptor<List<Object>> flagsCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
        when(freeIpaClient.invoke(eq("user_enable"), flagsCaptor.capture(), paramsCaptor.capture(), eq(Object.class)))
                .thenThrow(new FreeIpaClientException("Asdf", new JsonRpcClientException(FreeIpaErrorCodes.SESSION_ERROR.getValue(), "fdsa", null)));

        underTest.enableUsers(false, freeIpaClient, users, warnings::put);

        assertFalse(warnings.isEmpty());
        verifyNoInteractions(batchPartitionSizeProperties);
        verify(freeIpaClient, never()).callBatch(any(), any(), any(), any(), any());
        verify(freeIpaClient, times(2)).checkIfClientStillUsable(any(FreeIpaClientException.class));
        List<List<Object>> flagsList = flagsCaptor.getAllValues();
        assertThat(flagsList, allOf(
                hasItem(hasItem("user1")),
                hasItem(hasItem("user2"))
        ));
        List<Map<String, Object>> paramsList = paramsCaptor.getAllValues();
        assertThat(paramsList, everyItem(aMapWithSize(0)));
        verify(interruptChecker, times(4)).throwTimeoutExIfInterrupted();
    }

    private String createUserCrn() {
        return CrnTestUtil.getUserCrnBuilder().setAccountId(ACCOUNT_ID).setResource(UUID.randomUUID().toString())
                .build().toString();
    }

    private Multimap<String, String> setupGroupMapping(int numGroups, int numPerGroup) {
        Multimap<String, String> groupMapping = HashMultimap.create();
        for (int i = 0; i < numGroups; ++i) {
            String group = "group" + i;
            for (int j = 0; j < numPerGroup; ++j) {
                groupMapping.put(group, "user" + j);
            }
        }
        return groupMapping;
    }

    private static class ObjectSubstringMatcher extends BaseMatcher<Object> {
        private final String stringToMatch;

        ObjectSubstringMatcher(String stringToMatch) {
            this.stringToMatch = stringToMatch;
        }

        @Override
        public boolean matches(Object actual) {
            assertTrue(actual instanceof String);
            return ((String) actual).contains(stringToMatch);
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("contains '" + stringToMatch + "'");
        }
    }

}