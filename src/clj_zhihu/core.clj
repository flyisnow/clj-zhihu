(ns clj-zhihu.core
  (:require [clj-http.client :as client]
            [clojure.java.io :as io]
            [seesaw.core :as ss]))

(defn ^:private show-image
  "show an image in a JFrame"
  [image-path]
  (->
   (ss/frame :title "Captcha"
             :content (ss/label :icon (seesaw.icon/icon (io/resource image-path))))
   ss/pack!
   ss/show!))

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
        _ (with-open [f (io/output-stream (io/resource download-path))]
            (.write f (:body (client/get captcha-url {:as :byte-array}))))
        image-frame (show-image download-path)]
    (try
      (prn "please input the captcha")
      (read-line)
      (finally
        (ss/dispose! image-frame)))))

(defn log-in
  "log in zhihu and return the cookie store"
  [user pass]
  (let [cookie-store (clj-http.cookies/cookie-store)]
    (binding [clj-http.core/*cookie-store* cookie-store]
      (let [login-page-url    "http://zhihu.com"
            form-submit-url   "http://www.zhihu.com/login/email"
            login-page-source (:body (client/get login-page-url))
            captcha           (get-captcha)
            xsrf              (get-xsrf login-page-source)]
        (client/post form-submit-url
                     {:form-params
                      {:email user
                       :password pass
                       :remember_me true
                       :_xsrf xsrf
                       :captcha captcha}})))
    cookie-store))
