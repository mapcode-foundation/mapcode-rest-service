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
    mvn jetty:run
    
Try out if the web services work by entering the following URL in your web browser:

    http://localhost:8080/help
    
Or use a tool like cURL:
    
    curl -X GET http://localhost:8080/help
