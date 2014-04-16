(ns kafka-mon.core
  (:require [clojure.tools.cli :refer [cli]]
    [kafka-mon.offsets :refer [prn-offsets]])
  (:gen-class))



(defn check-opts2 [opts]
  opts)

(defn check-opts [[opts _ usage] ]
  (if-let [m (check-opts2 opts)] m (do (prn usage) false)) )

(defn parse-broker-list 
  "Parse a string like localhost:9092,abc:9092 into [{:host localhost, :port 9092} {:host abc, :port 9092}]"
  [broker-str]
  (reduce (fn [state [host port]]
            (conj state {:host host :port (if port (Integer/parseInt port) 9092)})) [] (map #(clojure.string/split % #":") (clojure.string/split broker-str #"[, ;]"))))


(defn cmd [args]
   (cli args
     ["-c" "--cmd" "Command to specify [metadata, offsets]"] 
     ["-t" "-topics" "Kafka topics to use comma emicomma or white space separated" :parse-fn #(clojure.string/split % #"[,; ]")]
     ["-g" "-group" "Logical group to use to get consumer offsets, default is default-group" :default "default-group"]
     ["-b" "--brokers" "Metadata broker list as a comma semicomma or white space separated list broker1:9092,broker2:9092" :parse-fn parse-broker-list]
     ["-f" "--format" "Output format can be json,csv or tsv"]
     ["-r" "--redis" "Redis host"]
     ["-h" "--help" :flag true]))


(defn- prn-usage [args]
  (print (nth (cmd args) 2)))
         
(defn -main [& args]
     (if-let [{:keys [help cmd redis brokers group topics format] :as opts} (check-opts (cmd args) )]
       (cond 
         help 
         (prn-usage args)
         (= cmd "offsets")
         (prn-offsets group redis brokers format topics)
         :else
         (prn-usage args)))
     (System/exit 0))
         