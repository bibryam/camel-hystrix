/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.component.hystrix;

import org.apache.camel.*;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.SimpleRegistry;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;

public class HystrixComponentTest extends CamelTestSupport {

    @Produce(uri = "direct:start")
    protected ProducerTemplate template;

    @EndpointInject(uri = "mock:result")
    protected MockEndpoint resultEndpoint;

    @EndpointInject(uri = "mock:error")
    protected MockEndpoint errorEndpoint;

    @Override
    protected CamelContext createCamelContext() throws Exception {
        SimpleRegistry registry = new SimpleRegistry();
        CamelContext context = new DefaultCamelContext(registry);
        registry.put("run", context.getEndpoint("direct:run"));
        registry.put("fallback", context.getEndpoint("direct:fallback"));
        return context;
    }

    @Test
    public void executesRunEndpoint() throws Exception {
        resultEndpoint.expectedMessageCount(1);

        template.sendBody("successful message body");

        resultEndpoint.assertIsSatisfied();
    }

    @Test
    public void executesFallbackEndpoint() throws Exception {
        errorEndpoint.expectedMessageCount(1);
        resultEndpoint.whenAnyExchangeReceived(new Processor() {
            @Override
            public void process(Exchange exchange) throws Exception {
                throw new RuntimeException("blow");
            }
        });

        template.sendBody("failing message body");
        errorEndpoint.assertIsSatisfied();
    }

    @Override
    protected RouteBuilder createRouteBuilder() {
        return new RouteBuilder() {

            public void configure() {
                from("direct:run")
                        .to("mock:result");

                from("direct:fallback")
                        .to("mock:error");

                from("direct:start")
                        .to("hystrix:testKey?runEndpointId=run&fallbackEndpointId=fallback");
            }
        };
    }
}

