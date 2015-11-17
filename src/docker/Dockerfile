# Copyright (C) 2015 Stichting Mapcode Foundation (http://www.mapcode.com)
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

FROM tomcat:8.0.20-jre8

MAINTAINER Mapcode-Foundation

ENV JAVA_TOOL_OPTIONS -Dfile.encoding=UTF8

ADD mapcode-rest-service.war    /usr/local/tomcat/webapps/
ADD mapcode-secret.properties   /usr/local/tomcat/lib/
ADD tomcat-users.xml            /usr/local/tomcat/conf/

# RUN mkdir /usr/local/tomcat/webapps/myapp

# COPY /mapcode-foundation /usr/local/tomcat/webapps
