(ns clj-zhihu.auth ^{:author "Xiangru Lian"}
  (:require [clj-http.client :as client]
            [clojure.java.io :as io]
            [clojure.data.json :as json]
            [clj-zhihu.utils :refer [get-xsrf get-captcha
                                     write-cookie-store
                                     read-cookie-store]])
    (:import [org.apache.commons.io IOUtils]))

;; (def headers-for-zhihu
;;   {:Accept "*/*"
;;    :Accept-Encoding "gzip, deflate"
;;    :Accept-Language "en,zh-CN;q=0.8,zh;q=0.6,zh-TW;q=0.4"
;;    :Connection "keep-alive"
;;    :Content-Type "application/x-www-form-urlencoded; charset=UTF-8"
;;    :Host "www.zhihu.com"
;;    :Origin "http://www.zhihu.com"
;;    :Referer "http://www.zhihu.com/"
;;    :User-Agent "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Ubuntu Chromium/45.0.2454.101 Chrome/45.0.2454.101 Safari/537.36"
;;    :X-Requested-With "XMLHttpRequest"})

(defn- force-login
  "log in zhihu and return the cookie store"
  [user pass]
  (let [cookie-store (clj-http.cookies/cookie-store)]
    (binding [clj-http.core/*cookie-store* cookie-store]
      (let [login-page-url    "http://zhihu.com"
            form-submit-url   "http://www.zhihu.com/login/email"
            login-page-source (:body (client/get login-page-url))
            captcha           (get-captcha)
            xsrf              (find-by-regex login-page-source
                                             #"\<input\stype=\"hidden\"\sname=\"_xsrf\"\svalue=\"(\S+)\""
                                             not-empty)]
        (case  (-> (:body (client/post form-submit-url
                                       {:form-params
                                        {:email user
                                         :password pass
                                         :remember_me true
                                         :_xsrf xsrf
                                         :captcha captcha}}))
                   json/read-str
                   (get "r"))
          0 (prn "login success")
          (throw (Exception. "login Failed")))))
    cookie-store))


(defn login?
  "return true if logged in with a cookie store, otherwise false"
  [cookie-store]
  (binding [clj-http.core/*cookie-store* cookie-store]
    (= 200 (:status (client/get "http://www.zhihu.com/settings/profile"
                                {:max-redirects 0})))))

(defn login
  "log into zhihu and return the cookie store"
  [user pass]
  (let [cookie (read-cookie-store user)]
    (if (or (nil? cookie)
            (not (login? cookie)))
      (write-cookie-store user (force-login user pass))
      (do (prn "login success")
          cookie))))

(defmacro with-zhihu-account
  "use a zhihu account for following actions
  usage:
  (with-zhihu-account [username password]
    actions)"
  [bindings & body]
  `(binding [clj-http.core/*cookie-store*
             (apply login ~bindings)]
     ~@body))
