package cli

import (
	"strconv"
	"strings"
	"testing"
)

func TestClusterSkeletonValidateAllMissing(t *testing.T) {
	skeleton := ClusterSkeleton{}

	errors := skeleton.Validate()

	if errors == nil {
		t.Error("errors couldn't be nil")
	} else if c := strings.Count(errors.Error(), "required"); c != 8 {
		t.Errorf("required fields 8 != %d : %s", c, errors.Error())
	}
}

func TestClusterSkeletonValidateInstanceCountIsZero(t *testing.T) {
	skeleton := ClusterSkeleton{ClusterSkeletonBase: ClusterSkeletonBase{Worker: InstanceConfig{InstanceCount: -1}}}

	errors := skeleton.Validate()

	if errors == nil {
		t.Error("errors couldn't be nil")
	} else if strings.Index(errors.Error(), "The instance count has to be greater than 0") <= 0 {
		t.Errorf("missing instance count validation: %s", errors.Error())
	}
}

func TestClusterSkeletonValidateAllGood(t *testing.T) {
	skeleton := ClusterSkeleton{
		ClusterSkeletonBase: ClusterSkeletonBase{
			ClusterName:              "name",
			HDPVersion:               "2.5",
			ClusterType:              "type",
			Master:                   InstanceConfig{InstanceCount: 1},
			Worker:                   InstanceConfig{InstanceCount: 1, RecoveryMode: "AUTO"},
			Compute:                  SpotInstanceConfig{InstanceConfig: InstanceConfig{InstanceCount: 1, RecoveryMode: "MANUAL"}},
			SSHKeyName:               "ssh",
			RemoteAccess:             "remote",
			WebAccess:                true,
			ClusterAndAmbariUser:     "user",
			ClusterAndAmbariPassword: "pass",
		},
	}

	errors := skeleton.Validate()

	if errors != nil {
		t.Errorf("validation went fail: %s", errors.Error())
	}
}

func TestNetworkSkeletonValidateVpcMissing(t *testing.T) {
	skeleton := Network{
		SubnetId: "subnet",
	}

	errors := skeleton.Validate()

	if errors == nil {
		t.Error("errors couldn't be nil")
	} else if len(errors) != 1 {
		s := convertErrorsToString(errors)
		t.Errorf("not only VpcId not valid: %s", strings.Join(s, ", "))
	} else if errors[0].Error() != "VpcId in network is required" {
		t.Error("missing VpcId in network is required")
	}
}

func TestNetworkSkeletonValidateSubnetMissing(t *testing.T) {
	skeleton := Network{
		VpcId: "vpc",
	}

	errors := skeleton.Validate()

	if errors == nil {
		t.Error("errors couldn't be nil")
	} else if len(errors) != 1 {
		s := convertErrorsToString(errors)
		t.Errorf("not only SubnetId not valid: %s", strings.Join(s, ", "))
	} else if errors[0].Error() != "SubnetId in network is required" {
		t.Error("missing SubnetId in network is required")
	}
}

func TestNetworkSkeletonValidateAllGood(t *testing.T) {
	skeleton := Network{
		VpcId:    "vpc",
		SubnetId: "subnet",
	}

	errors := skeleton.Validate()

	if errors != nil {
		t.Errorf("validation went fail: %s", strings.Join(convertErrorsToString(errors), ", "))
	}
}

func TestHiveMetastoreSkeletonValidateOnlyUrl(t *testing.T) {
	skeleton := HiveMetastore{
		MetaStore: MetaStore{
			URL: "url",
		},
	}

	errors := skeleton.Validate()

	if errors == nil {
		t.Error("errors couldn't be nil")
	} else if len(errors) != 4 {
		s := convertErrorsToString(errors)
		t.Errorf("required fields 4 != %d : %s", len(errors), strings.Join(s, ", "))
	}
}

func TestHiveMetastoreSkeletonValidateOnlyUsername(t *testing.T) {
	skeleton := HiveMetastore{
		MetaStore: MetaStore{
			Username: "user",
		},
	}

	errors := skeleton.Validate()

	if errors == nil {
		t.Error("errors couldn't be nil")
	} else if len(errors) != 4 {
		s := convertErrorsToString(errors)
		t.Errorf("required fields 4 != %d : %s", len(errors), strings.Join(s, ", "))
	}
}

func TestHiveMetastoreSkeletonValidateWrongDatabase(t *testing.T) {
	skeleton := HiveMetastore{
		MetaStore: MetaStore{
			DatabaseType: "db",
		},
	}

	errors := skeleton.Validate()

	if errors == nil {
		t.Error("errors couldn't be nil")
	} else if s := convertErrorsToString(errors); strings.Index(strings.Join(s, ""), "Invalid database type. Accepted value is: POSTGRES") < 0 {
		t.Error("only POSTGRES is approoved")
	}
}

func TestHiveMetastoreSkeletonValidateAllGoodNew(t *testing.T) {
	skeleton := HiveMetastore{
		MetaStore: MetaStore{
			Name:         "name",
			DatabaseType: "POSTGRES",
			Username:     "user",
			Password:     "pass",
			URL:          "url",
		},
	}

	errors := skeleton.Validate()

	if errors != nil {
		t.Errorf("validation went fail: %s", strings.Join(convertErrorsToString(errors), ", "))
	}
}

func TestHiveMetastoreSkeletonValidateAllGoodExisting(t *testing.T) {
	skeleton := HiveMetastore{MetaStore: MetaStore{Name: "name"}}

	errors := skeleton.Validate()

	if errors != nil {
		t.Errorf("validation went fail: %s", strings.Join(convertErrorsToString(errors), ", "))
	}
}

func TestInvalidHDPVersion(t *testing.T) {
	skeleton := ClusterSkeleton{
		ClusterSkeletonBase: ClusterSkeletonBase{
			ClusterName:              "name",
			HDPVersion:               "2.4",
			ClusterType:              "type",
			Worker:                   InstanceConfig{InstanceCount: 1},
			SSHKeyName:               "ssh",
			RemoteAccess:             "remote",
			WebAccess:                true,
			ClusterAndAmbariUser:     "user",
			ClusterAndAmbariPassword: "pass",
		},
	}

	errors := skeleton.Validate()

	if errors == nil {
		t.Error("validation should fail for HDP version")
	}
}

func TestInvalidHDPVersionSimpleNumber(t *testing.T) {
	skeleton := ClusterSkeleton{
		ClusterSkeletonBase: ClusterSkeletonBase{
			ClusterName:              "name",
			HDPVersion:               "2",
			ClusterType:              "type",
			Worker:                   InstanceConfig{InstanceCount: 1},
			SSHKeyName:               "ssh",
			RemoteAccess:             "remote",
			WebAccess:                true,
			ClusterAndAmbariUser:     "user",
			ClusterAndAmbariPassword: "pass",
		},
	}

	errors := skeleton.Validate()

	if errors == nil {
		t.Error("validation should fail for HDP version")
	}
}

func TestInvalidHDPVersionForNonNumber(t *testing.T) {
	skeleton := ClusterSkeleton{
		ClusterSkeletonBase: ClusterSkeletonBase{
			ClusterName:              "name",
			HDPVersion:               "something",
			ClusterType:              "type",
			Worker:                   InstanceConfig{InstanceCount: 1},
			SSHKeyName:               "ssh",
			RemoteAccess:             "remote",
			WebAccess:                true,
			ClusterAndAmbariUser:     "user",
			ClusterAndAmbariPassword: "pass",
		},
	}

	errors := skeleton.Validate()

	if errors == nil {
		t.Error("validation should fail for HDP version")
	}
}

func TestTagsForReservedWordKey(t *testing.T) {
	tags := map[string]string{"aws-test1": "test"}

	errors := validateTags(tags)

	if len(errors) != 1 {
		t.Errorf("Accepted invalid tags: %v", tags)
	}
}

func TestTagsForReservedWordValue(t *testing.T) {
	tags := map[string]string{"test1": "aws:test"}

	errors := validateTags(tags)

	if len(errors) != 1 {
		t.Errorf("Accepted invalid tags: %v", tags)
	}
}

func TestTooMuchTags(t *testing.T) {
	var tags = make(map[string]string, 0)
	for i := 0; i < 11; i++ {
		tags["tag"+strconv.Itoa(i)] = "value"
	}

	errors := validateTags(tags)

	if len(errors) != 1 {
		t.Errorf("Accepted invalid tags: %v", tags)
	}
}

func TestTooLongTagKey(t *testing.T) {
	var testKey string
	for i := 0; i < 128; i++ {
		testKey += "a"
	}
	var tags = make(map[string]string, 0)
	tags[testKey] = "value"

	errors := validateTags(tags)

	if len(errors) != 1 {
		t.Errorf("Accepted invalid tags: %v", tags)
	}
}

func TestTooLongTagValue(t *testing.T) {
	var testValue string
	for i := 0; i < 256; i++ {
		testValue += "a"
	}
	var tags = make(map[string]string, 0)
	tags["test"] = testValue

	errors := validateTags(tags)

	if len(errors) != 1 {
		t.Errorf("Accepted invalid tags: %v", tags)
	}
}

func TestTagsForInvalidKeyChar(t *testing.T) {
	tags := map[string]string{"test1,": "test"}

	errors := validateTags(tags)

	if len(errors) != 1 {
		t.Errorf("Accepted invalid tags: %v", tags)
	}
}

func TestTagsForInvalidValueChar(t *testing.T) {
	tags := map[string]string{"test1": "test;"}

	errors := validateTags(tags)

	if len(errors) != 1 {
		t.Errorf("Accepted invalid tags: %v", tags)
	}
}

func TestValidTag(t *testing.T) {
	tags := map[string]string{"TeSt123:-_/ :.=+ -=._:/": "ValuE0123:-_/ :.=+ -=._:/"}

	errors := validateTags(tags)

	if len(errors) != 0 {
		t.Errorf("Valid tag is not accepted: %v", tags)
	}
}

func TestTagsForMultipleErrors(t *testing.T) {
	tags := map[string]string{"aws=test1": "test;"}
	var testValue string
	for i := 0; i < 256; i++ {
		testValue += "a"
	}
	tags["test"] = testValue
	var testKey string
	for i := 0; i < 128; i++ {
		testKey += "a"
	}
	tags[testKey] = "value"
	for i := 0; i < 11; i++ {
		tags["tag"+strconv.Itoa(i)] = "value"
	}

	errors := validateTags(tags)

	if len(errors) != 5 {
		t.Errorf("Accepted invalid tags: %v", tags)
	}
}

func convertErrorsToString(errors []error) []string {
	var s []string
	for _, e := range errors {
		s = append(s, e.Error())
	}
	return s
}
