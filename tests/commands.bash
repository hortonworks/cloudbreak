COMMON_ARGS_WO_CLUSTER=" --server ${CLOUD_URL} --username ${EMAIL} --password ${PASSWORD}  "
COMMON_ARGS=" --cluster-name testcluster --server ${CLOUD_URL} --username ${EMAIL} --password ${PASSWORD} "

AWS_ARGS_KEY=" --name cli-aws-key --access-key testaccess --secret-key testsecretkey "
AWS_ARGS_ROLE=" --name cli-aws-role --role-arn  testawsrole "

OPENSTACK_ARGS=" --name cli-openstack --tenant-user testuser  --tenant-password testpassword --tenant-name testtenant --endpoint http://1.1.1.1:5000/v2.0"
AZURE_ARGS="--name cli-azure --subscription-id aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa --tenant-id aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa --app-id aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa --app-password testpassword"
GCP_ARGS="--name cli-gcp --project-id testprojet --service-account-id testuser@siq-haas.iam.gserviceaccount.com --service-account-private-key-file test.p12"

CB_BIN="/usr/local/bin/cb"


function configure-cb() {
    $CB_BIN configure "$@" $COMMON_ARGS_WO_CLUSTER
}

function describe-cluster() {
    $CB_BIN describe-cluster "$@"
}

function start-cluster() {
    $CB_BIN start-cluster "$@"
}

function stop-cluster() {
    $CB_BIN stop-cluster "$@"
}

function list-clusters() {
    $CB_BIN list-clusters
}

function list-blueprints() {
     $CB_BIN list-blueprints
 }

 function create-blueprint {
     $CB_BIN create-blueprint "$@"
 }

  function delete-blueprint() {
     $CB_BIN delete-blueprint --name "$@"
 }

 function describe-blueprint() {
    $CB_BIN describe-blueprint --name "$@"
 }


function create-credential-aws-key() {
        $CB_BIN create-credential aws key-based "$@" $AWS_ARGS_KEY
   }

function create-credential-aws-role() {
        $CB_BIN create-credential aws role-based "$@" $AWS_ARGS_ROLE
   }

function create-credential-openstack-v2() {
        $CB_BIN create-credential openstack keystone-v2 "$@" $OPENSTACK_ARGS
   }

function create-credential-openstack-v3() {
        $CB_BIN create-credential openstack keystone-v3 "$@" $OPENSTACK_ARGS
   }

function create-credential-azure() {
        $CB_BIN create-credential azure app-based "$@" $AZURE_ARGS
   }

function create-credential-gcp() {
        $CB_BIN create-credential gcp "$@" $GCP_ARGS
   }

function list-credentials() {
        $CB_BIN list-credentials
   }

function describe-credential() {
        $CB_BIN describe-credential --name openstack
   }
function delete-credential() {
        $CB_BIN delete-credential --name cli-aws-role
   }

function create-cluster() {
    $CB_BIN create-cluster "$@"
}

function delete-cluster() {
    $CB_BIN delete-cluster "$@"
}

function create-recipe-pre-ambari-start-url() {
    $CB_BIN  create-recipe from-url  --name recipe-post-cli --execution-type pre-ambari-start --url ${RECIPE_URL}
}

function create-recipe-post-ambari-start-url() {
    $CB_BIN  create-recipe from-url  --name recipe-pre-cli --execution-type post-ambari-start --url ${RECIPE_URL}
}

function create-recipe-post-cluster-install-url() {
    $CB_BIN  create-recipe from-url  --name recipe-pre-cli --execution-type post-cluster-install --url ${RECIPE_URL}
}

function list-recipes() {
    $CB_BIN  list-recipes
}

function delete-recipe() {
    $CB_BIN  delete-recipe --name recipe-pre-cli
}

function describe-recipe() {
    $CB_BIN  describe-recipe --name recipe-pre-cli
}

function scale-cluster() {
    $CB_BIN  scale-cluster  "$@"
}

function repair-cluster() {
    $CB_BIN  repair-cluster  --name ${CLUSTER_NAME}
}

function sync-cluster() {
    $CB_BIN  sync-cluster --nanme ${CLUSTER_NAME}

}

function change-ambari-password() {
    $CB_BIN  change-ambari-password --name ${CLUSTER_NAME} --ambari-user admin --old-password admin --new-password adminnew

}
