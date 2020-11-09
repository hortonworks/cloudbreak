az ad sp create-for-rbac \
    --name http://{app-name} \
    --role "Storage Blob Data Contributor" \
    --scopes /subscriptions/{subscriptionId}/resourceGroups/{resource-group-name}/providers/Microsoft.Storage/storageAccounts/{storage-account-name}/blobServices/default/containers/{container-name}