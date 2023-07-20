package com.sequenceiq.cloudbreak.core.flow2.stack.image.update;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.cloud.event.model.EventStatus;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.core.flow2.CheckResult;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.service.cluster.InstanceMetadataUpdater;
import com.sequenceiq.cloudbreak.service.cluster.Package;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;

@ExtendWith(MockitoExtension.class)
public class PackageVersionCheckerTest {

    @Mock
    private InstanceMetadataUpdater instanceMetadataUpdater;

    @Mock
    private CloudbreakMessagesService messagesService;

    @Mock
    private StatedImage statedImage;

    @Mock
    private Image image;

    @InjectMocks
    private PackageVersionChecker underTest;

    @BeforeEach
    public void setUp() {
        lenient().when(messagesService.getMessage(anyString(), anyCollection())).thenReturn("message");
    }

    @Test
    public void compareImageAndInstancesMandatoryPackageVersionBaseOk() {
        String packageName = "package";
        Map<String, String> packageVersions = Collections.singletonMap(packageName, "1");
        when(statedImage.getImage()).thenReturn(image);
        when(image.isPrewarmed()).thenReturn(false);
        when(image.getPackageVersions()).thenReturn(packageVersions);
        Package aPackage = new Package();
        aPackage.setName(packageName);
        aPackage.setPrewarmed(false);
        Package prewarmedPackage = new Package();
        prewarmedPackage.setName(packageName);
        prewarmedPackage.setPrewarmed(true);
        when(instanceMetadataUpdater.getPackages()).thenReturn(Lists.newArrayList(aPackage, prewarmedPackage));
        when(instanceMetadataUpdater.isPackagesVersionEqual(anyString(), anyString())).thenReturn(true);
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setImage(new Json(new com.sequenceiq.cloudbreak.cloud.model.Image(
                "image", Collections.emptyMap(), "os", "ostype", "catalogn", "catalogu", "id",
                packageVersions, null, null)));
        Set<InstanceMetadataView> instanceMetaDataSet = Collections.singleton(instanceMetaData);

        CheckResult result = underTest.compareImageAndInstancesMandatoryPackageVersion(statedImage, instanceMetaDataSet);

        assertEquals(EventStatus.OK, result.getStatus());
    }

    @Test
    public void compareImageAndInstancesMandatoryPackageVersionPrewarmedOk() {
        String packageName = "package";
        Map<String, String> packageVersions = Collections.singletonMap(packageName, "1");
        when(statedImage.getImage()).thenReturn(image);
        when(image.isPrewarmed()).thenReturn(true);
        when(image.getPackageVersions()).thenReturn(packageVersions);
        Package aPackage = new Package();
        aPackage.setName(packageName);
        aPackage.setPrewarmed(true);
        when(instanceMetadataUpdater.getPackages()).thenReturn(Collections.singletonList(aPackage));
        when(instanceMetadataUpdater.isPackagesVersionEqual(anyString(), anyString())).thenReturn(true);
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setImage(new Json(new com.sequenceiq.cloudbreak.cloud.model.Image(
                "image", Collections.emptyMap(), "os", "ostype", "catalogn", "catalogu", "id",
                packageVersions, null, null)));
        Set<InstanceMetadataView> instanceMetaDataSet = Collections.singleton(instanceMetaData);

        CheckResult result = underTest.compareImageAndInstancesMandatoryPackageVersion(statedImage, instanceMetaDataSet);

        assertEquals(EventStatus.OK, result.getStatus());
    }

    @Test
    public void compareImageAndInstancesMandatoryPackageVersionMissingPackageInImage() {
        String packageName = "package";
        Map<String, String> packageVersions = Collections.singletonMap(packageName, "1");
        when(statedImage.getImage()).thenReturn(image);
        when(image.isPrewarmed()).thenReturn(true);
        when(image.getPackageVersions()).thenReturn(Collections.emptyMap());
        Package aPackage = new Package();
        aPackage.setName(packageName);
        aPackage.setPrewarmed(true);
        when(instanceMetadataUpdater.getPackages()).thenReturn(Collections.singletonList(aPackage));
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setImage(new Json(new com.sequenceiq.cloudbreak.cloud.model.Image(
                "image", Collections.emptyMap(), "os", "ostype", "catalogn", "catalogu", "id",
                packageVersions, null, null)));
        Set<InstanceMetadataView> instanceMetaDataSet = Collections.singleton(instanceMetaData);

        CheckResult result = underTest.compareImageAndInstancesMandatoryPackageVersion(statedImage, instanceMetaDataSet);

        assertEquals(EventStatus.FAILED, result.getStatus());
    }

    @Test
    public void compareImageAndInstancesMandatoryPackageVersionDifferentPackageVersionInImage() {
        String packageName = "package";
        Map<String, String> packageVersions = Collections.singletonMap(packageName, "1");
        when(statedImage.getImage()).thenReturn(image);
        when(image.isPrewarmed()).thenReturn(true);
        when(image.getPackageVersions()).thenReturn(Collections.singletonMap(packageName, "2"));
        Package aPackage = new Package();
        aPackage.setName(packageName);
        aPackage.setPrewarmed(true);
        when(instanceMetadataUpdater.getPackages()).thenReturn(Collections.singletonList(aPackage));
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setImage(new Json(new com.sequenceiq.cloudbreak.cloud.model.Image(
                "image", Collections.emptyMap(), "os", "ostype", "catalogn", "catalogu", "id",
                packageVersions, null, null)));
        Set<InstanceMetadataView> instanceMetaDataSet = Collections.singleton(instanceMetaData);

        CheckResult result = underTest.compareImageAndInstancesMandatoryPackageVersion(statedImage, instanceMetaDataSet);

        assertEquals(EventStatus.FAILED, result.getStatus());
    }

    @Test
    public void checkInstancesHaveAllMandatoryPackageVersionOk() {
        when(instanceMetadataUpdater.collectInstancesWithMissingPackageVersions(anySet())).thenReturn(Collections.emptyMap());

        CheckResult result = underTest.checkInstancesHaveAllMandatoryPackageVersion(Collections.emptySet());

        assertEquals(EventStatus.OK, result.getStatus());
    }

    @Test
    public void checkInstancesHaveAllMandatoryPackageVersionNok() {
        when(instanceMetadataUpdater.collectInstancesWithMissingPackageVersions(anySet()))
                .thenReturn(Collections.singletonMap("instance", Collections.singletonList("package")));

        CheckResult result = underTest.checkInstancesHaveAllMandatoryPackageVersion(Collections.emptySet());

        assertEquals(EventStatus.FAILED, result.getStatus());
    }

    @Test
    public void checkInstancesHaveMultiplePackageVersionsOk() {
        when(instanceMetadataUpdater.collectPackagesWithMultipleVersions(anySet())).thenReturn(Collections.emptyList());

        CheckResult result = underTest.checkInstancesHaveMultiplePackageVersions(Collections.emptySet());

        assertEquals(EventStatus.OK, result.getStatus());
    }

    @Test
    public void checkInstancesHaveMultiplePackageVersionsNok() {
        when(instanceMetadataUpdater.collectPackagesWithMultipleVersions(anySet())).thenReturn(Collections.singletonList("package"));

        CheckResult result = underTest.checkInstancesHaveMultiplePackageVersions(Collections.emptySet());

        assertEquals(EventStatus.FAILED, result.getStatus());
    }
}