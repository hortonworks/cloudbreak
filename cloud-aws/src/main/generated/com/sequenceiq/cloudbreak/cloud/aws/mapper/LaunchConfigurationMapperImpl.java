package com.sequenceiq.cloudbreak.cloud.aws.mapper;

import com.amazonaws.services.autoscaling.model.BlockDeviceMapping;
import com.amazonaws.services.autoscaling.model.CreateLaunchConfigurationRequest;
import com.amazonaws.services.autoscaling.model.LaunchConfiguration;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Generated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor"
)
@Component
public class LaunchConfigurationMapperImpl implements LaunchConfigurationMapper {

    @Autowired
    private EmptyToNullStringMapper emptyToNullStringMapper;

    @Override
    public CreateLaunchConfigurationRequest mapExistingLaunchConfigToRequest(LaunchConfiguration launchConfiguration) {
        if ( launchConfiguration == null ) {
            return null;
        }

        CreateLaunchConfigurationRequest createLaunchConfigurationRequest = new CreateLaunchConfigurationRequest();

        createLaunchConfigurationRequest.setLaunchConfigurationName( emptyToNullStringMapper.map( launchConfiguration.getLaunchConfigurationName() ) );
        createLaunchConfigurationRequest.setImageId( emptyToNullStringMapper.map( launchConfiguration.getImageId() ) );
        createLaunchConfigurationRequest.setKeyName( emptyToNullStringMapper.map( launchConfiguration.getKeyName() ) );
        List<String> list = launchConfiguration.getSecurityGroups();
        if ( list != null ) {
            createLaunchConfigurationRequest.setSecurityGroups( new ArrayList<String>( list ) );
        }
        else {
            createLaunchConfigurationRequest.setSecurityGroups( null );
        }
        createLaunchConfigurationRequest.setClassicLinkVPCId( emptyToNullStringMapper.map( launchConfiguration.getClassicLinkVPCId() ) );
        List<String> list1 = launchConfiguration.getClassicLinkVPCSecurityGroups();
        if ( list1 != null ) {
            createLaunchConfigurationRequest.setClassicLinkVPCSecurityGroups( new ArrayList<String>( list1 ) );
        }
        else {
            createLaunchConfigurationRequest.setClassicLinkVPCSecurityGroups( null );
        }
        createLaunchConfigurationRequest.setUserData( emptyToNullStringMapper.map( launchConfiguration.getUserData() ) );
        createLaunchConfigurationRequest.setInstanceType( emptyToNullStringMapper.map( launchConfiguration.getInstanceType() ) );
        createLaunchConfigurationRequest.setKernelId( emptyToNullStringMapper.map( launchConfiguration.getKernelId() ) );
        createLaunchConfigurationRequest.setRamdiskId( emptyToNullStringMapper.map( launchConfiguration.getRamdiskId() ) );
        List<BlockDeviceMapping> list2 = launchConfiguration.getBlockDeviceMappings();
        if ( list2 != null ) {
            createLaunchConfigurationRequest.setBlockDeviceMappings( new ArrayList<BlockDeviceMapping>( list2 ) );
        }
        else {
            createLaunchConfigurationRequest.setBlockDeviceMappings( null );
        }
        createLaunchConfigurationRequest.setInstanceMonitoring( launchConfiguration.getInstanceMonitoring() );
        createLaunchConfigurationRequest.setSpotPrice( emptyToNullStringMapper.map( launchConfiguration.getSpotPrice() ) );
        createLaunchConfigurationRequest.setIamInstanceProfile( emptyToNullStringMapper.map( launchConfiguration.getIamInstanceProfile() ) );
        createLaunchConfigurationRequest.setEbsOptimized( launchConfiguration.getEbsOptimized() );
        createLaunchConfigurationRequest.setAssociatePublicIpAddress( launchConfiguration.getAssociatePublicIpAddress() );
        createLaunchConfigurationRequest.setPlacementTenancy( emptyToNullStringMapper.map( launchConfiguration.getPlacementTenancy() ) );

        return createLaunchConfigurationRequest;
    }
}
