az ad sp create-for-rbac \
    --name http://{app-name} \
    --role Contributor \
    --scopes /subscriptions/{subscriptionId}