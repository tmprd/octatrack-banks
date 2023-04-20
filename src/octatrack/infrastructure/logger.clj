(ns octatrack.infrastructure.logger
  "Infrastructure layer for formatting, displaying, & persisting application logs"
  (:require [clojure.string :as str]
            [clojure.java.io :as io]))

(def log-file-enabled? (atom false))
(def session-log (atom nil))
(def logs-folder-path "./otbanks-logs")
(def project-name (atom "project"))

(defn set-project-name [new-project-path]
  (reset! project-name (-> new-project-path
                           (str/split  #"/")
                           (last))))

(defn now [] (new java.util.Date))
(defn now-formatted [] (.format (java.text.SimpleDateFormat. "yyyy-MM-dd--hh-mm-ss-aaa") (now)))
(defn log-file-path [] (str logs-folder-path "/" @project-name "-" (now-formatted) ".txt"))

;; Exclamation mark means non-idempotent
(defn enable-log-file! []
  (reset! session-log nil)
  (reset! log-file-enabled? true)
  (.mkdir (io/file logs-folder-path)))

(defn write-to-file! []
  (when @log-file-enabled?
    (spit (log-file-path) @session-log)
    (reset! session-log nil)))

(defn append-to-log! [message]
  (when @log-file-enabled?
    (swap! session-log
           (fn [old new] (str old new "\n"))
           message)))

(defn printlog "Print message and write to log file" [message]
  (let [prefixed-message (str  "==> " message)]
    (println prefixed-message)
    (append-to-log! prefixed-message)))

(defn debug [message]
  (append-to-log! (str "DEBUG: " message)))