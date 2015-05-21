# README for Mapcode REST API Web Services 
 
Copyright (C) 2014-2015 Stichting Mapcode Foundation (http://www.mapcode.com)

This application provide a REST API for mapcodes. 
Available methods:
    
    GET /mapcode         Returns this help page.
    GET /mapcode/version Returns the software version.
    GET /mapcode/metrics Returns some system metrics (also available from JMX).
    GET /mapcode/status  Returns 200 if the service OK.
    
    GET /mapcode/codes/{lat},{lon}[/[mapcodes|local|international]]
         [?precision=[0|1|2] & territory={restrictToTerritory} & alphabet={alphabet} & include={offset|territory}]
    
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
       
To build and run the REST API, type:

    mvn clean install
    mvn jetty:run           (alternatively, you can use: mvn tomcat7:run)
    
Try out if the web services work by entering the following URL in your web browser
(this should show you a HTML help page):

    http://localhost:8080/mapcode
    
Or use a tool like cURL:
    
    curl -X GET http://localhost:8080/mapcode
    

There's also an example HTML page in the examples/index.html for HTML/Javascript developers. 

The source uses Java JDK 1.8, so make sure your Java compiler is set to 1.8, for example
using something like (MacOSX):

    export JAVA_HOME=`/usr/libexec/java_home -v 1.8`
