#!/bin/bash

set -e

msg_regex='^CB-[0-9]+\s?'

echo "[Starting] JIRA ticket update"

github_pull_request_link=https://github.infra.cloudera.com/$PROJECT/pull/$PULL_REQUEST_NUMBER
commitMessage=$(git log -n 1 --pretty=%B origin/$BRANCH)
echo commit message was: $commitMessage
echo The pull request link is: $github_pull_request_link

if echo $commitMessage | grep -iqE "$msg_regex"; then
  ticket_id=$(echo $commitMessage | grep -Eo '^(\w+/)?(\w+[-_])?[0-9]+')
	if [[ ! -z "$ticket_id" ]]
	then
    echo The ticket is: https://jira.cloudera.com/browse/$ticket_id
    curl -k -D- -H "Authorization: Bearer $JIRA_TOKEN" \
      -X PUT --data '{"fields":{"customfield_10011": "'"$github_pull_request_link"'"}}' \
      -H "Content-Type: application/json" https://jira.cloudera.com/rest/api/latest/issue/$ticket_id
  else
    echo "The ticket id is empty."
  fi
fi

echo "[Finished] JIRA ticket update"

