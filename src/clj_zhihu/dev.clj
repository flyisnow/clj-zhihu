(ns clj-zhihu.dev
  (:require (clj-zhihu [core :as core]
                       [auth :as auth])
            [clj-http.client :as client] :reload-all))

(auth/with-zhihu-account ["xlian2@cs.rochester.edu" "clj-zhihu-lxr"]
  (client/with-connection-pool {:timeout 5 :threads 4 :insecure? false :default-per-route 10}
    (let [followers (-> (core/user "you-jiang-55")
                        :followers)]
      (first followers))))
