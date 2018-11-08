az ad app create \
	--display-name cloudbreak-app \
	--password 'Cloudbreak123!' \
	--homepage ${cloudbreakAddress} \
	--identifier-uris ${identifierURI} \
	--reply-urls ${cloudbreakReplyUrl} \
	--end-date '${expirationDate}' \
	--required-resource-accesses '[{"resourceAppId":"${resourceAppId}","resourceAccess":[{"id":"${resourceAccessScopeId}","type":"Scope"}]}]'
