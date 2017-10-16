package cli

import (
	"io/ioutil"
	"testing"
)

func TestGetNodesByBlueprint(t *testing.T) {
	bp, _ := ioutil.ReadFile("testdata/blueprint.json")

	nodes := getNodesByBlueprint(bp)

	expectedNodes := []node{
		node{"master", "GATEWAY", 1},
		node{"slave_1", "CORE", 9},
		node{"slave_2", "CORE", 3},
		node{"slave_3", "CORE", 1},
	}
	for i, n := range expectedNodes {
		node := nodes[i]
		if node.name != n.name {
			t.Errorf("node[%d] name not match %s == %s", i, n.name, node.name)
		} else if node.groupType != n.groupType {
			t.Errorf("node[%d] group type not match %s == %s", i, n.groupType, node.groupType)
		} else if node.count != n.count {
			t.Errorf("node[%d] count not match %d == %d", i, n.count, node.count)
		}
	}
}
