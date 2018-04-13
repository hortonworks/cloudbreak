package com.sequenceiq.cloudbreak.converter.mapper;

import com.sequenceiq.cloudbreak.api.model.StackDescriptor;
import com.sequenceiq.cloudbreak.api.model.imagecatalog.StackRepoDetailsJson;
import com.sequenceiq.cloudbreak.cloud.model.component.StackInfo;
import com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor"
)
@Component
public class StackInfoMapperImpl implements StackInfoMapper {

    @Override
    public StackDescriptor mapStackInfoToStackDescriptor(StackInfo stackInfo) {
        if ( stackInfo == null ) {
            return null;
        }

        StackDescriptor stackDescriptor = new StackDescriptor();

        stackDescriptor.setVersion( stackInfo.getVersion() );
        stackDescriptor.setMinAmbari( stackInfo.getMinAmbari() );
        stackDescriptor.setRepo( stackRepoDetailsToStackRepoDetailsJson( stackInfo.getRepo() ) );

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
}
