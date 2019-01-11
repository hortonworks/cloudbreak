package com.sequenceiq.cloudbreak.converter.mapper;

import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.StackRepoDetailsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.StackDescriptorV4;
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
    public StackDescriptorV4 mapStackInfoToStackDescriptor(StackInfo stackInfo, Map<String, List<ManagementPackComponent>> mpacks) {
        if ( stackInfo == null && mpacks == null ) {
            return null;
        }

        StackDescriptorV4 stackDescriptorV4 = new StackDescriptorV4();

        if ( stackInfo != null ) {
            stackDescriptorV4.setVersion( stackInfo.getVersion() );
            stackDescriptorV4.setMinAmbari( stackInfo.getMinAmbari() );
            stackDescriptorV4.setRepo( defaultStackRepoDetailsToStackRepoDetailsV4Response( stackInfo.getRepo() ) );
        }
        if ( mpacks != null ) {
            stackDescriptorV4.setMpacks( managementPackComponentListMapMapper.mapManagementPackComponentMap( mpacks ) );
        }

        return stackDescriptorV4;
    }

    protected StackRepoDetailsV4Response defaultStackRepoDetailsToStackRepoDetailsV4Response(DefaultStackRepoDetails defaultStackRepoDetails) {
        if ( defaultStackRepoDetails == null ) {
            return null;
        }

        StackRepoDetailsV4Response stackRepoDetailsV4Response = new StackRepoDetailsV4Response();

        Map<String, String> map = defaultStackRepoDetails.getStack();
        if ( map != null ) {
            stackRepoDetailsV4Response.setStack( new HashMap<String, String>( map ) );
        }
        else {
            stackRepoDetailsV4Response.setStack( null );
        }
        Map<String, String> map1 = defaultStackRepoDetails.getUtil();
        if ( map1 != null ) {
            stackRepoDetailsV4Response.setUtil( new HashMap<String, String>( map1 ) );
        }
        else {
            stackRepoDetailsV4Response.setUtil( null );
        }

        return stackRepoDetailsV4Response;
    }
}
