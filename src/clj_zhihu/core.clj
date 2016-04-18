;; clj-zhihu
;; Copyright (C) 2016  Xiangru Lian <xlian@gmx.com>

;; This program is free software: you can redistribute it and/or modify
;; it under the terms of the GNU General Public License as published by
;; the Free Software Foundation, either version 3 of the License, or
;; (at your option) any later version.

;; This program is distributed in the hope that it will be useful,
;; but WITHOUT ANY WARRANTY; without even the implied warranty of
;; MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
;; GNU General Public License for more details.

;; You should have received a copy of the GNU General Public License
;; along with this program.  If not, see <http://www.gnu.org/licenses/>.

(ns ^{:doc "The clj-zhihu core functionality."
      :author "Xiangru Lian"}
    clj-zhihu.core
  (:require [clj-http.client :as client]
            [clojure.java.io :as io]
            [clojure.data.json :as json]
            [net.cgrand.enlive-html :as html]
            [clj-zhihu.utils :refer [get-hashid
                                     get-xsrf
                                     find-by-regex
                                     get-captcha
                                     regex-map]]
            [clj-zhihu.auth :as auth]))

(defn- get-followees
  "get a sequence of followees' id given the id of a user"
  [id followee#]
  (if (zero? followee#)
    '()
    (try
      (let [url      "http://www.zhihu.com/node/ProfileFolloweesListV2"
            userurl  (str "http://www.zhihu.com/people/" id "/followers")
            userpage (-> userurl client/get :body)
            hashid   (get-hashid userpage)
            xsrf     (get-xsrf userpage)]
        (mapcat #(->> (client/post
                       url
                       {:form-params
                        {:method "next"
                         :params (json/write-str
                                  {:offset (* 20 %1)
                                   :order_by "created"
                                   :hash_id hashid})
                         :_xsrf xsrf}})
                      :body
                      (re-seq (:homepageid regex-map))
                      (map second))
                (range 0 (Math/ceil (/ followee# 20)))))
      (catch Exception e nil))))

(defn- get-followers
  "given user id return a sequence of user id of the followers"
  [id follower#]
  (if (zero? follower#)
    '()
    (try
      (let [url      "http://www.zhihu.com/node/ProfileFollowersListV2"
            userurl  (str "http://www.zhihu.com/people/" id "/followers")
            userpage (-> userurl client/get :body)
            hashid   (get-hashid userpage)
            xsrf     (get-xsrf userpage)]
        (mapcat #(->> (client/post
                       url
                       {:form-params
                        {:method "next"
                         :params (json/write-str
                                  {:offset (* 20 %1)
                                   :order_by "created"
                                   :hash_id hashid})
                         :_xsrf xsrf}})
                      :body
                      (re-seq (:homepageid regex-map))
                      (map second))
                (range 0 (Math/ceil (/ follower# 20)))))
      (catch Exception e nil))))

(defn- get-news
  "given a user id get a sequence of news"
  [id]
  )

(defn- get-answers
  "given a user id get a sequence of all answers"
  [id answer#]
  (let [url (str "http://www.zhihu.com/people/" id "/answers")]
    (letfn [(get-page-tree
              [page#]
              (->> (client/get
                    url
                    {:query-params {:page page#}})
                   :body
                   (java.io.StringReader.)
                   (html/html-resource)))
            (get-vote-sequence
              [tree]
              (->> (html/select tree [:div.zm-item-vote-info])
                   (map :attrs)
                   (map :data-votecount)
                   (map #(Integer/parseInt %))))
            (get-link-sequence
              [tree]
              (->> (html/select tree [:div.zm-item-rich-text])
                   (map :attrs)
                   (map :data-entry-url)))
            (get-content-sequence
              [tree]
              (->> (html/select tree [:textarea.content.hidden])
                   (map :content)
                   (map first)))
            (get-answer-map-sequence
              [tree]
              (letfn [(answer-map-construct [vote content link]
                        {:vote vote :content content :link link})]
                (map apply (repeat answer-map-construct)
                     (apply map vector
                            ((juxt get-vote-sequence
                                   get-link-sequence
                                   get-content-sequence)
                             tree)))))]
      (mapcat #(get-answer-map-sequence (get-page-tree %1))
              (range 1 (inc (Math/ceil (/ answer# 20))))))))

(defn user
  "given user id return a user map"
  [id]
  (let [userurl           (str "http://www.zhihu.com/people/" id)
        userpage          (-> userurl
                              (client/get {:throw-exceptions false})
                              :body)
        enlive-userpage   (html/html-resource (java.io.StringReader. userpage))
        enlive-navbarnode (html/select enlive-userpage
                                       [:div.profile-navbar.clearfix])
        navbar-numbers    (map (comp #(Integer/parseInt %) #(or % "0") first :content)
                               (html/select enlive-navbarnode [:span.num]))
        followee#         (-> (html/select enlive-userpage
                                           [:div.zm-profile-side-following.zg-clear])
                              (html/select [:strong])
                              first :content first (or "0") Integer/parseInt)
        follower#         (-> (html/select enlive-userpage [:div.zm-profile-side-following.zg-clear])
                              (html/select [:strong])
                              second :content first (or "0") Integer/parseInt)
        answer#           (second navbar-numbers)
        question#         (first navbar-numbers)
        favoriate#        (get navbar-numbers 3)]
    {:name        (-> (html/select enlive-userpage [:span.name])
                      last :content first)
     :mood        (-> (html/select enlive-userpage [:span.bio])
                      first :content first)
     :icon        (-> (html/select enlive-userpage [:img.avatar.avatar-l])
                      last :attrs :src)
     :location    (-> (html/select enlive-userpage [:span.location.item])
                      last :attrs :title)
     :business    (-> (html/select enlive-userpage [:span.business.item])
                      last :attrs :title)
     :gender      ((fn [class-name]
                     (if (nil? class-name)
                       nil
                       (if (.contains class-name "female")
                         :female
                         (when (.contains class-name "male")
                           :male))))
                   (-> (html/select enlive-userpage [:span.item.gender])
                       first :content first :attrs :class))
     :employment  (-> (html/select enlive-userpage [:span.employment.item])
                      last :attrs :title)
     :position    (-> (html/select enlive-userpage [:span.position.item])
                      last :attrs :title)
     :education   (-> (html/select enlive-userpage [:span.education.item])
                      last :attrs :title)
     :major       (-> (html/select enlive-userpage [:span.education-extra.item])
                      last :attrs :title)
     :description (-> (html/select enlive-userpage
                                   [:span.description.unfold-item])
                      last (html/select [:span.content])
                      last :content first)
     :upvote#     (-> (html/select enlive-userpage
                                   [:span.zm-profile-header-user-agree])
                      last (html/select [:strong])
                      last :content first (or "0") Integer/parseInt)
     :thank#      (-> (html/select enlive-userpage
                                   [:span.zm-profile-header-user-thanks])
                      last (html/select [:strong])
                      last :content first (or "0") Integer/parseInt)
     :answer#     answer#
     :question#   question#
     :favoriate#  favoriate#
     :followee#   followee#
     :follower#   follower#
     :followees   (get-followees id followee#)
     :followers   (get-followers id follower#)
     :answers     (get-answers id answer#)
     }))

(defn get-questions
  "given a user id get a set of all questions"
  [])
