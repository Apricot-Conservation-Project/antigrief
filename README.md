### GriefLess

Helps combat griefers on your server. At the moment prevents players from joining from dodgy ips (vpns, proxies, etc)

This code uses the free requests on http://getipintel.net/, of which you have 500 per day. Going over this limit, or using an ip with no valid email attached, will cause your ip to be permanently banned from using the service, so make sure to test this on a different ip to prod first so you don't get banned.

To help mitigate the number of requests used, this plugin remembers ips and their associated scores so a database can be queried first before the site.

Anyone is free to use this code for any purpose, including services outside of Mindustry
