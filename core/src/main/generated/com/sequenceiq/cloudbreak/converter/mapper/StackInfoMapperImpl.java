package com.sequenceiq.cloudbreak.converter.mapper;

import com.sequenceiq.cloudbreak.api.model.imagecatalog.ManagementPackEntry;
import com.sequenceiq.cloudbreak.api.model.imagecatalog.StackRepoDetailsJson;
import com.sequenceiq.cloudbreak.api.model.stack.StackDescriptor;
import com.sequenceiq.cloudbreak.cloud.model.component.ManagementPackComponent;
import com.sequenceiq.cloudbreak.cloud.model.component.StackInfo;
import com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor"
)
@Component
public class StackInfoMapperImpl implements StackInfoMapper {

    @Override
    public StackDescriptor mapStackInfoToStackDescriptor(StackInfo stackInfo, List<ManagementPackComponent> mpacks) {
        if ( stackInfo == null && mpacks == null ) {
            return null;
        }

        StackDescriptor stackDescriptor = new StackDescriptor();

        if ( stackInfo != null ) {
            stackDescriptor.setVersion( stackInfo.getVersion() );
            stackDescriptor.setMinAmbari( stackInfo.getMinAmbari() );
            stackDescriptor.setRepo( stackRepoDetailsToStackRepoDetailsJson( stackInfo.getRepo() ) );
        }
        if ( mpacks != null ) {
            stackDescriptor.setMpacks( managementPackComponentListToManagementPackEntryList( mpacks ) );
        }

        return stackDescriptor;
    }

    protected StackRepoDetailsJson stackRepoDetailsToStackRepoDetailsJson(StackRepoDetails stackRepoDetails) {
        if ( stackRepoDetails == null ) {
            return null;
        }

        StackRepoDetailsJson stackRepoDetailsJson = new StackRepoDetailsJson();

        Map<String, String> map = stackRepoDetails.getStack();
        if ( map != null ) {
            stackRepoDetailsJson.setStack( new HashMap<String, String>( map ) );
        }
        else {
            stackRepoDetailsJson.setStack( null );
        }
        Map<String, String> map1 = stackRepoDetails.getUtil();
        if ( map1 != null ) {
            stackRepoDetailsJson.setUtil( new HashMap<String, String>( map1 ) );
        }
        else {
            stackRepoDetailsJson.setUtil( null );
        }

        return stackRepoDetailsJson;
    }

    protected ManagementPackEntry managementPackComponentToManagementPackEntry(ManagementPackComponent managementPackComponent) {
        if ( managementPackComponent == null ) {
            return null;
        }

        ManagementPackEntry managementPackEntry = new ManagementPackEntry();

        managementPackEntry.setMpackUrl( managementPackComponent.getMpackUrl() );

        return managementPackEntry;
    }

    protected List<ManagementPackEntry> managementPackComponentListToManagementPackEntryList(List<ManagementPackComponent> list) {
        if ( list == null ) {
            return null;
        }

        List<ManagementPackEntry> list1 = new ArrayList<ManagementPackEntry>( list.size() );
        for ( ManagementPackComponent managementPackComponent : list ) {
            list1.add( managementPackComponentToManagementPackEntry( managementPackComponent ) );
        }

        return list1;
    }
}
