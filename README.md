# cors-proxy
Java Jersey based CORS proxy to by-pass javascript cross-domain request restrictions

## About
Javascript from browsers cannot access resource from another domain due to CORS restrictions enforced by browsers.
This web proxy will by pass those restrictions.

## Environment
- Java 1.7+
- Tomcat 7+

## Usage

After you deploy this application locally, you can use below pattern to access from javascript:

http://localhost:8080/cors-proxy/request/forward/http//www.ebay.com
http://localhost:8080/cors-proxy/request/forward/http//www.amazon.com


**In you javascript, prefix all your url with** 'http://<cors-server-host>:8080/cors-proxy/request/forward/'.
This will bypass all CORS restrictions in browser.
