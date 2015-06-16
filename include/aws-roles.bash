aws-show-policy() {
    declare policyArn=$1

    local policyName=${policyArn#*/}
    debug show policy document for: $policyName
    
    #local defVersion=$(aws iam list-policy-versions \
    #    --policy-arn $policyArn \
    #    --query 'Versions[?IsDefaultVersion].VersionId' \
    #    --out text)

    local defVersion=$(aws iam get-policy \
        --policy-arn $policyArn \
        --query Policy.DefaultVersionId \
        --out text)

    aws iam get-policy-version \
        --policy-arn $policyArn \
        --version-id $defVersion \
        --query 'PolicyVersion.Document.Statement'
}

aws-show-role-assumers() {
    declare roleName=$1

    info "Assumers for role: $roleName"
    aws iam get-role \
        --role-name $roleName \
        --query Role.AssumeRolePolicyDocument.Statement[0].Principal \
        --out text
}

aws-show-role-inline-policies() {
     declare roleName=$1
    
     inlinePolicies=$(aws iam list-role-policies --role-name $roleName --query PolicyNames --out text)

     if ! [[ "$inlinePolicies" ]];then
        info NO Inline policies for role: $roleName
        return
     fi

    info Inline policies for role: $roleName
    for p in ${inlinePolicies}; do
        debug "inline policy: $p"
        aws iam get-role-policy \
            --role-name $roleName \
            --policy-name $p \
            --query "PolicyDocument.Statement[][Effect,Action[0],Resource[0]]" --out text
    done
}

aws-show-role-managed-policies() {
     declare roleName=$1
    
     attachedPolicies=$(aws iam list-attached-role-policies --role-name $roleName --query 'AttachedPolicies[].PolicyArn' --out text)

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
    : ${roleName:? required}

    aws-show-role-assumers $roleName
    aws-show-role-inline-policies $roleName
    aws-show-role-managed-policies $roleName
}

aws-assume-role() {
    declare roleArn=$1 externalId=$2 roleSession=$3

    local roleResp=$(aws sts assume-role \
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


