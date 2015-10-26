(ns clj-zhihu.core
  (:require [clj-http.client :as client]
            [clojure.java.io :as io]
            [seesaw.core :as ss]
            [clojure.data.json :as json]
            [taoensso.nippy :as nippy]
            [net.cgrand.enlive-html :as html])
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

(defn ^:private get-hashid
  "get the hashid value given an html page"
  [html-source]
  (if-let [hashid (last (re-find
                         #"hash_id&quot;: &quot;(.*)&quot;},"
                         html-source))]
    (if (< (count hashid) 5)
      (throw (Exception. "hashid not valid!"))
      hashid)
    (throw (Exception. "hashid not found!"))))

(defn ^:private get-captcha
  "return the captcha"
  []
  (let [captcha-url   "http://www.zhihu.com/captcha.gif"
        download-path "captcha.gif"
        _             (with-open [f (io/output-stream (io/file "resources" download-path))]
                        (.write f (:body (client/get captcha-url {:as :byte-array}))))
        image-frame   (show-image download-path)]
    (try (prn "please input the captcha")
         (read-line)
         (finally (ss/dispose! image-frame)))))

(defn ^:private force-login
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

(defn ^:private write-cookie-store
  "save cookie corresponding to a user"
  [user cookie-store]
  (with-open [f (io/output-stream (io/file "resources"
                                           "cookies"
                                           user))]
    (.write f (nippy/freeze cookie-store)))
  cookie-store)

(defn ^:private read-cookie-store
  "read cookie store given a user"
  [user]
  (if-let [cookie-file (io/resource (str "cookies/" user))]
    (with-open [f (io/input-stream cookie-file)]
      (nippy/thaw (IOUtils/toByteArray f)))))

(defn login?
  "return true if logged in with a cookie store, otherwise false"
  [cookie-store]
  (binding [clj-http.core/*cookie-store* cookie-store]
    (= 200 (:status (client/get "http://www.zhihu.com/settings/profile"
                                {:max-redirects 0})))))

(defn login
  "log into zhihu"
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
