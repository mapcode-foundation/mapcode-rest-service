The directory `swagger` contains a distribution of the Swagger web site files.
Visit http://swagger.io for more information on where to obtain the latest 
version of these files.

The contents of the directory `swagger` can be copied to a directory on
a web server.

Search for this line in `index.html` and change it to the correct base URI
of your REST API web services:

    url: "http://api.mapcode.com/swagger.json",

The fiel `swagger.json` contains the generated REST API definitions.
You can build this file, simply by executing:

    mvn compile
    
      