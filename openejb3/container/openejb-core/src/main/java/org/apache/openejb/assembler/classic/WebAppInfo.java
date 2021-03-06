/**
 *
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
package org.apache.openejb.assembler.classic;

import java.util.Set;
import java.util.TreeSet;
import java.util.List;
import java.util.ArrayList;

public class WebAppInfo extends ValidationInfoObject {
    public String path;
    public String description;
    public String displayName;
    public String smallIcon;
    public String largeIcon;
    public String moduleId;
    public String host;
    public String contextRoot;
    public final Set<String> watchedResources = new TreeSet<String>();
    public final Set<String> restClass = new TreeSet<String>();
    public final Set<String> restApplications = new TreeSet<String>();
    public final List<PortInfo> portInfos = new ArrayList<PortInfo>();
    public final JndiEncInfo jndiEnc = new JndiEncInfo();
    public final List<ServletInfo> servlets = new ArrayList<ServletInfo>();
}
