package com.sequenceiq.cloudbreak.cloud.openstack.common;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.util.HashMap;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.openstack4j.api.OSClient;
import org.openstack4j.api.image.v2.ImageService;
import org.openstack4j.api.image.v2.TaskService;
import org.openstack4j.model.image.v2.Task.TaskStatus;
import org.openstack4j.openstack.image.v2.domain.GlanceTask;

import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.common.service.url.UrlAccessValidationService;

@RunWith(MockitoJUnitRunner.class)
public class OpenStackImageImageImporterTest {

    @InjectMocks
    private final OpenStackImageImporter underTest = new OpenStackImageImporter();

    @Mock
    private OSClient<?> osClient;

    @Mock
    private ImageService imageService;

    @Mock
    private TaskService taskService;

    @Mock
    private OpenStackImageImportTaskParameters openStackImageImportTaskParameters;

    @Mock
    private UrlAccessValidationService urlAccessValidationService;

    @Before
    public void setUp() {
        when(osClient.imagesV2()).thenReturn(imageService);
        when(imageService.tasks()).thenReturn(taskService);

        when(urlAccessValidationService.isAccessible("https://myimage.img", null)).thenReturn(true);
        when(openStackImageImportTaskParameters.getImportLocation("myimage")).thenReturn("https://myimage.img");
        when(openStackImageImportTaskParameters.buildInput(any())).thenReturn(new HashMap<>());
    }

    @Test
    public void testImportSuccess() {
        GlanceTask task = Mockito.mock(GlanceTask.class);
        when(task.getStatus()).thenReturn(TaskStatus.SUCCESS);
        when(taskService.create(any())).thenReturn(task);
        underTest.importImage(osClient, "myimage");
    }

    @Test(expected = CloudConnectorException.class)
    public void testImportNotExsist() {
        try {
            when(urlAccessValidationService.isAccessible("https://not-exist.img", null)).thenReturn(false);
            when(openStackImageImportTaskParameters.getImportLocation("not-exist")).thenReturn("https://not-exist.img");
            underTest.importImage(osClient, "not-exist");
        } catch (CloudConnectorException e) {
            Assert.assertEquals("OpenStack image 'https://not-exist.img' is not accessible, therefore it cannot be imported automatically", e.getMessage());
            throw e;
        }
    }

    @Test(expected = CloudConnectorException.class)
    public void testImportFailure() {
        try {
            GlanceTask task = Mockito.mock(GlanceTask.class);
            when(task.getStatus()).thenReturn(TaskStatus.FAILURE);
            when(task.getMessage()).thenReturn("USEFUL ERRROR MESSAGE");
            when(taskService.create(any())).thenReturn(task);
            underTest.importImage(osClient, "myimage");
        } catch (CloudConnectorException e) {
            Assert.assertEquals("Import of myimage failed with status: FAILURE, message: USEFUL ERRROR MESSAGE", e.getMessage());
            throw e;
        }
    }

    @Test(expected = CloudConnectorException.class)
    public void testImportNoStatus() {
        try {
            GlanceTask task = Mockito.mock(GlanceTask.class);
            when(task.getStatus()).thenReturn(null);
            when(task.getMessage()).thenReturn("USEFUL ERRROR MESSAGE");
            when(taskService.create(any())).thenReturn(task);
            underTest.importImage(osClient, "myimage");
        } catch (CloudConnectorException e) {
            Assert.assertEquals("Import of myimage did not return any status, message: USEFUL ERRROR MESSAGE", e.getMessage());
            throw e;
        }
    }

    @Test(expected = CloudConnectorException.class)
    public void nullTask() {
        try {
            when(taskService.create(any())).thenReturn(null);
            underTest.importImage(osClient, "myimage");
        } catch (CloudConnectorException e) {
            Assert.assertEquals("Import of myimage did not return any task or status object", e.getMessage());
            throw e;
        }
    }
}
