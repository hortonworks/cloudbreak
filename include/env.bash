
declare -a _env

env-validate() {
	declare var="$1" pattern="$2" patterntext="$3"

	if [[ ${!var} == $pattern ]]; then
		echo "!! Imported variable $var contains $patterntext." | red
		_exit 3
	fi
}

env-import() {
	declare var="$1" default="$2"
	if [[ -z "${!var+x}" ]]; then
		if [[ -z "${2+x}" ]]; then
			echo "!! Imported variable $var must be set in profile or environment." | red
			_exit 2
		else
			export $var="$default"
		fi
	fi
	_env+=($var)
}

env-show() {
	declare desc="Shows relevant environment variables, in human readable format"

    cloudbreak-config
    migrate-config
	local longest=0
	for var in "${_env[@]}"; do
		if [[ "${#var}" -gt "$longest" ]]; then
			longest="${#var}"
		fi
	done
	for var in "${_env[@]}"; do
		printf "%-${longest}s = %s [%s]\n" "$var" "$(_env-description $var)" "${!var}"
	done
}

env-export() {
	declare desc="Shows relevant environment variables, in a machine friendly format."

    # TODO cloudbreak config shouldnt be called here ...
    cloudbreak-config
    migrate-config
	for var in "${_env[@]}"; do
		printf 'export %s=%s\n' "$var" "${!var}"
	done
}

_env-description() {
echo '''
ADDRESS_RESOLVING_TIMEOUT - DNS lookup timeout for internal service discovery
AWS_ACCESS_KEY_ID - Access key of the AWS account
AWS_ROLE_NAME - Name of the AWS role for the `cbd aws [generate-rol, show role]` commands
AWS_SECRET_ACCESS_KEY - Secret access key of the AWS account
AZURE_SUBSCRIPTION_ID - Azure subscription ID for interactive login in Web UI
AZURE_TENANT_ID - Azure tenant ID for interactive login in Web UI
CAPTURE_CRON_EXPRESSION - SmartSense bundle generation time interval in Cron format
CBD_CERT_ROOT_PATH - Path where deployer stores Cloudbreak certificates
CBD_LOG_NAME - Name of the Cloudbreak log file
CBD_TRAEFIK_TLS - Path inside of the Traefik container where TLS files located
CB_AWS_CUSTOM_CF_TAGS - Comma separated list of AWS CloudFormation Stack tags
CB_AWS_DEFAULT_CF_TAG - Default tag AWS CloudFormation Stack
CB_AWS_DEFAULT_INBOUND_SECURITY_GROUP - Default inbound policy name for AWS CloudFormation Stack
CB_AWS_EXTERNAL_ID - External ID of the assume role policy
CB_AWS_HOSTKEY_VERIFY - Enables host fingerprint verification on AWS
CB_AWS_VPC - Configures the VPC id on AWS if it is the same as provisioned cluster
CB_BLUEPRINT_DEFAULTS - Comma separated list of the default blueprints what Cloudbreak initialize in database
CB_BYOS_DFS_DATA_DIR - Deprecated - Default data dir for BYOP orchestrators
CB_COMPONENT_CLUSTER_ID - SmartSense component cluster ID
CB_COMPONENT_ID - SmartSense component ID
CB_COMPOSE_PROJECT - Name of the Docker Compose project, will appear in container names too
CB_DB_ENV_DB - Name of the Cloudbreak database
CB_DB_ENV_PASS - Password for the Cloudbreak database authentication
CB_DB_ENV_SCHEMA - Used schema in the Cloudbreak database
CB_DB_ENV_USER - User for the Cloudbreak database authentication
CB_DB_ROOT_PATH - Deprecated - Location of the database volume on Cloudbreak host
CB_DEFAULT_SUBSCRIPTION_ADDRESS - Address of the default subscription for Cloudbreak notifications
CB_CAPABILITIES - Comma separated list of enabled capabilities
CB_DISABLE_SHOW_CLI - Disables the 'show cli commond' function
CB_DISABLE_SHOW_BLUEPRINT - Disables the 'show generated blueprint' function
CB_ENABLEDPLATFORMS - Disables Cloudbreak resource called Platform
CB_ENABLED_LINUX_TYPES - List of enabled OS types from image catalog
CBD_FORCE_START - Disables docker-compose.yml and uaa.yml validation
CB_GCP_HOSTKEY_VERIFY - Enables host fingerprint verification on GCP
CB_HBM2DDL_STRATEGY - Configures hibernate.hbm2ddl.auto in Cloudbreak
CB_HOST_DISCOVERY_CUSTOM_DOMAIN - Custom domain of the provisioned cluster
CB_HOST_ADDRESS - Address of the Cloudbreak backend service
CB_IMAGE_CATALOG_URL - Image catalog url
CB_INSTANCE_NODE_ID - Unique identifier of the Cloudbreak node
CB_INSTANCE_PROVIDER - Cloud provider of the Cloudbreak instance
CB_INSTANCE_REGION - Cloud region of the Cloudbreak instance
CB_INSTANCE_UUID - Unique identifier of Cloudbreak deployment
CB_JAVA_OPTS - Extra Java options for Autoscale and Cloudbreak
CB_AUDIT_FILE_ENABLED - Enable audit log file
CB_KAFKA_BOOTSTRAP_SERVERS - Kafka server endpoints for structured audit logs (eg. server1:123,server2:456)
CB_LOG_LEVEL - Log level of the Cloudbreak service
CB_DEFAULT_GATEWAY_CIDR - Cidr for default security rules
CB_MAX_SALT_NEW_SERVICE_RETRY - Salt orchestrator max retry count
CB_MAX_SALT_NEW_SERVICE_RETRY_ONERROR - Salt orchestrator max retry count in case of error
CB_MAX_SALT_RECIPE_EXECUTION_RETRY - Salt orchestrator max retry count for recipes
CB_PLATFORM_DEFAULT_REGIONS - Comma separated list of default regions by platform (AWS:eu-west-1)
CB_PRODUCT_ID - SmartSense product ID
CB_PORT - Cloudbreak port
CB_SCHEMA_MIGRATION_AUTO - Flag for Cloudbreak automatic database schema update
CB_SMARTSENSE_CONFIGURE - Flag to install and configure SmartSense on cluster nodes
CB_SMARTSENSE_CLUSTER_NAME_PREFIX - SmartSense Cloudbreak cluster name prefix
CB_SMARTSENSE_ID - SmartSense subscription ID
CB_TEMPLATE_DEFAULTS - Comma separated list of the default templates what Cloudbreak initialize in database
CB_UI_MAX_WAIT - Wait timeout for `cbd start-wait` command
CERTS_BUCKET - S3 bucket name for backup and restore certificates via `cbd aws [certs-restore-s3  certs-upload-s3]` commands
CERT_VALIDATION - Enables cert validation in Cloudbreak and Autoscale
CLOUDBREAK_SMTP_AUTH - Configures mail.smtp.auth in Cloudbreak
CLOUDBREAK_SMTP_SENDER_FROM - Email address of the sender
CLOUDBREAK_SMTP_SENDER_HOST - SMTP server address ot hostname
CLOUDBREAK_SMTP_SENDER_PASSWORD - Password
CLOUDBREAK_SMTP_SENDER_PORT - Port of the SMTP server
CLOUDBREAK_SMTP_SENDER_USERNAME - User name for SMTP authentication
CLOUDBREAK_SMTP_STARTTLS_ENABLE - Configures mail.smtp.starttls.enable in Cloudbreak
CLOUDBREAK_SMTP_TYPE - Defines mail.transport.protocol in CLoudbreak
COMMON_DB - Name of the database container
COMMON_DB_VOL - Name of the database volume
CURL_CONNECT_TIMEOUT - Timeout for curl command
COMPOSE_HTTP_TIMEOUT - Docker Compose execution timeout
DB_DUMP_VOLUME - Name of the database dump volume
DB_MIGRATION_LOG - Database migration log file
DOCKER_CONSUL_OPTIONS - Extra options for Consul
DOCKER_IMAGE_CBD_SMARTSENSE - SmartSense Docker image name
DOCKER_IMAGE_CLOUDBREAK - Cloudbreak Docker image name
DOCKER_IMAGE_CLOUDBREAK_AUTH - Authentication service Docker image name
DOCKER_IMAGE_CLOUDBREAK_PERISCOPE - Autoscale Docker image name
DOCKER_IMAGE_CLOUDBREAK_WEB - Web UI Docker image name
DOCKER_TAG_ALPINE - Alpine container version
DOCKER_TAG_CBD_SMARTSENSE - SmartSense container version
DOCKER_TAG_CERT_TOOL - Cert tool container version
DOCKER_TAG_CLOUDBREAK - Cloudbreak container version
DOCKER_TAG_CONSUL - Consul container version
DOCKER_TAG_HAVEGED - Haveged container version
DOCKER_TAG_MIGRATION - Migration container version
DOCKER_TAG_PERISCOPE - Autoscale container version
DOCKER_TAG_POSTFIX - Postfix container version
DOCKER_TAG_POSTGRES - Postgresql container version
DOCKER_TAG_LOGROTATE - Logrotate container version
DOCKER_TAG_REGISTRATOR - Registrator container version
DOCKER_TAG_SULTANS - Authentication service container version
DOCKER_TAG_TRAEFIK - Traefik container version
DOCKER_TAG_UAA - Identity container version
DOCKER_TAG_ULUWATU - Web UI container version
DOCKER_STOP_TIMEOUT - Specify a shutdown timeout in seconds for containers
HTTP_PROXY_HOST - HTTP proxy address
HTTPS_PROXY_HOST - HTTPS proxy address
PROXY_PORT - Proxy port
PROXY_USER - Proxy user (basic auth)
PROXY_PASSWORD - Proxy password (basic auth)
NON_PROXY_HOSTS - Indicates the hosts that should be accessed without going through the proxy. Typically this defines internal hosts. The value of this property is a list of hosts, separated by the "|" character. In addition the wildcard character "*" can be used for pattern matching. For example ”*.foo.com|localhost” will indicate that every hosts in the foo.com domain and the localhost should be accessed directly even if a proxy server is specified. Warning: *.consul should be included!
HTTPS_PROXYFORCLUSTERCONNECTION - if set to true, Cloudbreak will use the proxy to connect Ambari server. Default: false
IDENTITY_DB_NAME - Name of the Identity database
IDENTITY_DB_PASS - Password for the Identity database authentication
IDENTITY_DB_URL - Url for the Identity database connection included the port number
IDENTITY_DB_USER - User for the Identity database authentication
LOCAL_SMTP_PASSWORD - Default password for the internal mail server
PERISCOPE_HBM2DDL_STRATEGY - Configures hibernate.hbm2ddl.auto in Autoscale
PERISCOPE_DB_ENV_DB - Name of the Autoscale database
PERISCOPE_DB_ENV_PASS - Password for the Autoscale database authentication
PERISCOPE_DB_ENV_SCHEMA - Used schema in the Autoscale database
PERISCOPE_DB_ENV_USER - User for the Autoscale database authentication
PERISCOPE_DB_PORT_5432_TCP_ADDR - Address of the Autoscale database
PERISCOPE_DB_PORT_5432_TCP_PORT - Port number of the Autoscale database
PERISCOPE_LOG_LEVEL - Log level of the Autoscale service
PERISCOPE_SCHEMA_MIGRATION_AUTO - Flag for Autoscale automatic database schema update
PUBLIC_IP - Ip address or hostname of the public interface
REST_DEBUG - Enables REST call debug level in Cloudbreak and Autoscale
SL_ADDRESS_RESOLVING_TIMEOUT - DNS lookup timeout of Authentication service for internal service discovery
SL_NODE_TLS_REJECT_UNAUTHORIZED - Enables self signed certifications in Authentication service
SULTANS_CONTAINER_PATH - Default project location in Authentication service container
TRAEFIK_MAX_IDLE_CONNECTION - Configures --maxidleconnsperhost for Traefik
PUBLIC_HTTP_PORT - Configures the public http port for Cloudbreak
PUBLIC_HTTPS_PORT - Configures the public https port for Cloudbreak
UAA_CLOUDBREAK_ID - Identity of the Cloudbreak scope in Identity
UAA_CLOUDBREAK_SECRET - Secret of the Cloudbreak scope in Identity
UAA_CLOUDBREAK_SHELL_ID - Identity of the Cloudbreak Shell scope in Identity
UAA_DEFAULT_SECRET - Default secret for all the scopes and encryptions
UAA_DEFAULT_USER_EMAIL - Email address of default admin user
UAA_DEFAULT_USER_FIRSTNAME - First name of default admin user
UAA_DEFAULT_USER_GROUPS - Default user groups of the users
UAA_DEFAULT_USER_LASTNAME - Last name of default admin user
UAA_DEFAULT_USER_PW - Password of default admin user
UAA_SETTINGS_FILE - You can specify custom settings for UAA which will be merged with the default (e.g provide LDAP settings)
UAA_FLEX_USAGE_CLIENT_ID - Identity of the Flex usage generator scope in Identity
UAA_FLEX_USAGE_CLIENT_SECRET - Secret of the Flex usage generator scope in Identity
UAA_PERISCOPE_ID - Identity of the Autoscale scope in Identity
UAA_PERISCOPE_SECRET - Secret of the Autoscale scope in Identity
UAA_PORT - Identity service public port
UAA_SULTANS_ID - Identity of the Authentication service scope in Identity
UAA_SULTANS_SECRET - Secret of the Authentication service scope in Identity
UAA_ULUWATU_ID - Identity of the Web UI scope in Identity
UAA_ULUWATU_SECRET - Secret of the Web UI scope in Identity
UAA_ZONE_DOMAIN - External domain name for zone in Identity
ULUWATU_CONTAINER_PATH - Default project location in Web UI container
ULU_DEFAULT_SSH_KEY - Default SSH key for the credentials in Cloudbreak
ULU_HOST_ADDRESS - Web UI host
ULU_NODE_TLS_REJECT_UNAUTHORIZED - Enables self signed certifications in Web UI
ULU_OAUTH_REDIRECT_URI - Authorization page on Web UI
ULU_SUBSCRIBE_TO_NOTIFICATIONS - Flag for automatic subscriptions for CLoudbreak events
ULU_SULTANS_ADDRESS - Authentication service address
VERBOSE_MIGRATION - Flag of verbose database migration

CB_LOCAL_DEV_BIND_ADDR - Ambassador external address for local development of Cloudbreak and Autoscale
CB_SCHEMA_SCRIPTS_LOCATION - Location of Cloudbreak schema update files
DOCKER_TAG_AMBASSADOR - Ambassador container version for local development
PERISCOPE_SCHEMA_SCRIPTS_LOCATION - Location of Cloudbreak schema update files
PRIVATE_IP - Ip address or hostname of the private interface
REMOVE_CONTAINER - Keeps side effect containers for debug purpose
SULTANS_VOLUME_HOST - Location of the locally developed Authentication service project
UAA_SCHEMA_SCRIPTS_LOCATION - Location of Identity schema update files
ULUWATU_VOLUME_HOST - Location of the locally developed Web UI project

DOCKER_MACHINE - Name of the Docker Machine where Cloudbreak runs
DOCKER_PROFILE - Profile file for Docker Machine related environment variables
MACHINE_CPU - Number of the CPU cores on the Docker Machine instance
MACHINE_MEM - Amount of RAM on the Docker Machine instance
MACHINE_NAME - Name of the Docker Machine instance
MACHINE_OPTS - Extra options for Docker Machine instance
MACHINE_STORAGE_PATH - Docker Machine storage path
''' | grep "$1 " | sed "s/^.* - //" || echo Deprecated
}
