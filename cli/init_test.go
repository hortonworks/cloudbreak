package cli

import "log"

func init() {
	exit = func(code int) {
		log.Printf("system exited with code %d", code)
	}
}
