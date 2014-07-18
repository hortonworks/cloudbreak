#! /bin/bash
if [ ! -f cb-assume-role-policy.json ]
then
  echo "cb-assume-role-policy.json file not found in the current directory"
elif [ ! -f cb-iam-role-policy.json ]
then
  echo "cb-iam-role-policy.json file not found in the current directory"
else
  aws iam create-role --output text --query Role.Arn --role-name cb-access --assume-role-policy-document file://cb-assume-role-policy.json
  aws iam put-role-policy --role-name cb-access --policy-name cb-policy --policy-document file://cb-iam-role-policy.json
fi
