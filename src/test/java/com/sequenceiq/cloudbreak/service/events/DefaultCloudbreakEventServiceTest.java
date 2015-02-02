package com.sequenceiq.cloudbreak.service.events;

import static org.mockito.BDDMockito.given;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.BDDMockito;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.CloudbreakEvent;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.repository.CloudbreakEventRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.ServiceTestUtils;
import com.sequenceiq.cloudbreak.service.notification.NotificationSender;

public class DefaultCloudbreakEventServiceTest {

    @InjectMocks
    private DefaultCloudbreakEventService eventService;

    @Mock
    private CloudbreakEventRepository eventRepository;

    @Mock
    private StackRepository stackRepository;

    @Mock
    private NotificationSender notificationSender;

    @Captor
    private ArgumentCaptor<CloudbreakEvent> captor;

    @Before
    public void setUp() throws Exception {
        eventService = new DefaultCloudbreakEventService();
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testGenerateAwsStackEvent() throws Exception {
        //GIVEN
        Template template = ServiceTestUtils.createTemplate(ServiceTestUtils.DUMMY_OWNER, ServiceTestUtils.DUMMY_ACCOUNT, CloudPlatform.AWS);
        Stack stack = ServiceTestUtils.createStack("John", "Acme", template, null);

        given(stackRepository.findById(1L)).willReturn(stack);
        CloudbreakEvent stackEvent = new CloudbreakEvent();
        given(eventRepository.save(Mockito.any(CloudbreakEvent.class))).willReturn(stackEvent);
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setGroupName("master");
        instanceGroup.setTemplate(template);
        CloudbreakEventData eventData = new CloudbreakEventData(1L, "STACK_CREATED", "Stack created");

        //WHEN
        eventService.createStackEvent(eventData);

        //THEN
        BDDMockito.verify(eventRepository).save(captor.capture());
        CloudbreakEvent event = captor.getValue();

        Assert.assertNotNull(event);
        Assert.assertEquals("The user name is not the expected", "John", event.getOwner());
        Assert.assertEquals("The cloudprovider is not the expected", CloudPlatform.AWS.name(), event.getCloud());
    }

    @Test
    public void testGenerateAzureStackEvent() throws Exception {
        //GIVEN
        Template template = ServiceTestUtils.createTemplate(ServiceTestUtils.DUMMY_OWNER, ServiceTestUtils.DUMMY_ACCOUNT, CloudPlatform.AZURE);
        Stack stack = ServiceTestUtils.createStack("John", "Acme", template, null);

        given(stackRepository.findById(1L)).willReturn(stack);
        CloudbreakEvent stackEvent = new CloudbreakEvent();
        given(eventRepository.save(Mockito.any(CloudbreakEvent.class))).willReturn(stackEvent);
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setGroupName("master");
        instanceGroup.setTemplate(template);
        CloudbreakEventData eventData = new CloudbreakEventData(1L, "STACK_CREATED", "Stack created");

        //WHEN
        eventService.createStackEvent(eventData);

        //THEN
        BDDMockito.verify(eventRepository).save(captor.capture());
        CloudbreakEvent event = captor.getValue();

        Assert.assertNotNull(event);
        Assert.assertEquals("The user name is not the expected", "John", event.getOwner());
        Assert.assertEquals("The cloudprovider is not the expected", CloudPlatform.AZURE.name(), event.getCloud());
    }

    @Test
    public void testShouldClusterDataBePopulated() {
        //GIVEN
        Template template = ServiceTestUtils.createTemplate(ServiceTestUtils.DUMMY_OWNER, ServiceTestUtils.DUMMY_ACCOUNT, CloudPlatform.AZURE);
        Blueprint blueprint = ServiceTestUtils.createBlueprint(ServiceTestUtils.DUMMY_OWNER, ServiceTestUtils.DUMMY_ACCOUNT);
        Cluster cluster = ServiceTestUtils.createCluster("John", "Acme", blueprint);
        Stack stack = ServiceTestUtils.createStack("John", "Acme", template, cluster);

        given(stackRepository.findById(1L)).willReturn(stack);
        CloudbreakEvent stackEvent = new CloudbreakEvent();
        given(eventRepository.save(Mockito.any(CloudbreakEvent.class))).willReturn(stackEvent);
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setGroupName("master");
        instanceGroup.setTemplate(template);
        CloudbreakEventData eventData = new CloudbreakEventData(1L, "STACK_CREATED", "Stack created");

        //WHEN
        eventService.createStackEvent(eventData);

        //THEN
        BDDMockito.verify(eventRepository).save(captor.capture());
        com.sequenceiq.cloudbreak.domain.CloudbreakEvent event = captor.getValue();

        Assert.assertNotNull(event);
        Assert.assertEquals("The user name is not the expected", "John", event.getOwner());
        Assert.assertEquals("The blueprint name is not the expected", "test-blueprint", event.getBlueprintName());
        Assert.assertEquals("The blueprint id is not the expected", 1L, event.getBlueprintId());

    }

}