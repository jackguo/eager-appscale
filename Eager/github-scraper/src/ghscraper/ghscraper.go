package main

import (
	"fmt"
	"encoding/json"
	"net/http"
	"io/ioutil"
	"os/exec"
)

func main() {
	url := "https://api.github.com/search/repositories?q=google+appengine+language:java&sort=stars&order=desc&per_page=100&page="
	page := 1
	total := -1
	received := -2
	for received < total {
		fmt.Println("Contacting api.github.com...")
		resp, err := http.Get(fmt.Sprintf("%s%d", url, page))
		if err != nil {
			fmt.Println(err)
			return
		}

		defer resp.Body.Close()
		body, err := ioutil.ReadAll(resp.Body)
		if err != nil {
			fmt.Println(err)
			return
		}

		var f interface{}
		err = json.Unmarshal(body, &f)
		if err != nil {
			fmt.Println(err)
			return
		}
		m := f.(map[string]interface{})
		if total < 0 {
			total = int(m["total_count"].(float64))
			if total == 0 {
				fmt.Println("Search did not return any hits. Exiting...")
				return
			} else {
				fmt.Println("Total count is", total)
				received = 0
			}
		}
		items := m["items"].([]interface{})
		for _,v := range(items) {
			received++
			item := v.(map[string]interface{})
			fmt.Println(received, "Cloning the repository:", item["full_name"])
			_, err := exec.Command("git", "clone", "https://github.com/" + string(item["full_name"].(string))).Output()
			if err != nil {
				fmt.Println("Failed to clone the repo", err)
			}
		}
		page++
	}
}
