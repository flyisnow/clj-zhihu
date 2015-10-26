(ns clj-zhihu.utils
  (:require [clj-http.client :as client]
            [clojure.java.io :as io]
            [seesaw.core :as ss]))

(defn get-xsrf
  "get the xsrf value given an html page"
  [html-source]
  (if-let [xsrf (last (re-find
                       #"\<input\stype=\"hidden\"\sname=\"_xsrf\"\svalue=\"(\S+)\""
                       html-source))]
    (if (< (count xsrf) 5)
      (throw (Exception. "xsrf not valid!"))
      xsrf)
    (throw (Exception. "xsrf not found!"))))

(defn get-hashid
  "get the hashid value given an html page"
  [html-source]
  (if-let [hashid (last (re-find
                         #"hash_id&quot;: &quot;(.*)&quot;},"
                         html-source))]
    (if (< (count hashid) 5)
      (throw (Exception. "hashid not valid!"))
      hashid)
    (throw (Exception. "hashid not found!"))))

(defn get-captcha
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
