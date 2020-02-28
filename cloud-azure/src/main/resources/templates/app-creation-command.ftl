az ad sp create-for-rbac \
    --name http://cloudbreak-app \
    --role Contributor \
    --scopes /subscriptions/{subscriptionId}