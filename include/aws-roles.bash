aws-init() {
    env-import CB_AWS_EXTERNAL_ID provision-ambari
    env-import AWS_ROLE_NAME cbreak-deployer

    deps-require aws
    AWS=.deps/bin/aws
}

aws-certs-upload-s3() {
    declare desc="uploads certs dir backup tgz into s3 bucket"

    local bucket=${1:-$CERTS_BUCKET}
    : ${bucket:? required}
    local regio=$($AWS s3api get-bucket-location --bucket $bucket --query LocationConstraint --out text)
    
    docker run --rm -it \
      --volumes-from cbreak_cloudbreak_1 \
      -e S3_BUCKET_NAME=$bucket \
      -e AWS_DEFAULT_REGION=$regio \
      -e BACKUP_NAME=certs \
      -e PATHS_TO_BACKUP=/certs \
      --entrypoint bash \
      tutum/dockup -c 'unset AWS_SECRET_ACCESS_KEY; unset AWS_ACCESS_KEY_ID; ./backup.sh'
}

aws-certs-restore-s3() {
    declare desc="restores certs dir backup tgz from s3 bucket"

    local bucket=${1:-$CERTS_BUCKET}
    : ${bucket:? required}
    local regio=$($AWS s3api get-bucket-location --bucket $bucket --query LocationConstraint --out text)

    mkdir -p certs
    docker rm certs-volume 2>/dev/null || true
    docker create -v $PWD/certs:/certs --name certs-volume alpine sh

    docker run --rm -it \
      --volumes-from certs-volume \
      -e S3_BUCKET_NAME=$bucket \
      -e AWS_DEFAULT_REGION=$regio \
      -e BACKUP_NAME=certs \
      -e PATHS_TO_BACKUP=/certs \
      --entrypoint bash \
      tutum/dockup -c 'unset AWS_SECRET_ACCESS_KEY; unset AWS_ACCESS_KEY_ID; ./restore.sh'

    docker rm certs-volume
}


aws-show-policy() {
    declare policyArn=$1

    local policyName=${policyArn#*/}
    debug show policy document for: $policyName
    
    #local defVersion=$($AWS iam list-policy-versions \
    #    --policy-arn $policyArn \
    #    --query 'Versions[?IsDefaultVersion].VersionId' \
    #    --out text)

    local defVersion=$($AWS iam get-policy \
        --policy-arn $policyArn \
        --query Policy.DefaultVersionId \
        --out text)

    $AWS iam get-policy-version \
        --policy-arn $policyArn \
        --version-id $defVersion \
        --query 'PolicyVersion.Document.Statement'
}

aws-show-role-assumers() {
    declare roleName=$1

    info "Assumers for role: $roleName"
    $AWS iam get-role \
        --role-name $roleName \
        --query Role.AssumeRolePolicyDocument.Statement[0].Principal \
        --out text
}

aws-show-role-inline-policies() {
     declare roleName=$1
    
     inlinePolicies=$($AWS iam list-role-policies --role-name $roleName --query PolicyNames --out text)

     if ! [[ "$inlinePolicies" ]];then
        info NO Inline policies for role: $roleName
        return
     fi

    info Inline policies for role: $roleName
    for p in ${inlinePolicies}; do
        debug "inline policy: $p"
        $AWS iam get-role-policy \
            --role-name $roleName \
            --policy-name $p \
            --query "PolicyDocument.Statement[][Effect,Action[0],Resource[0]]" --out text
    done
}

aws-list-roles() {
    declare desc="Lists all roles available to you"

    $AWS iam list-roles --query Roles[].RoleName --out table
}

aws-show-role-managed-policies() {
     declare roleName=$1
    
     attachedPolicies=$($AWS iam list-attached-role-policies --role-name $roleName --query 'AttachedPolicies[].PolicyArn' --out text)

     if ! [[ "$attachedPolicies" ]];then
         info NO attached policies for: $roleName
         return
     fi

    info Attached policies for ${roleName}: ${attachedPolicies}
    for p in $attachedPolicies; do
        aws-show-policy $p
    done
}

aws-show-role() {
    declare desc="Show assumers and policies for an AWS role"
    
    declare roleName=$1

    : ${roleName:= $AWS_ROLE_NAME}

    local arnName=$($AWS iam get-role --role-name cbreak-deployer --query 'Role.Arn' --out text)
     info "Full reference: $arnName"

    aws-show-role-assumers $roleName
    aws-show-role-inline-policies $roleName
    aws-show-role-managed-policies $roleName
}

aws-assume-role() {
    declare roleArn=$1 externalId=$2 roleSession=$3

    local roleResp=$($AWS sts assume-role \
        --role-arn $roleArn \
        --role-session-name $roleSession \
        --external-id $externalId)
    debug $roleResp

    local accesKeyId=$(echo $roleResp | jq .Credentials.AccessKeyId -r)
    local secretAccessKey=$(echo $roleResp | jq .Credentials.SecretAccessKey -r)
    local sessionToken=$(echo $roleResp | jq .Credentials.SessionToken -r)

    cat << EOF
export AWS_ACCESS_KEY_ID=$accesKeyId
export AWS_SECRET_ACCESS_KEY=$secretAccessKey
export AWS_SESSION_TOKEN=$sessionToken
EOF

cat << EOF
aaws() {
  AWS_ACCESS_KEY_ID=$accesKeyId AWS_SECRET_ACCESS_KEY=$secretAccessKey  AWS_SESSION_TOKEN=$sessionToken aws "$@"
}
EOF

}

aws-get-user-arn() {
    $AWS iam get-user --query User.Arn --out text
}

aws-get-account-id() {
    declare desc="Prints the 12 digit long AWS account id"

    local userArn=$(aws-get-user-arn)
    debug userArn=$userArn

    cut -d: -f 5 <<< "$userArn"
}

aws-generate-assume-role-policy() {
    cat << EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Action": "sts:AssumeRole",
      "Principal": {
        "AWS": "$(aws-get-account-id)"
      },
      "Effect": "Allow",
      "Condition": {
        "StringEquals": {
          "sts:ExternalId": "$CB_AWS_EXTERNAL_ID"
        }
      }
    }
  ]
}
EOF
}

aws-generate-inline-role-policy() {
    cat << EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [ "cloudformation:*" ],
      "Resource": [ "*" ]
    },
    {
      "Effect": "Allow",
      "Action": [ "ec2:*" ],
      "Resource": [ "*" ]
    },
    {
      "Effect": "Allow",
      "Action": [ "iam:PassRole" ],
      "Resource": [ "*" ]
    },
    {
      "Effect": "Allow",
      "Action": [ "autoscaling:*" ],
      "Resource": [ "*" ]
    }
  ]
}
EOF
}

aws-delete-role() {
    declare desc="Deletes an aws iam role, removes all inline policies"

    declare roleName=$1
    : ${roleName:? required}
    
    local inlinePolicies=$($AWS iam list-role-policies --role-name $roleName --query PolicyNames --out text)

    for pol in $inlinePolicies; do
        debug delete inlinePolicy: $pol
        $AWS iam delete-role-policy --role-name $roleName --policy-name $pol
    done

    local attachedPolicies=$($AWS iam list-attached-role-policies --role-name $roleName --query AttachedPolicies[].PolicyArn --out text)
    for pol in $attachedPolicies; do
        debug detach policy: $pol
        $AWS iam detach-role-policy --role-name $roleName --policy-arn $pol
    done
    $AWS iam delete-role --role-name $roleName
}

aws-generate-role() {
    declare desc="Generates an aws iam role for cloudbreak provisioning on AWS"

    aws-generate-role-files <(aws-generate-assume-role-policy) <(aws-generate-inline-role-policy)
}

aws-generate-role-files() {
    declare assumePolicyFile=$1 inlinePolicyFile=$2
    
    local roleResp=$($AWS iam create-role \
        --output text \
        --query Role.Arn \
        --role-name $AWS_ROLE_NAME \
        --assume-role-policy-document file://${assumePolicyFile} \
    )
    info "role created: $roleResp"
    
    local putPolicyResp=$($AWS iam put-role-policy \
        --role-name $AWS_ROLE_NAME \
        --policy-name cb-policy \
        --policy-document file://${inlinePolicyFile}
    )
    debug put policy resp: $putPolicyResp

}
