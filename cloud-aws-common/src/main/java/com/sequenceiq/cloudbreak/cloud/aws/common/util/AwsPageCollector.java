package com.sequenceiq.cloudbreak.cloud.aws.common.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;

import software.amazon.awssdk.services.ec2.model.DescribeRouteTablesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeRouteTablesResponse;
import software.amazon.awssdk.services.ec2.model.RouteTable;

@Component
public class AwsPageCollector {

    public List<RouteTable> getAllRouteTables(AmazonEc2Client ec2Client, DescribeRouteTablesRequest request) {
        List<RouteTable> routeTableList = collectPages(ec2Client::describeRouteTables, request,
                DescribeRouteTablesResponse::routeTables,
                DescribeRouteTablesResponse::nextToken,
                (req, token) -> req.toBuilder().nextToken(token).build());
        return routeTableList;
    }

    public <S, P, R> List<S> collectPages(Function<P, R> resultProvider, P request, Function<R, List<S>> listFromResponseGetter,
            Function<R, String> tokenFromResponseGetter, BiFunction<P, String, P> tokenToRequestSetter) {
        String nextToken;
        List<S> list = new ArrayList<>();
        do {
            R res = resultProvider.apply(request);
            list.addAll(listFromResponseGetter.apply(res));

            nextToken = tokenFromResponseGetter.apply(res);
            request = tokenToRequestSetter.apply(request, nextToken);
        } while (StringUtils.isNotEmpty(nextToken));

        return list;
    }
}
