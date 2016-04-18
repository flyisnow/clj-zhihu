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

(ns ^{:doc "The clj-zhihu auth functionality."
      :author "Xiangru Lian"}
    clj-zhihu.auth
  (:require [clj-http.client :as client]
            [clj-http.cookies :as cookie]
            [clojure.data.json :as json]
            [taoensso.truss :as truss :refer (have have! have?)]
            [slingshot.slingshot :refer [throw+ try+]]
            [clj-zhihu.utils :as utils]
            ;; [aprint.core :refer [aprint ap]]
            ;; [clojure.reflect :refer [reflect]]
            ))

(defn- get-login-captcha
  "Input login captcha"
  [cookie-store]
  (utils/download "http://www.zhihu.com/captcha.gif"
                  "resources/captcha.gif"
                  cookie-store)
  (prn "Captcha saved in resources/captcha.gif, please input the text.")
  (read-line))

(defn- post-login-info
  "Post login info. Return response map."
  [username password cookie-store]
  (-> (client/post "http://www.zhihu.com/login/email"
                   {:form-params {:email username
                                  :password password
                                  :remenber_me true
                                  :_xsrf (utils/get-xsrf cookie-store)
                                  :captcha (get-login-captcha cookie-store)}
                    :cookie-store cookie-store})
      (:body)
      (json/read-str)))

(defn login
  "Log into zhihu. Return cookie store."
  [username password]
  (have string? username)
  (have string? password)
  (let [cookie-store (cookie/cookie-store)
        resp (post-login-info username password cookie-store)]
    (if (= 0 (get resp "r"))
      cookie-store
      (throw+ [:type :login-failed]))
     ))

(defn login?
  "return true if logged in with a cookie store, otherwise false"
  [cookie-store]
  (= 200
     (:status (client/get "http://www.zhihu.com/settings/profile"
                          {:max-redirects 0
                           :cookie-store cookie-store}))))
