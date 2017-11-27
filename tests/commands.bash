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
    $CB_BIN cluster describe "$@"
}

function start-cluster() {
    $CB_BIN cluster start "$@"
}

function stop-cluster() {
    $CB_BIN cluster stop "$@"
}

function list-clusters() {
    $CB_BIN cluster list
}

function list-blueprints() {
     $CB_BIN blueprint list
 }

 function create-blueprint {
     $CB_BIN blueprint  create "$@"
 }

  function delete-blueprint() {
     $CB_BIN blueprint delete --name "$@"
 }

 function describe-blueprint() {
    $CB_BIN blueprint describe --name "$@"
 }


function create-credential-aws-key() {
        $CB_BIN credential create aws key-based "$@" $AWS_ARGS_KEY
   }

function create-credential-aws-role() {
        $CB_BIN credential create aws role-based "$@" $AWS_ARGS_ROLE
   }

function create-credential-openstack-v2() {
        $CB_BIN credential create openstack keystone-v2 "$@" $OPENSTACK_ARGS
   }

function create-credential-openstack-v3() {
        $CB_BIN credential create openstack keystone-v3 "$@" $OPENSTACK_ARGS
   }

function create-credential-azure() {
        $CB_BIN credential create azure app-based "$@" $AZURE_ARGS
   }

function create-credential-gcp() {
        $CB_BIN credential create gcp "$@" $GCP_ARGS
   }

function list-credentials() {
        $CB_BIN credential  list
   }

function describe-credential() {
        $CB_BIN credential describe --name openstack
   }
function delete-credential() {
        $CB_BIN credential delete --name cli-aws-role
   }

function create-cluster() {
    $CB_BIN cluster create "$@"
}

function delete-cluster() {
    $CB_BIN cluster delete "$@"
}

function create-recipe-pre-ambari-start-url() {
    $CB_BIN recipe  create from-url  --name recipe-post-cli --execution-type pre-ambari-start --url ${RECIPE_URL}
}

function create-recipe-post-ambari-start-url() {
    $CB_BIN  recipe create from-url  --name recipe-pre-cli --execution-type post-ambari-start --url ${RECIPE_URL}
}

function create-recipe-post-cluster-install-url() {
    $CB_BIN  recipe create from-url  --name recipe-pre-cli --execution-type post-cluster-install --url ${RECIPE_URL}
}

function list-recipes() {
    $CB_BIN  recipe list
}

function delete-recipe() {
    $CB_BIN recipe  delete --name recipe-pre-cli
}

function describe-recipe() {
    $CB_BIN  recipe describe --name recipe-pre-cli
}

function scale-cluster() {
    $CB_BIN  cluster  scale "$@"
}

function repair-cluster() {
    $CB_BIN  cluster  repair --name ${CLUSTER_NAME}
}

function sync-cluster() {
    $CB_BIN cluster  sync --nanme ${CLUSTER_NAME}

}

function change-ambari-password() {
    $CB_BIN  change-ambari-password --name ${CLUSTER_NAME} --ambari-user admin --old-password admin --new-password adminnew

}
