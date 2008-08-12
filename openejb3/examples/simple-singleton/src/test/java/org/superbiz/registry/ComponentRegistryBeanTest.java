/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.superbiz.registry;

import junit.framework.TestCase;

import javax.naming.Context;
import javax.naming.InitialContext;
import java.util.Properties;
import java.util.Date;
import java.net.URI;

public class ComponentRegistryBeanTest extends TestCase {

    public void test() throws Exception {
        Properties props = new Properties();
        props.setProperty(Context.INITIAL_CONTEXT_FACTORY, "org.apache.openejb.client.LocalInitialContextFactory");

        InitialContext context = new InitialContext(props);

        ComponentRegistry refOne = (ComponentRegistry) context.lookup("ComponentRegistryBeanLocal");

        ComponentRegistry refTwo = (ComponentRegistry) context.lookup("ComponentRegistryBeanLocal");


        refOne.setComponent(URI.class, new URI("foo://bar/baz"));

        URI uri = refTwo.getComponent(URI.class);

        assertEquals(uri, new URI("foo://bar/baz"));


        Date now = new Date();

        refTwo.setComponent(Date.class, now);

        Date date = refOne.getComponent(Date.class);

        assertEquals(now, date);

    }
}