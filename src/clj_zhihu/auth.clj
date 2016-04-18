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
            [aprint.core :refer [aprint ap]]
            [clojure.reflect :refer [reflect]]
            [net.cgrand.enlive-html :as html]
            ))

(def ^:private headers
  "Headers for posting and getting."
  {
   :User-Agent "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/42.0.2311.135 Safari/537.36",
   :Host "www.zhihu.com",
   :Referer "http://www.zhihu.com/",
   :X-Requested-With "XMLHttpRequest"
   }
  )

(defn- get-login-captcha
  "Input login captcha"
  []
  ;; Some insane Zhihu logic
  (client/get "https://www.zhihu.com" {:headers headers})
  (client/post "https://www.zhihu.com/login/email" {:form-params
                                                    {:email ""
                                                     :password ""
                                                     :remember_me true}
                                                    :headers headers})
  (utils/download (str "https://www.zhihu.com/captcha.gif?r="
                    (System/currentTimeMillis)
                    "&type=login")
                  "resources/captcha.gif")
  (prn "Captcha saved in resources/captcha.gif, please input the text.")
  (read-line))

(defn- post-login-info
  "Post login info. Return response map."
  [username password]
  (client/get "https://www.zhihu.com/#signin" {:headers headers})
  (-> (client/post "https://www.zhihu.com/login/email"
        {:form-params {:email username
                       :password password
                       :remember_me true
                       :_xsrf (utils/get-xsrf)
                       :captcha (get-login-captcha)}
         :headers headers})
      (:body)
      (json/read-str)))

(defn login
  "Log into zhihu. Return cookie store."
  [username password]
  (have string? username)
  (have string? password)
  (binding [clj-http.core/*cookie-store* (cookie/cookie-store)]
    (let [resp (post-login-info username password)]
      (if (= 0 (get resp "r"))
        clj-http.core/*cookie-store*
        (throw+ {:type :login-failed :response resp})))))

(defn login?
  "return true if logged in with a cookie store, otherwise false"
  [cookie-store]
  (= 200
     (:status (client/get "http://www.zhihu.com/settings/profile"
                          {:max-redirects 0
                           :cookie-store cookie-store}))))

;; (login "lianxiangru@gmail.com" "VEqHr1LbTS0Kg")
