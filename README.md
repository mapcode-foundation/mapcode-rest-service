# README for Mapcode REST API Web Services 
 
Copyright (C) 2014-2015 Stichting Mapcode Foundation (http://www.mapcode.com)

This application provide a REST API for mapcodes. 
Available methods:
    
    GET /mapcode         Returns this help page.
    GET /mapcode/version Returns the software version.
    GET /mapcode/metrics Returns some system metrics (also available from JMX).
    GET /mapcode/status  Returns 200 if the service OK.
    
    GET /mapcode/codes/{lat},{lon}[/[mapcodes|local|international]]
         [?precision=[0|1|2] & territory={restrictToTerritory} & alphabet={alphabet} & include={offset|territory|alphabet}]
    
       Convert latitude/longitude to one or more mapcodes.
       Path parameters:
         lat             : latitude, range [-90, 90]
         lon             : longitude, range [-180, 180] (mapped if outside range)
    
       An additional filter can be specified to limit the results:
         all             : same as without specifying a filter, returns all mapcodes
         local           : return the shortest local mapcode
         international   : return the shortest international mapcode
    
       Query parameters:
         precision       : precision, range [0, 2] (default=0)
         territory       : territory to restrict results to, numeric or alpha code
         alphabet        : Alphabet to return results in, numeric or alpha code
         include         : Multiple options may be set, separated by comma's:
                           offset    = include offset from mapcode center to lat/lon (in meters)
                           territory = always include territory in result, also for territory 'AAA'
                           alphabet  = always include mapcodeInAlphabet, even if the same as mapcode
    
    GET /mapcode/coords/{code} [?territory={mapcodeTerritory}]
       Convert a mapcode into a latitude/longitude pair
    
       Path parameters:
         code            : mapcode code (local or international)
       Query parameters:
         territory       : mapcode territory, numeric or alpha code
    
    GET /mapcode/territories [?offset={offset}&count={count}]
       Return a list of all territories.
    
    GET /mapcode/territories/{territory} [?context={territoryContext}]
       Return information for a single territory code.
    
       Path parameters:
         territory       : territory to get info for, numeric or alpha code
    
       Query parameters:
         territoryContext: territory context (optional, for disambiguation)
    
    GET /mapcode/alphabets [?offset={offset}&count={count}]
       Return a list of all alphabet codes.
    
    GET /mapcode/alphabets/{alphabet}
       Return information for a specific alphabet.
    
       Path parameters:
         alphabet        : alphabet to get info for, numeric or alpha code
    
    General query parameters for methods which return a list of results:
    
       offset            : return list from 'offset' (negative value start counting from end)
       count             : return 'count' items at most

       
## Building The REST API Service

To build and run the REST API, type:

    mvn clean install
    mvn jetty:run           (alternatively, you can use: mvn tomcat7:run)

Or deploy the WAR file (in `target`) on in your Tomcat instance.

If you wish to use MongoDB tracing, will need to provide your own local 
*secret* properties file, called `mapcode-secret.properties`, for example
in `src/main/resources` which override the following properties:

    MongoDBTrace.writeEnabled = true
    MongoDBTrace.servers = your-server:27017
    MongoDBTrace.database = your-database
    MongoDBTrace.userName = your-username
    MongoDBTrace.password = your-password
 
The service will work without this, but will not trace events to the
database.

The source uses Java JDK 1.8, so make sure your Java compiler is set to 1.8, for example
using something like (MacOSX):

    export JAVA_HOME=`/usr/libexec/java_home -v 1.8`


## Smoke Testing The REST API

Try out if the web services work by entering the following URL in your web browser
(this should show you a HTML help page):

    http://localhost:8080/mapcode
    http://localhost:8080/mapcode/codes/50,5
    
Or use a tool like cURL:
    
    curl -X GET http://localhost:8080/mapcode
    curl -X GET http://localhost:8080/mapcode/codes/50,5
    

There's also an example HTML page in the `examples/index.html` for HTML/Javascript developers. 


## Release Notes

* 1.50.1.3

    Added "?include=alphabet" option to always include mapcodeInAlphabet or territoryInAlphabet even if the same
    as the original (default is now these are only output if different).
    
* 1.50.1.0 - 1.50.1.2

    Bug fix for state IN-DD (in India).
    
* 1.50.0

    First release of the REST API, based on the Mapcode Java library, version 1.50.0.
