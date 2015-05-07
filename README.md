# README for Mapcode REST API Web Services 
 
Copyright (C) 2014-2015 Stichting Mapcode Foundation (http://www.mapcode.com)

This application provide a REST API for mapcodes. 
Available methods:

    GET /mapcode/to/{lat}/{lon}[?type=[all|shortest]&precision=[0|1|2]&territory={code}
       Convert a latitude/longitude pair to a mapcode.
       lat       : latitude, range [-90, 90]
       lon       : longitude, range [-180, 180]
       type      : all      = return all possible mapcodes
                   shortest = return shortest mapcode only (default)
       precision : precision, range [0, 2] (default=0)
       territory : numeric or alpha territory code

    GET /mapcode/from/{mapcode}[?territory={code}]
       Convert a mapcode into a latitude/longitude pair
       territory : numeric or alpha territory code

    GET /mapcode/territory
       Return a list of all valid numeric and alpha territory codes.

    GET /mapcode/territory/{code}
       Return information for a specific territory code.
       territory : numeric or alpha territory code

To build and run the REST API, type:

    mvn clean install
    mvn jetty:run           (alternatively, you can use: mvn tomcat7:run)
    
Try out if the web services work by entering the following URL in your web browser
(this should show you a HTML help page):

    http://localhost:8080/mapcode
    
Or use a tool like cURL:
    
    curl -X GET http://localhost:8080/mapcode
    
The source uses Java JDK 1.8, so make sure your Java compiler is set to 1.8, for example
using something like (MacOSX):

    export JAVA_HOME=`/usr/libexec/java_home -v 1.8`
