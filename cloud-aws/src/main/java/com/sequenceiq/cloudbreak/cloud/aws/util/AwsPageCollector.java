package com.sequenceiq.cloudbreak.cloud.aws.util;

import static com.amazonaws.util.StringUtils.isNullOrEmpty;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeRouteTablesRequest;
import com.amazonaws.services.ec2.model.DescribeRouteTablesResult;
import com.amazonaws.services.ec2.model.RouteTable;

public class AwsPage {
    private AwsPage() {
    }

    public static List<RouteTable> getAllRouteTables(AmazonEC2Client ec2Client, DescribeRouteTablesRequest request ) {
        List<RouteTable> routeTableList = collectPages(ec2Client::describeRouteTables, request,
                DescribeRouteTablesResult::getRouteTables,
                DescribeRouteTablesResult::getNextToken);
        return routeTableList;
    }

    public static <S, P, R> List<S> collectPages(Function<P,R> resultProvider, P request, Function<R, List<S>> getter, Function<R, String> token) {
        String nextToken;
        List<S> list = new ArrayList<>();
        {
            R res = resultProvider.apply(request);
            list.addAll(getter.apply(res));

            nextToken = token.apply(res);
        } while (!isNullOrEmpty(nextToken));

        return list;
    }
}
