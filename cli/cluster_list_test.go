package cli

import (
	"strconv"
	"strings"
	"sync"
	"testing"

	"github.com/hortonworks/hdc-cli/client/stacks"
	"github.com/hortonworks/hdc-cli/models"
)

func TestListClustersImpl(t *testing.T) {
	resps := make([]*models.StackResponse, 0)
	for i := 0; i <= 3; i++ {
		id := int64(i)
		resps = append(resps, &models.StackResponse{ID: &id})
	}
	getStacks := func(params *stacks.GetStacksUserParams) (*stacks.GetStacksUserOK, error) {
		return &stacks.GetStacksUserOK{Payload: resps}, nil
	}
	mtx := sync.Mutex{}
	expected := make([]ClusterSkeletonResult, 0)
	fetchCluster := func(stack *models.StackResponse) (*ClusterSkeletonResult, error) {
		u := strconv.FormatInt(*stack.ID, 10)
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
	groups := make([]*models.InstanceGroupResponse, 0)
	for i := 0; i <= 3; i++ {
		groups = append(groups, &models.InstanceGroupResponse{Metadata: []*models.InstanceMetaData{{}}})
	}
	getStack := func(params *stacks.GetStacksUserNameParams) (*stacks.GetStacksUserNameOK, error) {
		return &stacks.GetStacksUserNameOK{Payload: &models.StackResponse{InstanceGroups: groups}}, nil
	}
	var rows []Row

	listClusterNodesImpl("cluster", getStack, func(h []string, r []Row) { rows = r })

	if len(rows) != 0 {
		t.Errorf("rows not empty %s", rows)
	}
}

func TestListClusterNodesImplWithoutMaster(t *testing.T) {
	var expectedCount int
	groups := make([]*models.InstanceGroupResponse, 0)
	for i := 0; i <= 3; i++ {
		metas := make([]*models.InstanceMetaData, 0)
		for j := 0; j <= 3; j++ {
			u := strconv.Itoa(expectedCount)
			expectedCount++
			metas = append(metas, &models.InstanceMetaData{
				InstanceGroup: &(&stringWrapper{"group" + u}).s,
				InstanceID:    &(&stringWrapper{"id" + u}).s,
				DiscoveryFQDN: &(&stringWrapper{"fqdn" + u}).s,
				PublicIP:      &(&stringWrapper{"pubip" + u}).s,
				PrivateIP:     &(&stringWrapper{"privip" + u}).s,
			})
		}
		groups = append(groups, &models.InstanceGroupResponse{Metadata: metas})
	}
	getStack := func(params *stacks.GetStacksUserNameParams) (*stacks.GetStacksUserNameOK, error) {
		return &stacks.GetStacksUserNameOK{Payload: &models.StackResponse{InstanceGroups: groups}}, nil
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
	groups := []*models.InstanceGroupResponse{{
		Metadata: []*models.InstanceMetaData{{
			InstanceGroup: &(&stringWrapper{MASTER}).s,
			DiscoveryFQDN: &(&stringWrapper{"fqdn"}).s,
		}},
	}}
	getStack := func(params *stacks.GetStacksUserNameParams) (*stacks.GetStacksUserNameOK, error) {
		return &stacks.GetStacksUserNameOK{Payload: &models.StackResponse{InstanceGroups: groups}}, nil
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
