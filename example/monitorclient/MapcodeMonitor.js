/*
 * Copyright (C) 2016 Stichting Mapcode Foundation (http://www.mapcode.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

$(document).ready(function () {
    var fInterval = 1000;

    // To use on production system:
    var jolokiaConnector = new Jolokia({ url: "http://api.mapcode.com/monitor", fetchInterval: fInterval});

    // For local testing:
    // jolokiaConnector = new Jolokia({url: "http://localhost:8080/monitor", fetchInterval: fInterval});


    var colorsRed = ["#FDBE85", "#FEEDDE", "#FD8D3C", "#E6550D", "#A63603", "#FDBE85", "#FEEDDE", "#FD8D3C", "#E6550D", "#A63603"];
    var colorsGreen = ["#FDBE85"];
    var colorsBlue = ["#ECE7F2", "#A6BDDB", "#2B8CBE", "#ECE7F2", "#A6BDDB", "#2B8CBE"];

    var context = cubism.context()
        .serverDelay(0)
        .clientDelay(0)
        .step(fInterval)
        .size(400);
    var jolokia = context.jolokia(jolokiaConnector);


    function metric(metricName, path, label) {
        return jolokia.metric({
            type: 'read',
            mbean: 'mapcode:name=SystemMetrics',
            attribute: metricName,
            path: path
        }, label);
    }

    function graph(selector, title, metrics) {
        return d3.select(selector).call(function (div) {
            div.append("h2").text(title);
            div.selectAll(".horizon")
                .data(metrics)
                .enter()
                .append("div")
                .attr("class", "horizon")
                .call(context.horizon()
                    .colors(colorsBlue)
                    .format(d3.format("10d")));
            div.append("div")
                .attr("class", "axis")
                .attr("style", "fill: white")
                .call(context.axis().orient("bottom"));

            div.append("div")
                .attr("class", "rule")
                .call(context.rule());

        });
    }

    function stdgraph(selector, title, metricName, pathPostfix) {
        return graph(selector, title, [
            metric(metricName, "lastMinute/" + pathPostfix, "Last Minute"),
            metric(metricName, "lastHour/" + pathPostfix, "Last Hour"),
            metric(metricName, "lastDay/" + pathPostfix, "Last Day"),
            metric(metricName, "lastWeek/" + pathPostfix, "Last Week"),
            metric(metricName, "lastMonth/" + pathPostfix, "Last Month")]);
    }

    stdgraph("#totalMapcodeToLatLonRequestsSum", "Total requests: mapcode > coord", "TotalMapcodeToLatLonRequests", "sum");
    stdgraph("#validMapcodeToLatLonRequestsSum", "Valid requests: mapcode > coord", "ValidMapcodeToLatLonRequests", "sum");
    stdgraph("#totalLatLonToMapcodeRequestsSum", "Total requests: coord > mapcode", "TotalLatLonToMapcodeRequests", "sum");
    stdgraph("#validLatLonToMapcodeRequestsSum", "Valid requests: coord > mapcode", "ValidLatLonToMapcodeRequests", "sum");
    stdgraph("#warningsAndErrorsSum", "Warnings and Errors", "WarningsAndErrors", "sum");

    var value = jolokiaConnector.getAttribute("java.lang:type=Memory", "HeapMemoryUsage", "used") / (1024 * 1024);
    $("#memoryUsage").text("Memory used: " + Math.round(value) + "Mb");
});
