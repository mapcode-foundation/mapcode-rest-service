# README for Mapcode REST API Web Services 
 
This application provide a REST API for mapcodes. It is currently under construction.

To build and run the REST API, type:

    mvn clean install
    mvn jetty:run
    
Try out if the web services work by entering the following URL in your web browser:

    http://localhost:8080/help
    
Or use a tool like cURL:
    
    curl -X GET http://localhost:8080/help
