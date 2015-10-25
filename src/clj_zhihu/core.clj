(ns clj-zhihu.core
  (:require [clj-http.client :as client]
            [clojure.java.io :as io]
            [seesaw.core :as ss]
            [clojure.data.json :as json])
  (:import [org.apache.commons.io IOUtils]))

(defn ^:private show-image
  "show an image in a JFrame"
  [image-path]
  (with-open [captcha-stream (io/input-stream (io/resource image-path))]
    (let [captcha-bytearray (IOUtils/toByteArray captcha-stream)]
      (->
       (ss/frame :title "Captcha"
                 :content (ss/label :icon (seesaw.icon/icon
                                           (javax.swing.ImageIcon. captcha-bytearray))))
       ss/pack!
       ss/show!))))


(defn ^:private get-xsrf
  "get the xsrf value given an html page"
  [html-source]
  (if-let [xsrf (last (re-find
                       #"\<input\stype=\"hidden\"\sname=\"_xsrf\"\svalue=\"(\S+)\""
                       html-source))]
    (if (< (count xsrf) 5)
      (throw (Exception. "xsrf not valid!"))
      xsrf)
    (throw (Exception. "xsrf not found!"))))

(defn ^:private get-captcha
  "return the captcha"
  []
  (let [captcha-url   "http://www.zhihu.com/captcha.gif"
        download-path "captcha.gif"
        _ (with-open [f (io/output-stream (io/file "resources" download-path))]
            (.write f (:body (client/get captcha-url {:as :byte-array}))))
        image-frame (show-image download-path)]
    (try (prn "please input the captcha")
         (read-line)
         (finally (ss/dispose! image-frame)))))

(defn login
  "log in zhihu and return the cookie store"
  [user pass]
  (let [cookie-store (clj-http.cookies/cookie-store)]
    (binding [clj-http.core/*cookie-store* cookie-store]
      (let [login-page-url    "http://zhihu.com"
            form-submit-url   "http://www.zhihu.com/login/email"
            login-page-source (:body (client/get login-page-url))
            captcha           (get-captcha)
            xsrf              (get-xsrf login-page-source)]
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

(defmacro with-zhihu-account
  "use a zhihu account for following actions
  usage:
  (with-zhihu-account [username password]
    actions)"
  [bindings & body]
  `(binding [clj-http.core/*cookie-store*
             (apply login ~bindings)]
     ~@body))
