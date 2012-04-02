# Soup2Tumblr
You want to move your stuff from soup.io to Tumblr? Well I did, so I put together this stuff here, which takes the soup.io rss export, parses it, and submits it to Tumblr via its API (v1). It's written pretty badly, but works most of the time. Feel free to ask me any questions if it doesn't.

## Libraries
I used a bunch of libraries to build this:
* tumblr-java, a java library for the Tumblr API. Released under Apache 2.0 License.
* json.simple, a simple Java toolkit for JSON. Released under Apache 2.0 License.
* Apache Commons Lang. Released under Apache 2.0 License.

The libraries are included in the libs directory. tumblr-java uses a few libraries as well, which need to be included. Just run git submodule update --init to get the repository, and inclue the jar-files from libs/tumblr-java/libs in your classpath as well.
