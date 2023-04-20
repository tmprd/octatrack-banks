(ns octatrack.application.api
  "Application layer which provides a generic interface for using the domain"
  (:require [octatrack.domain.banks :as banks]
            [octatrack.application.validation :as validation]
            [octatrack.infrastructure.bank-files :as bank-files]
            [octatrack.infrastructure.project-folders :as project-folders]
            [octatrack.infrastructure.utils :as utils]
            [octatrack.infrastructure.logger :as log]
            [clojure.spec.alpha :as s]))

;; Specification
(s/fdef copy-project
  :args (s/cat :source-folder-path string?
               :target-folder-path string?
               :bank-pairs ::banks/bank-pairs))

;; Implementation
(defn- copy-project [source-folder-path target-folder-path bank-pairs]
  (bank-files/copy-bank-pairs bank-pairs
                              source-folder-path
                              target-folder-path)
  (project-folders/copy-other-project-files source-folder-path
                                            target-folder-path)
  (log/printlog "Other project files copied")
  (bank-files/show-missing-banks target-folder-path)
  nil) ;; return `nil` to not print out last returned value in console

(defn copy-project-command [source-folder-path target-folder-path bank-pair-strs]
  (log/set-project-name source-folder-path)
  (log/printlog (str "Using project folder: \"" source-folder-path "\""))
  (let [source-project-folder-path (project-folders/validate-project-folder source-folder-path)
        bank-pairs (validation/pair-strs->bank-pairs bank-pair-strs)]
    (log/printlog (str "Copying \"" source-folder-path "\" to \"" target-folder-path "\""))
    (when (utils/create-directory target-folder-path) (log/printlog (str "Created target folder \"" target-folder-path "\"")))
    (copy-project source-project-folder-path target-folder-path bank-pairs)))