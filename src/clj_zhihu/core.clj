(ns clj-zhihu.core
  (:require [clj-http.client :as client]
            [clojure.java.io :as io]
            [clojure.data.json :as json]
            [net.cgrand.enlive-html :as html])
  (:use [clj-zhihu.utils]))

(defn get-followee-num
  "get the number of followees"
  [id]
  (let [userurl (str "http://www.zhihu.com/people/" id "/followers")
        userpage (-> userurl
                     client/get
                     :body)]
    (-> (java.io.StringReader.  userpage)
        html/html-resource
        (html/select  [:div.zm-profile-side-following.zg-clear])
        (html/select [:strong])
        first
        :content
        first
        Integer/parseInt)))

(defn get-followees
  "get a set of followees' id given the id of a user"
  [id]
  (let [url      "http://www.zhihu.com/node/ProfileFolloweesListV2"
        userurl  (str "http://www.zhihu.com/people/" id "/followers")
        userpage (-> userurl
                     client/get
                     :body)]
    (reduce #(into %1
                   (->> (client/post url
                                     {:form-params
                                      {:method "next"
                                       :params (json/write-str
                                                {:offset (* 20 %2)
                                                 :order_by "created"
                                                 :hash_id (get-hashid userpage)})
                                       :_xsrf (get-xsrf userpage)}})
                        :body
                        (re-seq #"<h2.*?\\\/people\\\/(.*?)\\\" class=\\\"zg-link\\\"")
                        (map second)))
            #{}
            (range 0 (Math/ceil (/ (get-followee-num id) 20))))))

(defn get-follower-num
  "given user id return the number of followers"
  [id]
  (let [userurl (str "http://www.zhihu.com/people/" id "/followers")
        userpage (-> userurl
                     client/get
                     :body)]
    (-> (java.io.StringReader.  userpage)
        html/html-resource
        (html/select  [:div.zm-profile-side-following.zg-clear])
        (html/select [:strong])
        second
        :content
        first
        Integer/parseInt)))

(defn get-followers
  "given user id return a set of user id of the followers"
  [id]
  (let [url      "http://www.zhihu.com/node/ProfileFollowersListV2"
        userurl  (str "http://www.zhihu.com/people/" id "/followers")
        userpage (-> userurl
                     client/get
                     :body)]
    (reduce #(into %1
                   (->> (client/post url
                                     {:form-params
                                      {:method "next"
                                       :params (json/write-str
                                                {:offset (* 20 %2)
                                                 :order_by "created"
                                                 :hash_id (get-hashid userpage)})
                                       :_xsrf (get-xsrf userpage)}})
                        :body
                        (re-seq #"<h2.*?\\\/people\\\/(.*?)\\\" class=\\\"zg-link\\\"")
                        (map second)))
            #{}
            (range 0 (Math/ceil (/ (get-follower-num id) 20))))))

(defn get-profile
  "given user id return a map of user's profile"
  [id]
  (let [userurl (str "http://www.zhihu.com/people/" id)
        userpage (-> userurl
                     client/get
                     :body)]
    {:name nil
     :mood nil
     :icon nil
     :location nil
     :field nil
     :gender nil
     :company nil
     :position nil
     :school nil
     :major nil
     :description nil
     :upvote nil
     :thank nil
     :answer# nil
     :question# nil
     :favoriate# nil
     }))
