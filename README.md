**Betting Stake Server**

This is an HTTP-based back-end which stores and provides
betting offer's stakes for different customers, with the
capability to return the highest stakes.

**How to Run**

Since this is a pure java project with no external dependencies
or deployment steps required.
Only need to download the JAR file in root folder, make sure JDK installed
locally and then run it with following command:

`java -jar betting_stake_server.jar
`

**API Usage**

* Get session
GET http://localhost:9090/{customerId}/session

* Post a customer’s stake on a betting off
POST http://localhost:9090/{betofferid}/stake?sessionkey={sessionkey}
Request Body: <stake>

* Get a high stakes list for a betting offer
GET http://localhost:9090/{betofferid}/highstakes

**Design Decisions**

Given the service needs to be able to handle a lot of simultaneous requests, I have adopted the following design strategy:
* A ThreadPool is introduced at the entry point to maximize CPU utilization and improve processing efficiency under high 
concurrency, A bounded queue is used instead of an unbounded queue to reduce the risk of OOM.
* ConcurrentHashMap is selected for storage to guarantee thread safety and achieve efficient read and write.
* Expired sessions are also cleaned up properly to release memory and avoid unnecessary resource occupation.

**Future Enhancements**

* Traffic Control: To introduce Rate limiter to protect the service under peak traffic.
* ThreadPool Tuning: Optimize thread pool parameters based on actual prod hardware to balance throughput and resource usage.

**Project Structure**

betting_stake_server/
├── src/
│   ├── main/
│   │   ├── java/
│   │       ├── com/
│   │           ├── betting/
│   │               ├── handler/
│   │               │   └── RootHandler.java # Root http request to corresponding business logic
│   │               ├── manager/
│   │               │   ├── SessionManager.java # \Manager customer sessions (generation, validation, expiration)
│   │               │   └── StakeManager.java # Manager customer stake, add stake and get top 20 stakes for a betting offer
│   │               └── BettingStakeServerMain.java 
│   ├── test/
│       ├── java/
│           ├── SessionManagerTest.java
│           └── StakeManagerTest.java
└── README.md
