(ns clj-zhihu.utils
  (:require [clj-http.client :as client]
            [clojure.java.io :as io]
            [seesaw.core :as ss])
  (:import [org.apache.commons.io IOUtils]))

(defn get-xsrf
  "get the xsrf value given cookiestore"
  [cookie-store]
  (-> (clj-http.cookies/get-cookies cookie-store)
      (get "_xsrf")
      :value))

(defn download
  "Download from url to path."
  [url path cookie-store]
  (with-open [f (io/output-stream (io/file path))]
    (.write f (:body (client/get url {:as :byte-array
                                      :cookie-store cookie-store})))))

(def regex-map
  {:hashid #"hash_id&quot;: &quot;(.*)&quot;},"
   :xsrf #"\<input\stype=\"hidden\"\sname=\"_xsrf\"\svalue=\"(\S+)\""
   :homepageid #"<h2.*?\\\/people\\\/(.*?)\\\" class=\\\"zg-link\\\""})

(defn find-by-regex
  "return the last group element given a regex and source"
  [source reg validator]
  (if-let [e (last (re-find reg source))]
    (if (validator e)
      e
      (throw (Exception. "Result not valid!")))
    (throw (Exception. "Result not found!"))))


(defn get-hashid
  "get the hash id given an html page"
  [html-source]
  (find-by-regex html-source
                 (:hashid regex-map)
                 not-empty))

(defn show-image
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
