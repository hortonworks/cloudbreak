package com.sequenceiq.cloudbreak.converter.mapper;

import com.sequenceiq.cloudbreak.api.model.imagecatalog.StackRepoDetailsJson;
import com.sequenceiq.cloudbreak.api.model.stack.StackDescriptor;
import com.sequenceiq.cloudbreak.cloud.model.component.DefaultStackRepoDetails;
import com.sequenceiq.cloudbreak.cloud.model.component.ManagementPackComponent;
import com.sequenceiq.cloudbreak.cloud.model.component.StackInfo;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Generated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor"
)
@Component
public class StackInfoMapperImpl implements StackInfoMapper {

    @Autowired
    private ManagementPackComponentListMapMapper managementPackComponentListMapMapper;

    @Override
    public StackDescriptor mapStackInfoToStackDescriptor(StackInfo stackInfo, Map<String, List<ManagementPackComponent>> mpacks) {
        if ( stackInfo == null && mpacks == null ) {
            return null;
        }

        StackDescriptor stackDescriptor = new StackDescriptor();

        if ( stackInfo != null ) {
            stackDescriptor.setVersion( stackInfo.getVersion() );
            stackDescriptor.setMinAmbari( stackInfo.getMinAmbari() );
            stackDescriptor.setRepo( defaultStackRepoDetailsToStackRepoDetailsJson( stackInfo.getRepo() ) );
        }
        if ( mpacks != null ) {
            stackDescriptor.setMpacks( managementPackComponentListMapMapper.mapManagementPackComponentMap( mpacks ) );
        }

        return stackDescriptor;
    }

    protected StackRepoDetailsJson defaultStackRepoDetailsToStackRepoDetailsJson(DefaultStackRepoDetails defaultStackRepoDetails) {
        if ( defaultStackRepoDetails == null ) {
            return null;
        }

        StackRepoDetailsJson stackRepoDetailsJson = new StackRepoDetailsJson();

        Map<String, String> map = defaultStackRepoDetails.getStack();
        if ( map != null ) {
            stackRepoDetailsJson.setStack( new HashMap<String, String>( map ) );
        }
        else {
            stackRepoDetailsJson.setStack( null );
        }
        Map<String, String> map1 = defaultStackRepoDetails.getUtil();
        if ( map1 != null ) {
            stackRepoDetailsJson.setUtil( new HashMap<String, String>( map1 ) );
        }
        else {
            stackRepoDetailsJson.setUtil( null );
        }

        return stackRepoDetailsJson;
    }
}
