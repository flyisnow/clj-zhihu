(ns clj-zhihu.core
  (:require [clj-http.client :as client]
            [clojure.java.io :as io]
            [clojure.data.json :as json]
            [net.cgrand.enlive-html :as html])
  (:use [clj-zhihu.utils]))

(defn ^:private get-followees
  "get a set of followees' id given the id of a user"
  [id followee#]
  (let [url      "http://www.zhihu.com/node/ProfileFolloweesListV2"
        userurl  (str "http://www.zhihu.com/people/" id "/followers")
        userpage (-> userurl client/get :body)]
    (reduce #(into %1
                   (->> (client/post
                         url
                         {:form-params
                          {:method "next"
                           :params (json/write-str
                                    {:offset (* 20 %2)
                                     :order_by "created"
                                     :hash_id (get-hashid userpage)})
                           :_xsrf (get-xsrf userpage)}})
                        :body
                        (re-seq (:homepageid regex-map))
                        (map second)))
            #{}
            (range 0 (Math/ceil (/ followee# 20))))))

(defn ^:private get-followers
  "given user id return a set of user id of the followers"
  [id follower#]
  (let [url      "http://www.zhihu.com/node/ProfileFollowersListV2"
        userurl  (str "http://www.zhihu.com/people/" id "/followers")
        userpage (-> userurl client/get :body)]
    (reduce #(into %1
                   (->> (client/post
                         url
                         {:form-params
                          {:method "next"
                           :params (json/write-str
                                    {:offset (* 20 %2)
                                     :order_by "created"
                                     :hash_id (get-hashid userpage)})
                           :_xsrf (get-xsrf userpage)}})
                        :body
                        (re-seq (:homepageid regex-map))
                        (map second)))
            #{}
            (range 0 (Math/ceil (/ follower# 20))))))

(defn user
  "given user id return a user map"
  [id]
  (let [userurl (str "http://www.zhihu.com/people/" id)
        userpage (-> userurl
                     client/get
                     :body)
        enlive-userpage (html/html-resource (java.io.StringReader. userpage))
        enlive-navbarnode (html/select enlive-userpage
                                       [:div.profile-navbar.clearfix])
        navbar-numbers (map (comp #(Integer/parseInt %) first :content)
                            (html/select enlive-navbarnode [:span.num]))
        followee# (-> (html/select enlive-userpage
                                   [:div.zm-profile-side-following.zg-clear])
                      (html/select [:strong])
                      first :content first Integer/parseInt)
        follower# (-> (html/select enlive-userpage [:div.zm-profile-side-following.zg-clear])
                      (html/select [:strong])
                      second :content first Integer/parseInt)]
    {:name (-> (html/select enlive-userpage [:span.name])
               last :content first)
     :mood (-> (html/select enlive-userpage [:span.bio])
               first :content first)
     :icon (-> (html/select enlive-userpage [:img.avatar.avatar-l])
               last :attrs :src)
     :location (-> (html/select enlive-userpage [:span.location.item])
                   last :attrs :title)
     :business (-> (html/select enlive-userpage [:span.business.item])
                   last :attrs :title)
     :gender ((fn [class-name]
                (if (.contains class-name "male")
                  :male
                  (when (.contains class-name "female")
                    :female)))
              (-> (html/select enlive-userpage [:span.item.gender])
                  first :content first :attrs :class))
     :employment (-> (html/select enlive-userpage [:span.employment.item])
                     last :attrs :title)
     :position (-> (html/select enlive-userpage [:span.position.item])
                   last :attrs :title)
     :education (-> (html/select enlive-userpage [:span.education.item])
                    last :attrs :title)
     :major (-> (html/select enlive-userpage [:span.education-extra.item])
                last :attrs :title)
     :description (-> (html/select enlive-userpage
                                   [:span.description.unfold-item])
                      last
                      (html/select [:span.content])
                      last :content first clojure.string/trim)
     :upvote# (-> (html/select enlive-userpage
                               [:span.zm-profile-header-user-agree])
                  last
                  (html/select [:strong])
                  last :content first Integer/parseInt)
     :thank# (-> (html/select enlive-userpage
                              [:span.zm-profile-header-user-thanks])
                 last (html/select [:strong])
                 last :content first Integer/parseInt)
     :answer# (second navbar-numbers)
     :question# (first navbar-numbers)
     :favoriate# (nth navbar-numbers 3)
     :followee# followee#
     :follower# follower#
     :followees (delay (get-followees id followee#))
     :followers (delay (get-followers id follower#))
     }))

(defn get-answers
  "given a user id get a set of all answers"
  [])

(defn get-questions
  "given a user id get a set of all questions"
  [])
