package cli

import (
	"strconv"
	"strings"
	"sync"
	"testing"

	"github.com/hortonworks/hdc-cli/client_cloudbreak/stacks"
	"github.com/hortonworks/hdc-cli/models_cloudbreak"
)

func TestListClustersImpl(t *testing.T) {
	resps := make([]*models_cloudbreak.StackResponse, 0)
	for i := 0; i <= 3; i++ {
		id := int64(i)
		resps = append(resps, &models_cloudbreak.StackResponse{ID: id})
	}
	getStacks := func(params *stacks.GetPrivatesStackParams) (*stacks.GetPrivatesStackOK, error) {
		return &stacks.GetPrivatesStackOK{Payload: resps}, nil
	}
	mtx := sync.Mutex{}
	expected := make([]ClusterSkeletonResult, 0)
	fetchCluster := func(stack *models_cloudbreak.StackResponse, as *AutoscalingSkeletonResult) (*ClusterSkeletonResult, error) {
		u := strconv.FormatInt(stack.ID, 10)
		skeleton := ClusterSkeletonResult{
			ClusterSkeletonBase: ClusterSkeletonBase{
				ClusterName: "name" + u,
				HDPVersion:  "version" + u,
				ClusterType: "type" + u,
			},
			Status: "status" + u,
		}

		mtx.Lock()
		expected = append(expected, skeleton)
		mtx.Unlock()

		return &skeleton, nil
	}
	var rows []Row

	listClustersImpl(getStacks, fetchCluster, func(h []string, r []Row) { rows = r })

	if len(rows) != len(resps) {
		t.Fatalf("row number not match %d == %d", len(resps), len(rows))
	}

	for i, r := range rows {
		u := strconv.Itoa(i)
		expected := []string{"name" + u, "status" + u, "version" + u, "type" + u}
		if strings.Join(r.DataAsStringArray(), "") != strings.Join(expected, "") {
			t.Errorf("row data not match %s == %s", expected, r.DataAsStringArray())
		}
	}
}

func TestListClusterNodesImplWithoutDiscoveryFQDN(t *testing.T) {
	groups := make([]*models_cloudbreak.InstanceGroupResponse, 0)
	for i := 0; i <= 3; i++ {
		groups = append(groups, &models_cloudbreak.InstanceGroupResponse{Metadata: []*models_cloudbreak.InstanceMetaData{{}}})
	}
	getStack := func(params *stacks.GetPrivateStackParams) (*stacks.GetPrivateStackOK, error) {
		return &stacks.GetPrivateStackOK{Payload: &models_cloudbreak.StackResponse{InstanceGroups: groups}}, nil
	}
	var rows []Row

	listClusterNodesImpl("cluster", getStack, func(h []string, r []Row) { rows = r })

	if len(rows) != 0 {
		t.Errorf("rows not empty %s", rows)
	}
}

func TestListClusterNodesImplWithoutMaster(t *testing.T) {
	var expectedCount int
	groups := make([]*models_cloudbreak.InstanceGroupResponse, 0)
	for i := 0; i <= 3; i++ {
		metas := make([]*models_cloudbreak.InstanceMetaData, 0)
		for j := 0; j <= 3; j++ {
			u := strconv.Itoa(expectedCount)
			expectedCount++
			metas = append(metas, &models_cloudbreak.InstanceMetaData{
				InstanceGroup: "group" + u,
				InstanceID:    "id" + u,
				DiscoveryFQDN: "fqdn" + u,
				PublicIP:      "pubip" + u,
				PrivateIP:     "privip" + u,
			})
		}
		groups = append(groups, &models_cloudbreak.InstanceGroupResponse{Metadata: metas})
	}
	getStack := func(params *stacks.GetPrivateStackParams) (*stacks.GetPrivateStackOK, error) {
		return &stacks.GetPrivateStackOK{Payload: &models_cloudbreak.StackResponse{InstanceGroups: groups}}, nil
	}
	var rows []Row

	listClusterNodesImpl("cluster", getStack, func(h []string, r []Row) { rows = r })

	if len(rows) != expectedCount {
		t.Fatalf("row number not match %d == %d", expectedCount, len(rows))
	}

	for i, r := range rows {
		u := strconv.Itoa(i)
		expected := []string{"id" + u, "fqdn" + u, "pubip" + u, "privip" + u, "group" + u}
		if strings.Join(r.DataAsStringArray(), "") != strings.Join(expected, "") {
			t.Errorf("row data not match %s == %s", expected, r.DataAsStringArray())
		}
	}
}

func TestListClusterNodesImplWithMaster(t *testing.T) {
	groups := []*models_cloudbreak.InstanceGroupResponse{{
		Metadata: []*models_cloudbreak.InstanceMetaData{{
			InstanceGroup: MASTER,
			DiscoveryFQDN: "fqdn",
		}},
	}}
	getStack := func(params *stacks.GetPrivateStackParams) (*stacks.GetPrivateStackOK, error) {
		return &stacks.GetPrivateStackOK{Payload: &models_cloudbreak.StackResponse{InstanceGroups: groups}}, nil
	}
	var rows []Row

	listClusterNodesImpl("cluster", getStack, func(h []string, r []Row) { rows = r })

	for _, r := range rows {
		expected := []string{"", "fqdn", "", "", "master - ambari server"}
		if strings.Join(r.DataAsStringArray(), "") != strings.Join(expected, "") {
			t.Errorf("row data not match %s == %s", expected, r.DataAsStringArray())
		}
	}
}
