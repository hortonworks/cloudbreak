package com.sequenceiq.cloudbreak.service.blueprint;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.DataIntegrityViolationException;

import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.controller.NotFoundException;
import com.sequenceiq.cloudbreak.controller.json.BlueprintJson;
import com.sequenceiq.cloudbreak.converter.BlueprintConverter;
import com.sequenceiq.cloudbreak.domain.Account;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.User;
import com.sequenceiq.cloudbreak.domain.UserRole;
import com.sequenceiq.cloudbreak.domain.WebsocketEndPoint;
import com.sequenceiq.cloudbreak.repository.BlueprintRepository;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.service.ServiceTestUtils;
import com.sequenceiq.cloudbreak.service.account.AccountService;
import com.sequenceiq.cloudbreak.websocket.WebsocketService;
import com.sequenceiq.cloudbreak.websocket.message.StatusMessage;

public class BlueprintServiceTest {
    @InjectMocks
    private DefaultBlueprintService underTest;

    @Mock
    private BlueprintRepository blueprintRepository;

    @Mock
    private ClusterRepository clusterRepository;

    @Mock
    private BlueprintConverter blueprintConverter;

    @Mock
    private WebsocketService websocketService;

    @Mock
    private BlueprintJson blueprintJson;

    @Mock
    private AccountService accountService;

    private User user;

    private Account account;

    private Blueprint blueprint;

    @Before
    public void setUp() {
        underTest = new DefaultBlueprintService();
        account = new Account();
        account.setId(1L);

        MockitoAnnotations.initMocks(this);
        user = new User();
        user.setEmail("dummy@mymail.com");
        user.setAccount(account);
        blueprint = ServiceTestUtils.createBlueprint(user);
    }

    @Test
    public void testAddBlueprint() {
        // GIVEN
        given(blueprintConverter.convert(blueprintJson)).willReturn(blueprint);
        given(blueprintRepository.save(blueprint)).willReturn(blueprint);
        doNothing().when(websocketService).sendToTopicUser(anyString(), any(WebsocketEndPoint.class), any(StatusMessage.class));
        // WHEN
        Blueprint result = underTest.addBlueprint(user, blueprint);
        // THEN
        verify(websocketService, times(1)).sendToTopicUser(anyString(), any(WebsocketEndPoint.class), any(StatusMessage.class));
        Assert.assertEquals(result.getId(), (Long) 1L);
    }

    @Test
    public void testDeleteBlueprint() {
        // GIVEN
        given(blueprintRepository.findOne(anyLong())).willReturn(blueprint);
        doNothing().when(blueprintRepository).delete(blueprint);
        doNothing().when(websocketService).sendToTopicUser(anyString(), any(WebsocketEndPoint.class), any(StatusMessage.class));
        // WHEN
        underTest.delete(1L);
        // THEN
        verify(websocketService, times(1)).sendToTopicUser(anyString(), any(WebsocketEndPoint.class), any(StatusMessage.class));
        verify(blueprintRepository, times(1)).delete(blueprint);
    }

    @Test(expected = NotFoundException.class)
    public void testDeleteBlueprintWhenBlueprintNotFound() {
        // GIVEN
        given(blueprintRepository.findOne(anyLong())).willReturn(null);
        // WHEN
        underTest.delete(1L);
    }

    @Test(expected = DataIntegrityViolationException.class)
    public void testDeleteBlueprintWhenBlueprintDataIntegrityExceptionAndFindAllClusterByBlueprintReturnEmptyList() {
        // GIVEN
        given(blueprintRepository.findOne(anyLong())).willReturn(blueprint);
        doThrow(new DataIntegrityViolationException("test")).when(blueprintRepository).delete(blueprint);
        Set<Cluster> clusters = new HashSet<>();
        given(clusterRepository.findAllClusterByBlueprint(anyLong())).willReturn(clusters);
        // WHEN
        underTest.delete(1L);
    }

    @Test(expected = BadRequestException.class)
    public void testDeleteBlueprintWhenBlueprintDataIntegrityExceptionAndFindAllClusterByBlueprintReturnNotEmptyList() {
        // GIVEN
        given(blueprintRepository.findOne(anyLong())).willReturn(blueprint);
        doThrow(new DataIntegrityViolationException("test")).when(blueprintRepository).delete(blueprint);
        Set<Cluster> clusters = new HashSet<>();
        clusters.add(new Cluster());
        given(clusterRepository.findAllClusterByBlueprint(anyLong())).willReturn(clusters);
        // WHEN
        underTest.delete(1L);
    }

    @Test(expected = NotFoundException.class)
    public void testGetBlueprintWhenBlueprintNotFound() {
        // GIVEN
        given(blueprintRepository.findOne(anyLong())).willReturn(null);
        // WHEN
        underTest.get(1L);
    }

    @Test
    public void testGetAllForAccountAdminWithoutAccountUser() {
        // GIVEN
        Account account = ServiceTestUtils.createAccount("Blueprint Ltd.", 1L);
        User admin = ServiceTestUtils.createUser(UserRole.ACCOUNT_ADMIN, account, 1L);
        // admin has a blueprint
        admin.getBlueprints().add(ServiceTestUtils.createBlueprint(admin));

        // WHEN
        Set<Blueprint> blueprints = underTest.getAll(admin);

        // THEN
        Assert.assertNotNull(blueprints);
        Assert.assertTrue(blueprints.size() == 1);
    }

    @Test
    public void testGetAllForAccountAdminWithAccountUserWithBlueprint() {
        // GIVEN
        Account account = ServiceTestUtils.createAccount("Blueprint Ltd.", 1L);
        User admin = ServiceTestUtils.createUser(UserRole.ACCOUNT_ADMIN, account, 1L);
        User cUser = ServiceTestUtils.createUser(UserRole.ACCOUNT_USER, account, 3L);
        // admin has a blueprint
        admin.getBlueprints().add(ServiceTestUtils.createBlueprint(admin));
        // cUser has also one blueprint
        cUser.getBlueprints().add(ServiceTestUtils.createBlueprint(cUser));
        given(accountService.accountUsers(account.getId())).willReturn(new HashSet<User>(Arrays.asList(cUser)));

        // WHEN
        Set<Blueprint> blueprints = underTest.getAll(admin);

        // THEN
        Assert.assertNotNull(blueprints);
        Assert.assertTrue("The number of the returned blueprints is right", blueprints.size() == 2);
    }

    @Test
    public void testGetAllForAccountUserWithVisibleAccountBlueprints() {
        // GIVEN
        Account account = ServiceTestUtils.createAccount("Blueprint Ltd.", 1L);
        User admin = ServiceTestUtils.createUser(UserRole.ACCOUNT_ADMIN, account, 1L);
        User cUser = ServiceTestUtils.createUser(UserRole.ACCOUNT_USER, account, 3L);
        // admin has a blueprint, with Account_ADMIN role! (not visible for
        // Account users
        admin.getBlueprints().add(ServiceTestUtils.createBlueprint(admin));
        // cUser has also one blueprint
        cUser.getBlueprints().add(ServiceTestUtils.createBlueprint(cUser));
        given(accountService.accountUserData(account.getId(), UserRole.ACCOUNT_USER)).willReturn(admin);

        // WHEN
        Set<Blueprint> blueprints = underTest.getAll(cUser);

        // THEN
        Assert.assertNotNull(blueprints);
        Assert.assertTrue("The number of the returned blueprints is right", blueprints.size() == 2);
    }

}
