package com.sequenceiq.cloudbreak.service.blueprint;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.times;
import static org.mockito.BDDMockito.verify;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;

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
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.Company;
import com.sequenceiq.cloudbreak.domain.User;
import com.sequenceiq.cloudbreak.domain.UserRole;
import com.sequenceiq.cloudbreak.domain.WebsocketEndPoint;
import com.sequenceiq.cloudbreak.repository.BlueprintRepository;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.service.company.CompanyService;
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
    private CompanyService companyService;


    private User user;

    private Company company;

    private Blueprint blueprint;

    @Before
    public void setUp() {
        underTest = new DefaultBlueprintService();
        company = new Company();
        company.setId(1L);

        MockitoAnnotations.initMocks(this);
        user = new User();
        user.setEmail("dummy@mymail.com");
        user.setCompany(company);
        blueprint = createBlueprint(user);
    }

    @Test
    public void testAddBlueprint() {
        //GIVEN
        given(blueprintConverter.convert(blueprintJson)).willReturn(blueprint);
        given(blueprintRepository.save(blueprint)).willReturn(blueprint);
        doNothing().when(websocketService).sendToTopicUser(anyString(), any(WebsocketEndPoint.class), any(StatusMessage.class));
        //WHEN
        Blueprint result = underTest.addBlueprint(user, blueprintJson);
        //THEN
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

    @Test
    public void testGetBlueprint() {
        // GIVEN
        given(blueprintRepository.findOne(anyLong())).willReturn(blueprint);
        given(blueprintConverter.convert(blueprint)).willReturn(blueprintJson);
        // WHEN
        underTest.get(1L);
        // THEN
        verify(blueprintConverter, times(1)).convert(blueprint);
    }

    @Test(expected = NotFoundException.class)
    public void testGetBlueprintWhenBlueprintNotFound() {
        // GIVEN
        given(blueprintRepository.findOne(anyLong())).willReturn(null);
        // WHEN
        underTest.get(1L);
    }

    @Test
    public void testGetAllForCompanyAdminWithoutCompanyUser() {
        // GIVEN
        Company company = createCompany("Blueprint Ltd.", 1L);
        User admin = createUser(UserRole.COMPANY_ADMIN, company, 1L);
        // admin has a blueprint
        admin.getBlueprints().add(createBlueprint(admin));

        // WHEN
        Set<Blueprint> blueprints = underTest.getAll(admin);

        // THEN
        Assert.assertNotNull(blueprints);
        Assert.assertTrue(blueprints.size() == 1);
    }

    @Test
    public void testGetAllForCompanyAdminWithCompanyUserWithBlueprint() {
        // GIVEN
        Company company = createCompany("Blueprint Ltd.", 1L);
        User admin = createUser(UserRole.COMPANY_ADMIN, company, 1L);
        User cUser = createUser(UserRole.COMPANY_USER, company, 3L);
        // admin has a blueprint
        admin.getBlueprints().add(createBlueprint(admin));
        // cUser has also one blueprint
        cUser.getBlueprints().add(createBlueprint(cUser));
        given(companyService.companyUsers(company.getId())).willReturn(new HashSet<User>(Arrays.asList(cUser)));

        // WHEN
        Set<Blueprint> blueprints = underTest.getAll(admin);

        // THEN
        Assert.assertNotNull(blueprints);
        Assert.assertTrue("The number of the returned blueprints is right", blueprints.size() == 2);
    }


    @Test
    public void testGetAllForCompanyUserWithVisibleCompanyBlueprints() {
        // GIVEN
        Company company = createCompany("Blueprint Ltd.", 1L);
        User admin = createUser(UserRole.COMPANY_ADMIN, company, 1L);
        User cUser = createUser(UserRole.COMPANY_USER, company, 3L);
        // admin has a blueprint, with COMPANY_ADMIN role! (not visible for company users
        admin.getBlueprints().add(createBlueprint(admin));
        // cUser has also one blueprint
        cUser.getBlueprints().add(createBlueprint(cUser));
        given(companyService.companyUserData(company.getId(), UserRole.COMPANY_USER)).willReturn(admin);

        // WHEN
        Set<Blueprint> blueprints = underTest.getAll(cUser);

        //THEN
        Assert.assertNotNull(blueprints);
        Assert.assertTrue("The number of the returned blueprints is right", blueprints.size() == 2);
    }


    private Blueprint createBlueprint(User bpUser) {
        Blueprint blueprint = new Blueprint();
        blueprint.setId(1L);
        blueprint.setUser(bpUser);
        blueprint.setBlueprintName("dummyName");
        blueprint.setBlueprintText("dummyText");
        blueprint.getUserRoles().addAll(bpUser.getUserRoles());
        return blueprint;
    }

    private User createUser(UserRole role, Company company, Long userId) {
        User usr = new User();
        usr.setId(userId);
        usr.setCompany(company);
        usr.getUserRoles().add(role);
        return usr;
    }

    private Company createCompany(String name, Long companyId) {
        Company company = new Company();
        company.setName(name);
        company.setId(companyId);
        return company;
    }

}
