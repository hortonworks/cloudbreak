// +build darwin dragonfly freebsd linux netbsd openbsd solaris

package cloudbreak

import "os"

func clearenv() {
	os.Clearenv()
}
