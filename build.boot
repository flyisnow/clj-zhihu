(set-env! :resource-paths #{"resources"}
          :source-paths   #{"src"}
          :dependencies   #(into % '[[org.clojure/clojure "1.8.0"]
                                     [clj-http "2.1.0"]
                                     [seesaw "RELEASE"]
                                     [slingshot "RELEASE"]
                                     [com.taoensso/truss "RELEASE"]
                                     ;; [midje "RELEASE"]
                                     [adzerk/bootlaces "0.1.13" :scope "test"]
                                     [org.clojure/data.json "RELEASE"]
                                     [enlive "RELEASE"]
                                     ;; [zilti/boot-midje "RELEASE"]
                                     ]))

(require '[adzerk.bootlaces :refer :all])
(def project 'clj_zhihu)
(def version "0.1.0-SNAPSHOT")
(bootlaces! version)

(task-options!
 ;; aot {:namespace   #{'clj_project.core}}
 pom {:project     project
      :version     version
      :description "Zhihu API in Clojure"
      :url         "https://github.com/lianxiangru/clj-zhihu"
      :scm         {:url "https://github.com/lianxiangru/clj-zhihu"}
      :license     {"GPLv3"
                    "http://www.gnu.org/licenses/gpl-3.0.en.html"}}
 jar {:main        'clj_project.core
      :file        (str "clj_zhihu-" version "-standalone.jar")})

(deftask build
  "Build the project locally as a JAR."
  [d dir PATH #{str} "the set of directories to write to (target)."]
  (let [dir (if (seq dir) dir #{"target"})]
    (comp (pom) (target :dir dir))))
    ;; (comp (aot) (pom) (uber) (jar) (target :dir dir))))

;; (deftask run
;;   "Run the project."
;;   [a args ARG [str] "the arguments for the application."]
;;   (require '[clj_project.core :as app])
;;   (apply (resolve 'app/-main) args))

;; (require '[zilti.boot-midje :refer [midje]])
