

kafka-clj
==========

fast kafka send library implemented in clojure


#Usage

Please note that this library is still under development, any contributions are welcome

```[kafka-clj "0.2.0-SNAPSHOT"]```

## Multi Producer

The ```kafka-clien.client``` namespace contains a ```create-connector``` function that returns a async multi threaded thread safe connector.
One producer will be created per topic partition combination, each with its own buffer and timeout, such that compression can be maximised.


```clojure

(use 'kafka-clj.client :reload)

(def msg1kb (.getBytes (clojure.string/join "," (range 278))))
(def msg4kb (.getBytes (clojure.string/join "," (range 10000))))

(def c (create-connector [{:host "localhost" :port 9092}] {}))

(time (doseq [i (range 100000)] (send-msg c "data" msg1kb)))

```

## Single Producer 
```clojure
(use 'kafka-clj.produce :reload)

(def d [{:topic "data" :partition 0 :bts (.getBytes "HI1")} {:topic "data" :partition 0 :bts (.getBytes "ho4")}] )
;; each message must have the keys :topic :partition :bts, there is a message record type that can be created using the (message topic partition bts) function
(def d [(message "data" 0 (.getBytes "HI1")) (message "data" 0 (.getBytes "ho4"))])
;; this creates the same as above but using the Message record

(def p (producer "localhost" 9092))
;; creates a producer, the function takes the arguments host and port

(send-messages p {} d)
;; sends the messages asyncrhonously to kafka parameters are p , a config map and a sequence of messages

(read-response p 100)
;; ({:topic "data", :partitions ({:partition 0, :error-code 0, :offset 2131})})
;; read-response takes p and a timeout in milliseconds on timeout nil is returned
```

# Benchmark Producer

Environment:

Network: 10 gigbit
Brokers: 4
CPU: 24 (12 core hyper threaded)
RAM: 72 gig (each kafka broker has 8 gig assigned)
DISKS: 12 Sata 7200 RPM (each broker has 12 network threads and 40 io threads assigned)
Topics: 8

Client: (using the lein uberjar command and then running the client as java -XX:MaxDirectMemorySize=2048M -XX:+UseCompressedOops -XX:+UseG1GC -Xmx4g -Xms4g  -jar kafka-clj-0.1.4-SNAPSHOT-standalone.jar)


Results:
1 kb messag (generated using (def msg1kb (.getBytes (clojure.string/join "," (range 278)))) )

```clojure
(time (doseq [i (range 1000000)] (send-msg c "data" msg1kb)))
;;"Elapsed time: 5209.614983 msecs"
```

191975 K messages per second.


# Consumer
Note: this is still very experimental

```clojure

(require '[kafka-clj.consumer :refer [consumer read-msg]])

;create a consumer using localhost as the bootstrap brokers, you can provide more
;read from the "ping" topic, again you can specify more topics here.
;use-earliest true means that the consumer will start from the latest messages
;locking group management and offsets are all saved in redis.the redis conf and options can be found at
;https://github.com/gerritjvv/group-redis
(def c (consumer [{:host "localhost" :port 9092}] ["ping"] {:use-earliest true :max-bytes 1073741824 :metadata-timeout 60000 :redis-conf {:redis-host "localhost" :heart-beat 10} }))

;create a lazy sequence of messages
(defn lazy-ch [c]
  (lazy-seq (cons (read-msg c) (lazy-ch c))))

;the messages returned are FetchMessage [topic partition ^bytes bts offset locked]

(doseq [msg (take 10 (lazy-ch c))]
  (prn (String. (:bts msg))))
 
```

##Consumer metrics

The api uses http://metrics.codahale.com/ for metrics.
The following metrics are provides

```
kafka-consumer.consume-#[number]        Message consumption per second
kafka-consumer.redis-reads-#[number]    Redis reads per second
kafka-consumer.redis-writes-#[number]   Redis writes per second
kafka-consumer.msg-size-#[number]       Histogram of message byte sizes
kafka-consume.cycle-#[number]           Internal metrics to time each cycle between consume and check for new members
```


```clojure

(require '[kafka-clj.metrics :refer [ report-consumer-metrics ]])
(report-consumer-metrics :console :freq 10) ;report to stdout every 10 seconds
(report-consumer-metrics :csv :freq 10 :dir "/tmp/mydir") ; report to the directory :dir every 10 seconds

```

The metrics registry is held in ```kafka-clj.consumer.metrics-registry```, and can be used to customize reporting






