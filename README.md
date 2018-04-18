# RetrofitRequestManager

Manages caching and offline Retrofit usage. For Kotlin with RXJava only.

### Usage

[The example activity](https://github.com/Frank1234/RetrofitRequestManager/blob/master/app/src/main/java/com/ironflowers/rm/testapp/SimpleExampleActivity.kt) shows a simple example.

// TODO add steps on how to use it here. 

### Main Features
#### Caching Types
- *retrieveLocal* - Fetches data from local cache.
- *retrieveRemote* - Fetches data from remote server.
- *retrieveLocalFallbackRemote* - Fetches data from remote server. On failure, it tries to fetch a cached response.
- *retrieveRemoteFallbackLocal* - Fetches data from local cache. On failure / not found, it tries to fetch it from the server.
- *retrieveLocalAndRemote* - Fetches data from cache and server at the same time. May emit multiple success and/or failure responses.

#### Multi requests
If you do a request with the same TAG twice, the second will subscribe to the same request as the first; the request will only be done once but both observers get the response.
