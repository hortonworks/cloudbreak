package cli

import (
	"errors"

	"fmt"
	swagerrors "github.com/go-swagger/go-swagger/errors"
	"github.com/go-swagger/go-swagger/httpkit/validate"
	"strconv"
)

func (s *ClusterSkeleton) Validate() error {
	var res []error
	if err := validate.RequiredString("ClusterName", "body", string(s.ClusterName)); err != nil {
		res = append(res, err)
	}
	if err := validate.RequiredString("HDPVersion", "body", string(s.HDPVersion)); err != nil {
		res = append(res, err)
	}
	if err := validateHDPVersion(string(s.HDPVersion)); err != nil {
		res = append(res, err)
	}
	if err := validate.RequiredString("ClusterType", "body", string(s.ClusterType)); err != nil {
		res = append(res, err)
	}
	if err := validate.RequiredNumber("InstanceCount", "worker", float64(s.Worker.InstanceCount)); err != nil {
		res = append(res, err)
	} else if s.Worker.InstanceCount < 1 {
		res = append(res, swagerrors.New(1, "The instance count has to be greater than 0"))
	}
	if s.Compute.InstanceCount >= 0 {
		res = append(res)
	} else {
		res = append(res, swagerrors.New(1, "The instance count has to be not less than 0"))
	}
	if s.Compute.SpotPrice != "" {
		if f, err := strconv.ParseFloat(s.Compute.SpotPrice, 64); err != nil || f <= 0 {
			res = append(res, swagerrors.New(1, "SpotPrice must be numeric and greater than 0"))
		}
	}
	if err := validate.RequiredString("SSHKeyName", "body", string(s.SSHKeyName)); err != nil {
		res = append(res, err)
	}
	if err := validate.RequiredString("RemoteAccess", "body", string(s.RemoteAccess)); err != nil {
		res = append(res, err)
	}
	if err := validate.Required("WebAccess", "body", s.WebAccess); err != nil {
		res = append(res, err)
	}
	if err := validate.RequiredString("ClusterAndAmbariUser", "body", string(s.ClusterAndAmbariUser)); err != nil {
		res = append(res, err)
	}
	if err := validate.RequiredString("ClusterAndAmbariPassword", "body", string(s.ClusterAndAmbariPassword)); err != nil {
		res = append(res, err)
	}
	if s.Network != nil {
		if err := s.Network.Validate(); err != nil {
			for _, e := range err {
				res = append(res, e)
			}
		}
	}
	if s.HiveMetastore != nil {
		if err := s.HiveMetastore.Validate(); err != nil {
			for _, e := range err {
				res = append(res, e)
			}
		}
	}

	if len(s.Master.Recipes) != 0 {
		if e := validateRecipes(s.Master.Recipes); len(e) != 0 {
			res = append(res, e...)
		}
	}

	if len(s.Worker.Recipes) != 0 {
		if e := validateRecipes(s.Worker.Recipes); len(e) != 0 {
			res = append(res, e...)
		}
	}

	if len(s.Compute.Recipes) != 0 {
		if e := validateRecipes(s.Compute.Recipes); len(e) != 0 {
			res = append(res, e...)
		}
	}

	if len(res) > 0 {
		return swagerrors.CompositeValidationError(res...)
	}
	return nil
}

func (n *Network) Validate() []error {
	var res []error = nil

	if !n.isEmpty() {
		if err := validate.RequiredString("VpcId", "network", n.VpcId); err != nil {
			res = append(res, err)
		}
		if err := validate.RequiredString("SubnetId", "network", n.SubnetId); err != nil {
			res = append(res, err)
		}
	}

	return res
}

func (n *Network) isEmpty() bool {
	return len(n.VpcId) == 0 && len(n.SubnetId) == 0
}

func (h *HiveMetastore) Validate() []error {
	var res []error = nil

	if h.isNew() {
		if err := validate.RequiredString("Name", "hivemetastore", h.Name); err != nil {
			res = append(res, err)
		}
		if err := validate.RequiredString("DatabaseType", "hivemetastore", h.DatabaseType); err != nil {
			res = append(res, err)
		} else if h.DatabaseType != POSTGRES {
			res = append(res, errors.New("Invalid database type. Accepted value is: POSTGRES"))
		}
		if err := validate.RequiredString("Password", "hivemetastore", h.Password); err != nil {
			res = append(res, err)
		}
		if err := validate.RequiredString("Username", "hivemetastore", h.Username); err != nil {
			res = append(res, err)
		}
		if err := validate.RequiredString("URL", "hivemetastore", h.URL); err != nil {
			res = append(res, err)
		}
	}

	return res
}

func (r *Recipe) Validate() []error {
	var res []error = nil

	if err := validate.RequiredString("URI", "recipe", r.URI); err != nil {
		res = append(res, err)
	}
	if err := validate.RequiredString("Phase", "recipe", r.Phase); err != nil {
		res = append(res, err)
	}

	if r.Phase != PRE && r.Phase != POST {
		res = append(res, errors.New(fmt.Sprintf("Valid recipe phases: %s, %s", PRE, POST)))
	}

	return res
}

func validateRecipes(recipes []Recipe) []error {
	var res []error = make([]error, 0)
	for _, recipe := range recipes {
		for _, e := range recipe.Validate() {
			res = append(res, e)
		}
	}
	return res
}

func (h *HiveMetastore) isNew() bool {
	return len(h.DatabaseType) > 0 || len(h.Username) > 0 || len(h.Password) > 0 || len(h.URL) > 0
}

func validateHDPVersion(version string) error {
	if hdp, err := strconv.ParseFloat(version, 10); err != nil || !isVersionSupported(hdp) {
		return errors.New(fmt.Sprintf("Invalid HDP version. Accepted value(s): %v", SUPPORTED_HDP_VERSIONS))
	}
	return nil
}

func isVersionSupported(version float64) bool {
	for _, v := range SUPPORTED_HDP_VERSIONS {
		if v == version {
			return true
		}
	}
	return false
}
