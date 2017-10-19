package aws

// import (
// 	"strconv"
// 	"strings"
// 	"testing"
//
// 	"github.com/hortonworks/cb-cli/cli/cloud"
// )
//
// var cloudProvider cloud.CloudProvider = new(AwsProvider)
//
// func TestNetworkSkeletonValidateVpcMissing(t *testing.T) {
// 	skeleton := cloud.Network{
// 		SubnetId: "subnet",
// 	}
//
// 	errors := cloudProvider.ValidateNetwork(&skeleton)
//
// 	if errors == nil {
// 		t.Error("errors couldn't be nil")
// 	} else if len(errors) != 1 {
// 		s := convertErrorsToString(errors)
// 		t.Errorf("not only VpcId not valid: %s", strings.Join(s, ", "))
// 	} else if errors[0].Error() != "VpcId in network is required" {
// 		t.Error("missing VpcId in network is required")
// 	}
// }
//
// func TestNetworkSkeletonValidateSubnetMissing(t *testing.T) {
// 	skeleton := cloud.Network{
// 		VpcId: "vpc",
// 	}
//
// 	errors := cloudProvider.ValidateNetwork(&skeleton)
//
// 	if errors == nil {
// 		t.Error("errors couldn't be nil")
// 	} else if len(errors) != 1 {
// 		s := convertErrorsToString(errors)
// 		t.Errorf("not only SubnetId not valid: %s", strings.Join(s, ", "))
// 	} else if errors[0].Error() != "SubnetId in network is required" {
// 		t.Error("missing SubnetId in network is required")
// 	}
// }
//
// func TestNetworkSkeletonValidateAllGood(t *testing.T) {
// 	skeleton := cloud.Network{
// 		VpcId:    "vpc",
// 		SubnetId: "subnet",
// 	}
//
// 	errors := cloudProvider.ValidateNetwork(&skeleton)
//
// 	if errors != nil {
// 		t.Errorf("validation went fail: %s", strings.Join(convertErrorsToString(errors), ", "))
// 	}
// }
//
// func TestTagsForReservedWordKey(t *testing.T) {
// 	tags := map[string]string{"aws-test1": "test"}
//
// 	errors := cloudProvider.ValidateTags(tags)
//
// 	if len(errors) != 1 {
// 		t.Errorf("Accepted invalid tags: %v", tags)
// 	}
// }
//
// func TestTagsForReservedWordValue(t *testing.T) {
// 	tags := map[string]string{"test1": "aws:test"}
//
// 	errors := cloudProvider.ValidateTags(tags)
//
// 	if len(errors) != 1 {
// 		t.Errorf("Accepted invalid tags: %v", tags)
// 	}
// }
//
// func TestTooMuchTags(t *testing.T) {
// 	var tags = make(map[string]string, 0)
// 	for i := 0; i < 11; i++ {
// 		tags["tag"+strconv.Itoa(i)] = "value"
// 	}
//
// 	errors := cloudProvider.ValidateTags(tags)
//
// 	if len(errors) != 1 {
// 		t.Errorf("Accepted invalid tags: %v", tags)
// 	}
// }
//
// func TestTooLongTagKey(t *testing.T) {
// 	var testKey string
// 	for i := 0; i < 128; i++ {
// 		testKey += "a"
// 	}
// 	var tags = make(map[string]string, 0)
// 	tags[testKey] = "value"
//
// 	errors := cloudProvider.ValidateTags(tags)
//
// 	if len(errors) != 1 {
// 		t.Errorf("Accepted invalid tags: %v", tags)
// 	}
// }
//
// func TestTooLongTagValue(t *testing.T) {
// 	var testValue string
// 	for i := 0; i < 256; i++ {
// 		testValue += "a"
// 	}
// 	var tags = make(map[string]string, 0)
// 	tags["test"] = testValue
//
// 	errors := cloudProvider.ValidateTags(tags)
//
// 	if len(errors) != 1 {
// 		t.Errorf("Accepted invalid tags: %v", tags)
// 	}
// }
//
// func TestTagsForInvalidKeyChar(t *testing.T) {
// 	tags := map[string]string{"test1,": "test"}
//
// 	errors := cloudProvider.ValidateTags(tags)
//
// 	if len(errors) != 1 {
// 		t.Errorf("Accepted invalid tags: %v", tags)
// 	}
// }
//
// func TestTagsForInvalidValueChar(t *testing.T) {
// 	tags := map[string]string{"test1": "test;"}
//
// 	errors := cloudProvider.ValidateTags(tags)
//
// 	if len(errors) != 1 {
// 		t.Errorf("Accepted invalid tags: %v", tags)
// 	}
// }
//
// func TestValidTag(t *testing.T) {
// 	tags := map[string]string{"TeSt123:-_/ :.=+ -=._:/": "ValuE0123:-_/ :.=+ -=._:/"}
//
// 	errors := cloudProvider.ValidateTags(tags)
//
// 	if len(errors) != 0 {
// 		t.Errorf("Valid tag is not accepted: %v", tags)
// 	}
// }
//
// func TestTagsForMultipleErrors(t *testing.T) {
// 	tags := map[string]string{"aws=test1": "test;"}
// 	var testValue string
// 	for i := 0; i < 256; i++ {
// 		testValue += "a"
// 	}
// 	tags["test"] = testValue
// 	var testKey string
// 	for i := 0; i < 128; i++ {
// 		testKey += "a"
// 	}
// 	tags[testKey] = "value"
// 	for i := 0; i < 11; i++ {
// 		tags["tag"+strconv.Itoa(i)] = "value"
// 	}
//
// 	errors := cloudProvider.ValidateTags(tags)
//
// 	if len(errors) != 5 {
// 		t.Errorf("Accepted invalid tags: %v", tags)
// 	}
// }
//
// func convertErrorsToString(errors []error) []string {
// 	var s []string
// 	for _, e := range errors {
// 		s = append(s, e.Error())
// 	}
// 	return s
// }
