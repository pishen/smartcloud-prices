## Assumptions

1. Assume the total number of kinds returned by `/instances` is always the same.
2. Assume the error response of smartcloud will not be counted into the quota.
3. Assume the error response of smartcloud can be recovered in limited retry attempts.

## Design decisions

Since I can only make 1000 requests every 24 hours, each price received from smartcloud is cached in an AtomicCell for a specific time interval. The time interval (in seconds) is determined by the total number of kinds. If there are `x` kinds on smartcloud, the time interval will be `86400 / ((1000 - 1) / x) + 1`, where `1000 - 1` reserve one request for getting total number of kinds. Then each kind is able to update its price at most `(1000 - 1) / x` times in a 24-hour window. The chances are evenly distributed over the 24-hour window, thus I got a `86400 / ((1000 - 1) / x)` time interval, and `+ 1` at the end since the value may not divide evenly.

When a request come, it first try to get the cached price from AtomicCell. If the price is expired, try updating the price in AtomicCell. In the updating function, it checks the expiration again and only make the request to smartcloud if it's really expired. Since AtomicCell can allow only one fiber to update its value at a time, this prevent multiple fibers from making redundant requests to smartcloud accidentally. This can be shown if we make a request with two same kinds, e.g. `http://localhost:8080/prices?kind=sc2-micro&kind=sc2-micro`.

Finally, I use sttp to wrap the http4s ember client for an easier request-building API. And use cats-retry to handle the occasional error from smartcloud. I actually found out there's a built-in Retry in http4s, but since it can't handle the general error like circe parsing exceptions, I keep the choice of cats-retry here.

## How to run

Start the server using sbt
```
sbt
> reStart
```

Start smartcloud
```
docker run --rm -p 9999:9999 smartpayco/smartcloud:latest
```

Make the request to http4s server
```
curl 'http://localhost:8080/prices?kind=sc2-micro&kind=sc2-micro'
```

To show the function of price expiration check, one can manually set a shorter value of expiration interval in `application.conf`, here we set an 1 second interval for example:
```
app {
    host = "0.0.0.0"
    port = 8080
    expire-interval = 1
}
```
