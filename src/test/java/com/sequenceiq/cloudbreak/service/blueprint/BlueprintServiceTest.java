package com.sequenceiq.cloudbreak.service.blueprint;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.times;
import static org.mockito.BDDMockito.verify;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;

import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.DataIntegrityViolationException;

import com.sequenceiq.cloudbreak.controller.NotFoundException;
import com.sequenceiq.cloudbreak.controller.json.BlueprintJson;
import com.sequenceiq.cloudbreak.controller.json.IdJson;
import com.sequenceiq.cloudbreak.converter.BlueprintConverter;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.User;
import com.sequenceiq.cloudbreak.domain.WebsocketEndPoint;
import com.sequenceiq.cloudbreak.repository.BlueprintRepository;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.websocket.WebsocketService;
import com.sequenceiq.cloudbreak.websocket.message.StatusMessage;

public class BlueprintServiceTest {
    @InjectMocks
    private BlueprintService underTest;

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

    private User user;

    private Blueprint blueprint;

    @Before
    public void setUp() {
        underTest = new BlueprintService();
        MockitoAnnotations.initMocks(this);
        user = new User();
        user.setEmail("dummy@mymail.com");
        blueprint = createBlueprint();
    }

    @Test
    public void testAddBlueprint() {
        //GIVEN
        given(blueprintConverter.convert(blueprintJson)).willReturn(blueprint);
        given(blueprintRepository.save(blueprint)).willReturn(blueprint);
        doNothing().when(websocketService).sendToTopicUser(anyString(), any(WebsocketEndPoint.class), any(StatusMessage.class));
        //WHEN
        IdJson result = underTest.addBlueprint(user, blueprintJson);
        //THEN
        verify(websocketService, times(1)).sendToTopicUser(anyString(), any(WebsocketEndPoint.class), any(StatusMessage.class));
        Assert.assertEquals(result.getId(), (Long) 1L);
    }

    @Test
    public void testDeleteBlueprint() {
        //GIVEN
        given(blueprintRepository.findOne(anyLong())).willReturn(blueprint);
        doNothing().when(blueprintRepository).delete(blueprint);
        doNothing().when(websocketService).sendToTopicUser(anyString(), any(WebsocketEndPoint.class), any(StatusMessage.class));
        //WHEN
        underTest.delete(1L);
        //THEN
        verify(websocketService, times(1)).sendToTopicUser(anyString(), any(WebsocketEndPoint.class), any(StatusMessage.class));
        verify(blueprintRepository, times(1)).delete(blueprint);
    }

    @Test(expected = NotFoundException.class)
    public void testDeleteBlueprintWhenBlueprintNotFound() {
        //GIVEN
        given(blueprintRepository.findOne(anyLong())).willReturn(null);
        //WHEN
        underTest.delete(1L);
    }

    @Test(expected = DataIntegrityViolationException.class)
    public void testDeleteBlueprintWhenBlueprintDataIntegrityExceptionAndFindAllClusterByBlueprintReturnEmptyList() {
        //GIVEN
        given(blueprintRepository.findOne(anyLong())).willReturn(blueprint);
        doThrow(new DataIntegrityViolationException("test")).when(blueprintRepository).delete(blueprint);
        Set<Cluster> clusters = new HashSet<>();
        given(clusterRepository.findAllClusterByBlueprint(anyLong())).willReturn(clusters);
        //WHEN
        underTest.delete(1L);
    }

    @Test
    public void testDeleteBlueprintWhenBlueprintDataIntegrityExceptionAndFindAllClusterByBlueprintReturnNotEmptyList() {
        //GIVEN
        given(blueprintRepository.findOne(anyLong())).willReturn(blueprint);
        doThrow(new DataIntegrityViolationException("test")).when(blueprintRepository).delete(blueprint);
        Set<Cluster> clusters = new HashSet<>();
        clusters.add(new Cluster());
        given(clusterRepository.findAllClusterByBlueprint(anyLong())).willReturn(clusters);
        //WHEN
        underTest.delete(1L);
    }

    private Blueprint createBlueprint() {
        Blueprint blueprint = new Blueprint();
        blueprint.setId(1L);
        blueprint.setUser(user);
        blueprint.setBlueprintName("dummyName");
        blueprint.setBlueprintText("dummyText");
        return blueprint;
    }
}
