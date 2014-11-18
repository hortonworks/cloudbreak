#! /bin/bash

: ${EXTERNAL_ID=provision-ambari}
: ${ROLE_NAME=cb-access}

cat > cb-assume-role-policy.json << EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Action": "sts:AssumeRole",
      "Principal": {
        "AWS": "arn:aws:iam::755047402263:root"
      },
      "Effect": "Allow",
      "Condition": {
        "StringEquals": {
          "sts:ExternalId": "$EXTERNAL_ID"
        }
      },
      "Sid": ""
    }
  ]
}
EOF

cat > cb-iam-role-policy.json << EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "cloudformation:*"
      ],
      "Resource": [
        "*"
      ]
    },
    {
      "Effect": "Allow",
      "Action": [
        "ec2:*"
      ],
      "Resource": [
        "*"
      ]
    },
    {
      "Effect": "Allow",
      "Action": [
        "sns:*"
      ],
      "Resource": [
        "*"
      ]
    },
    {
      "Effect": "Allow",
      "Action": [
        "iam:PassRole"
      ],
      "Resource": [
        "*"
      ]
    },
    {
      "Effect": "Allow",
      "Action": [
        "autoscaling:*"
      ],
      "Resource": [
        "*"
      ]
    }
  ]
}
EOF

if [ ! -f cb-assume-role-policy.json ]
then
  echo "cb-assume-role-policy.json file not found in the current directory"
elif [ ! -f cb-iam-role-policy.json ]
then
  echo "cb-iam-role-policy.json file not found in the current directory"
else
  aws iam create-role --output text --query Role.Arn --role-name $ROLE_NAME --assume-role-policy-document file://cb-assume-role-policy.json
  aws iam put-role-policy --role-name $ROLE_NAME --policy-name cb-policy --policy-document file://cb-iam-role-policy.json
fi
