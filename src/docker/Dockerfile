FROM tomcat:8.0.20-jre8

MAINTAINER MapCode-Foundation

ENV JAVA_TOOL_OPTIONS -Dfile.encoding=UTF8

ADD mapcode-rest-service-2.2.3.5.war /usr/local/tomcat/webapps/
ADD mapcode-secret.properties /usr/local/tomcat/lib/

#RUN mkdir /usr/local/tomcat/webapps/myapp

#COPY /mapcode-foundation /usr/local/tomcat/webapps
