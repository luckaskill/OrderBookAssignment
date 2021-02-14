# order-book-assignment

## Require:
1. Java 8+
2. Maven 3

## Install:
In console go to the project folder and type `mvn clean package`

## Execute:
In console set credits as environment variables. For example for windows it's like below

**`set BSDEX_API_KEY=put-your-api-key-here`**
**`set BSDEX_API_SECRET=put-your-api-secret-here`**

Then go to the **`target`** folder in project root and type `java -jar orderbook-1.0.jar`

## How it works:
On application startup the system consuming balances from the rest api and log the response.
Then opens websocket connection, subscribes to appropriate channel 
and starts handling the orderbook messages(writing to data structures, sorting, calculating values).

After subscribing, the system open own websocket connection and start sending data through it.
On the client side JavaScript subscribe to server channel at handling the data(displays it).

Server logic of handling the orderbook messages based on SortedMap. 
That is allow to refuse the brute force and significantly increase the performance.

## Frameworks and libraries
1. Spring Web
2. Spring WebSocket as main library for websockets
3. Apache Utilities (HMAC and IO)
4. Lombok
5. Apache httpclient for REST API requests
6. stomp.js and sockjs for UI
