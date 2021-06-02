package com.sequenceiq.cloudbreak.cloud.aws.common.util;

import static com.amazonaws.util.StringUtils.isNullOrEmpty;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

import com.amazonaws.services.ec2.model.DescribeRouteTablesRequest;
import com.amazonaws.services.ec2.model.DescribeRouteTablesResult;
import com.amazonaws.services.ec2.model.RouteTable;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;

public class AwsPageCollector {
    private AwsPageCollector() {
    }

    public static List<RouteTable> getAllRouteTables(AmazonEc2Client ec2Client, DescribeRouteTablesRequest request) {
        List<RouteTable> routeTableList = collectPages(ec2Client::describeRouteTables, request,
                DescribeRouteTablesResult::getRouteTables,
                DescribeRouteTablesResult::getNextToken,
                DescribeRouteTablesRequest::setNextToken);
        return routeTableList;
    }

    public static <S, P, R> List<S> collectPages(Function<P, R> resultProvider, P request, Function<R, List<S>> listFromResultGetter,
            Function<R, String> tokenFromResultGetter, BiConsumer<P, String> tokenToRequestSetter) {
        String nextToken;
        List<S> list = new ArrayList<>();
        do {
            R res = resultProvider.apply(request);
            list.addAll(listFromResultGetter.apply(res));

            nextToken = tokenFromResultGetter.apply(res);
            tokenToRequestSetter.accept(request, nextToken);
        } while (!isNullOrEmpty(nextToken));

        return list;
    }
}
