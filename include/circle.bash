circle-init() {
    # readonly CircleCI token for accessing build artifacts
    : ${CIRCLE_TOKEN:=da6ded628881d187fdab480349807c929b6af287}
}

cci-latest() {
    declare desc="Get latest build number from CircleCI"
    declare project=$1 branch=${2:-master}
    : ${project:?}

    local account=""

    if [[ $project =~ / ]]; then
        account=${project%/*}
        project=${project#*/}
    else
      account=$(circle me|jq .name -r)
      debug account: $account
    fi
    
    cci-latest-org $account $project $branch
}

cci-latest-org() {
    declare desc="Get latest build number from CircleCI"
    declare user=$1 project=$2 branch=${3:-master}
    : ${project:?}
    : ${user:?}

    local latest=$(circle "project/$user/$project/tree/$branch?filter=completed&limit=1" |jq .[0].build_num)
    debug latest build: $latest

    circle  "project/$user/$project/$latest/artifacts" | jq .[].url -r | grep -i $(uname)
}

circle() {
    declare path=$1
    : ${CIRCLE_TOKEN:? required}
    shift
    
    local url="https://circleci.com/api/v1/$path"
    debug CURL: $url
    
    curl -s -G \
        -d circle-token=$CIRCLE_TOKEN \
        -H "Accept: application/json" \
        "$url" "$@"
}
