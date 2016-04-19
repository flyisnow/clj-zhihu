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
            [clj-zhihu.utils :as utils]
            [net.cgrand.enlive-html :as html]
            [taoensso.truss :as truss]
            [clojure.string :as str]))

(defn question-title
  "Given an enlive structure, return question title."
  [m]
  (-> (html/select m [[:h2 (html/attr= :class "zm-item-title zm-editable-content")]])
      first :content first str/trim))

(defn question-tags
  "Given an enlive structure, return the tags as a sequence."
  [m]
  (->> (html/select m [[:a (html/attr= :class "zm-item-tag")]])
       (mapcat :content)
       (map str/trim)))

(defn question-description
  "Given an enlive structure, return the description."
  [m]
  (->> (html/select m [(html/attr= :class "zm-editable-content")])
       first :content first str/trim))

(defn question-answers
  "Given an enlive structure, return a sequence of answers' url."
  [m]
  (->> (html/select m [(html/attr= :class "answer-date-link-wrap")])
       (mapcat :content) (map :attrs) (remove nil?) (map :href)
       (map #(str "https://www.zhihu.com" %))))

(defn question
  "Given a question url, return enlive structure."
  [url]
  (utils/url-to-enlive
   (truss/have #(re-matches #"(http|https)://www.zhihu.com/question/\d{8}" %)
               url :data "url not valid")))

;; (binding [clj-http.core/*cookie-store* cookie-store]
;;   (-> (question "https://www.zhihu.com/question/41945693")
;;       ((juxt question-title question-description question-tags))))
