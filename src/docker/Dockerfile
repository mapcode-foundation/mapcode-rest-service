FROM tomcat:8.0.20-jre8

MAINTAINER MapCode-Foundation

ENV JAVA_TOOL_OPTIONS -Dfile.encoding=UTF8

ADD mapcode-rest-service.war /usr/local/tomcat/webapps/
ADD mapcode-secret.properties /usr/local/tomcat/lib/
ADD tomcat-users.xml /usr/local/tomcat/conf/

#RUN mkdir /usr/local/tomcat/webapps/myapp

#COPY /mapcode-foundation /usr/local/tomcat/webapps
