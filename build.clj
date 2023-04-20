(ns build
  (:require [clojure.tools.build.api :as b]))

(defn get-sem-version [] (-> "deps.edn"
                             slurp
                             read-string
                             :version))

(def uber-main 'octatrack.application.console)
(def lib 'otbanks)
(def version (format "%s.%s" (get-sem-version) (b/git-count-revs nil)))
(def class-dir "target/classes")
(def basis (b/create-basis {:project "deps.edn"}))
(def jar-file (format "target/%s-%s.jar" (name lib) version))
(def uber-file (format "target/uberjar/%s.jar" (name lib)))

(defn clean [_] (b/delete {:path "target"}))

(defn jar [_]
  (b/write-pom {:class-dir class-dir
                :lib lib
                :version version
                :basis basis
                :src-dirs ["src"]})
  (b/copy-dir {:src-dirs ["src"]
               :target-dir class-dir})
  (b/jar {:class-dir class-dir
          :jar-file jar-file}))


(defn uber [_]
  (clean nil)
  (b/copy-dir {:src-dirs ["src"]
               :target-dir class-dir})
  (b/compile-clj {:basis basis
                  :src-dirs ["src"]
                  :class-dir class-dir})
  (b/uber {:class-dir class-dir
           :uber-file uber-file
           :basis basis
           :main uber-main})
  ;; output version
  (println version))