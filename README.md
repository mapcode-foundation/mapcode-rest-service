# README for Mapcode REST API Web Services

[![Build Status](https://img.shields.io/travis/mapcode-foundation/mapcode-rest-service.svg)](https://travis-ci.org/mapcode-foundation/mapcode-rest-service)
[![Coverage Status](https://coveralls.io/repos/github/mapcode-foundation/mapcode-rest-service/badge.svg)](https://coveralls.io/github/mapcode-foundation/mapcode-rest-service)
[![Release](https://img.shields.io/github/release/mapcode-foundation/mapcode-rest-service.svg)](https://github.com/mapcode-foundation/mapcode-rest-service/releases)

**Copyright (C) 2014-2016 Stichting Mapcode Foundation (http://www.mapcode.com)**

This application provides a REST API for mapcodes. It uses the Java Library for Mapcodes
extensively. The API supports both XML and JSON (default) responses.

**Warning:** *If you want to use this source to run a production service, always use the source
version from the **latest release tag**. Release names are tagged "vX.Y.Z". The head of the master
branch may not be stable during development.*

The available REST API methods are:

    GET /mapcode         Returns this help page.
    GET /mapcode/version Returns the software version.
    GET /mapcode/metrics Returns some system metrics (also available from JMX).
    GET /mapcode/status  Returns 200 if the service OK.

    GET /mapcode/codes/{lat},{lon}[/[mapcodes|local|international]]
         [?precision=[0|1|2] & territory={restrictToTerritory} & alphabet={alphabet} & include={offset|territory|alphabet}]

       Convert latitude/longitude to one or more mapcodes.
       Path parameters:
         lat             : Latitude, range [-90, 90] (automatically limited to this range).
         lon             : Longitude, range [-180, 180] (automatically wrapped to this range).

       An additional filter can be specified to limit the results:
         all             : Same as without specifying a filter, returns all mapcodes.
         local           : Return the shortest local mapcode.
         international   : Return the international mapcode.

       Query parameters:
         precision       : Precision, range [0, 2] (default=0).
         territory       : Territory to restrict results to, numeric or alpha code.
         alphabet        : Alphabet to return results in, numeric or alpha code.

         include         : Multiple options may be set, separated by comma's:
                             offset    = Include offset from mapcode center to lat/lon (in meters).
                             territory = Always include territory in result, also for territory 'AAA'.
                             alphabet  = Always the mapcodeInAlphabet, also if same as mapcode.

                           Note that you can use 'include=territory,alphabet' to ensure the territory code
                           is always present, as well as the translated territory and mapcode codes.
                           This can make processing the records easier in scripts, for example.

    GET /mapcode/coords/{code} [?context={territoryContext}]
       Convert a mapcode into a latitude/longitude pair.

       Path parameters:
         code            : Mapcode code (local or international).
       Query parameters:
         context         : Optional mapcode territory context, numeric or alpha code.

    GET /mapcode/territories [?offset={offset}&count={count}]
       Return a list of all territories.

    GET /mapcode/territories/{territory} [?context={territoryContext}]
       Return information for a single territory code.

       Path parameters:
         territory       : Territory to get info for, numeric or alpha code.

       Query parameters:
         context         : Territory context (optional, for disambiguation).

    GET /mapcode/alphabets [?offset={offset}&count={count}]
       Return a list of all alphabet codes.

    GET /mapcode/alphabets/{alphabet}
       Return information for a specific alphabet.

       Path parameters:
         alphabet        : Alphabet to get info for, numeric or alpha code.

    General query parameters for methods which return a list of results:

       offset            : Return list from 'offset' (negative value start counting from end).
       count             : Return 'count' items at most.

## Supporting XML-only Tools (e.g. Google Spreadsheets, Microsoft Excel)

The REST API methods defined above obey the HTTP `Accept:` header. To retrieve JSON responses,
use **Accept:application/json**, to retrieve XML responses, use **Accept:application/xml**.
The default response type, if no header is specified, is **JSON**.

Some tools, like Google Spreadheets and Microsoft Excel, can only handle XML responses, but do
not provide the correct HTTP `Accept:` header. In these cases you can alternatively
retrieve **XML** responses with these alias URLs (note the "/xml" in the URLs):

    GET /mapcode/xml/version
    GET /mapcode/xml/status
    GET /mapcode/xml/codes
    GET /mapcode/xml/coords
    GET /mapcode/xml/territories
    GET /mapcode/xml/alphabets

These URLs only provide XML responses, with or without the HTTP `Accept:` header.

You can use these URLs, for example, in Google Spreadsheets, using the **=IMPORTXML(url, xpath)**
function, or in Microsoft Excel 2013 or 2016 (for Windows) using the **=FILTERXML(WEBSERVICE(url), xpath)**
functions. This should make live integration of mapcode conversion with your spreadsheets a breeze.

## Build and Run

The service always runs from a WAR file.
To build the WAR file, type

    cd <project-root>
    mvn clean package

You can run the WAR file in 3 ways:

1. directly from the **command-line**, using:


    java -jar deployment/target/deployment-<version>.war [--port <port>] [--silent] [--debug] [--help]

  This will start the service at `http://localhost:<port>/mapcode`. If `<port>` is not specified, the
  default value is `8080`. If it is `0`, the server will choose any free port.

2. directly from **Maven** using:
 
 
     cd deployment
     mvn jetty:run
     
  This will start the service at `http://localhost:8080/mapcode`.

3. in a **Tomcat server**, deploying the file `deployment/target/deployment-<version>.war` into
your Tomcat instance.


The first method, running the WAR file from the command-line, using `java` only is particularly
useful if you wish use the XML services, for example, in a Microsoft Excel spreadsheet.

## Missing `mapcode-secret.properties` File

The service requires a file called `mapcode-secret.properties` to be present on the
classpath.  The properties file `mapcode-secret.properties` contains the username and password for
your MongDB database server for tracing, should you wish to use that.

If you get a start-up error complaining about a missing `mapcode-secret.properties` file,
make sure you add it to the classpath (or add it to `src/main/resources`) before building.

By default, you can simply use an empty `mapcode-secret.properties` file. So, you may want to
just create an empty file by executing:

    touch src/main/resource/mapcode-secret.properties

Note that the file `mapcode-secret.properties` is ignored by Git in `.gitignore`.

If you wish to use MongoDB tracing, will need to provide your own local
`mapcode-secret.properties`, which override the following properties:

    MongoDBTrace.writeEnabled = true
    MongoDBTrace.servers = your-server:27017 (eg. localhost:27017)
    MongoDBTrace.database = your-database (eg. trace)
    MongoDBTrace.userName = your-username
    MongoDBTrace.password = your-password

The service will work with an empty file as well, but will not trace events to the
database.


## Using Java 8 on MacOSX

The source uses Java JDK 1.8, so make sure your Java compiler is set to 1.8, for example
using something like (MacOSX):

    export JAVA_HOME=`/usr/libexec/java_home -v 1.8`


## Smoke Testing The REST API

Try out if the web services work by entering the following URL in your web browser
(this should show you a HTML help page):

    http://localhost:8080/mapcode
    http://localhost:8080/mapcode/codes/50.2,4.9

Or use a tool like cURL:

    curl -X GET http://localhost:8080/mapcode
    curl -X GET http://localhost:8080/mapcode/codes/50.2,4.9

There's also an example HTML page in the `examples/index.html` for HTML/Javascript developers.


# Using Git and `.gitignore`

It's good practice to set up a personal global `.gitignore` file on your machine which filters a number of files
on your file systems that you do not wish to submit to the Git repository. You can set up your own global
`~/.gitignore` file by executing:
`git config --global core.excludesfile ~/.gitignore`

In general, add the following file types to `~/.gitignore` (each entry should be on a separate line):
`*.com *.class *.dll *.exe *.o *.so *.log *.sql *.sqlite *.tlog *.epoch *.swp *.hprof *.hprof.index *.releaseBackup *~`

If you're using a Mac, filter:
`.DS_Store* Thumbs.db`

If you're using IntelliJ IDEA, filter:
`*.iml *.iws .idea/`

If you're using Eclips, filter:
`.classpath .project .settings .cache`

If you're using NetBeans, filter:
`nb-configuration.xml *.orig`

The local `.gitignore` file in the Git repository itself to reflect those file only that are produced by executing
regular compile, build or release commands, such as:
`target/ out/`

# Bug Reports and New Feature Requests

If you encounter any problems with this library, don't hesitate to use the `Issues` session to file your issues.
Normally, one of our developers should be able to comment on them and fix.


## Release Notes

* 2.2.3.19

    Updated to SpeedTools 3.0.21 which uses a more recent JBoss RESTEasy framework.
    
* 2.2.3.18

    Reworked responses for `/alphabets` and `/territories`, returning to older format (also easier for
    client parsing) and including a `total` attribute to indicate total number of items in list (for paging).

    Added metrics for `alphabets` and `territories` methods.
    
* 2.2.3.17

    Completely refactored the source tree to create JARs for all classes in the WAR file. This
    was needed to allow running the service from the command-line using `java` only.

    This version can be used to run the service stand-alone, without Tomcat, on a local machine.
    This allows you, for example, to use the XML services from within your own local MS Excel sheet.

* 2.2.3.15 - 2.2.3.16

    Changed JSON API response for `/alphabets` and `/territories`: these no longer return a top-level
    entity `alphabets` or `territories`; only the list of values, which is more in line with the
    rest of the API.

* 2.2.3.14

    Added help text.

    Removed Docker profile for now (might be re-added later), as it caused some issues building the
    service.

* 2.2.3.13

    Added additional XML support. You can prefix methods now with xml, to make
    the default output XML, rather than JSON. This is particularly useful if you
    need to use an XML service, but you can't specify the correct HTTP header.

    Works with Google spreadsheets and Microsoft Excel.

    In Google spreadsheets use the =IMPORTXML function, e.g.
    =IMPORTXML("http://api.mapcode.com/mapcode/xml/codes/52.376514,4.908543";"//local")

    In Microsoft Excel, use the =WEBSERVICE function.

* 2.2.3.12

    Added full XML support, next to JSON. Use the HTTP Accept header to specify
    the expected format: application/json or application/xml. (This features allows
    users of MS Excel to use the "=WEBSERVICE()" function, on Windows.)

    Added unit tests to test the REST services.

* 2.2.3.11

    Added debug mode, which prevents writing to the trace database.

* 2.2.3.10

    Fixed content type on GET operations to accept anything.

* 2.2.1.0 - 2.2.3.9

    Based on new version of the Java library. Includes high-precision codes, up to precision 8.

* 2.0.2.0

    Based on version 2.0.2 of the Java library.

* 2.0.1.0

    Based on version 2.0.1 of the Java library (which is Java 6 again, from Java 8).

    Typo in startup LOG message fixed.

    Allows to use territory names, as well as territory codes.

* 2.0.0.0

    Based version 2.0.0 of the Java library.

    Includes fixes and enhancements to data. For a complete description, read the Word file that ships with
    the C version of the library (on Github).

    This version no longer supports numeric codes for territories and alphabets.

* 1.50.3.0

    Changed property names in REST API for territories: `code` is renamed to `number`, `name` is renamed to `alphaCode`.

    The decode service now only returns local mapcode if all local mapcodes are within the same territory.

    The decode service produces a 404 if you ask for a local code exists and none exists, or multiple exist in
    different territories.

* 1.50.2.0 - 1.50.2.1

    Updated Java library for Mapcode.

* 1.50.1.3

    Added "?include=alphabet" option to always include mapcodeInAlphabet or territoryInAlphabet even if the same
    as the original (default is now these are only output if different).

* 1.50.1.0 - 1.50.1.2

    Bug fix for state IN-DD (in India).

* 1.50.0

    First release of the REST API, based on the Mapcode Java library, version 1.50.0.
