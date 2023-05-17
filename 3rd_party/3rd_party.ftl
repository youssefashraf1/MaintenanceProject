<#-- 
  Licensed to The Apereo Foundation under one or more contributor license
  agreements. See the NOTICE file distributed with this work for
  additional information regarding copyright ownership.

  The Apereo Foundation licenses this file to you under the Apache License,
  Version 2.0 (the "License"); you may not use this file except in
  compliance with the License. You may obtain a copy of the License at:

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.

  See the License for the specific language governing permissions and
  limitations under the License.
  
  ----
  This template is used to generate the NOTICE file:
     mvn license:add-third-party
-->
<#function artifactFormat p>
    <#if p.name?index_of('Unnamed') &gt; -1>
        <#return p.artifactId + " (" + p.groupId + ":" + p.artifactId + ":" + p.version + ")">
    <#else>
        <#return p.name + " (" + p.groupId + ":" + p.artifactId + ":" + p.version + ")">
    </#if>
</#function>
Copyright 2015, The Apereo Foundation
This project includes software developed by The Apereo Foundation.
http://www.apereo.org/

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this software except in compliance with the License.
You may obtain a copy of the License at:

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

===========================================================================

This software originally granted to the Apereo Foundation by UniTime LLC.

===========================================================================
<#function select linceses>
	<#list licenses as license>
		<#if license == 'Common Development and Distribution License (CDDL), Version 1.0'>
			<#return license>
		<#elseif license == 'Eclipse Distribution License (EDL), Version 1.0'>
			<#return license>
		<#elseif license == 'Apache Software License (ASL), Version 2.0'>
			<#return license>
		</#if>
	</#list>
	<#return licenses[0]>
</#function>

This project includes:
<#list dependencyMap as e><#assign project = e.getKey()/><#assign licenses = e.getValue()/>

${project.name} (${project.artifactId}-${project.version}.jar)
	under ${select(licenses)}<#if project.url??>
	${project.url}</#if>
</#list>

Tomcat Migration Tool for Jakarta EE (jakartaee-migration-1.0.0.jar)
	under the Apache Software License (ASL), Version 2.0
	https://github.com/apache/tomcat-jakartaee-migration

Leaflet 1.3.1 (a JavaScript library for interactive maps)
	under BSD or BSD-style License
	https://leafletjs.com/

The famfamfam Silk Icons under Creative Commons Attribution 3.0 License
	http://www.famfamfam.com/lab/icons/silk

LED Icon Set under Creative Commons Attribution 3.0 License
	http://led24.de/iconset/

Free web development icons under Public Domain
	http://www.icojam.com/blog/?p=119

Onebit icon sets under Public Domain
	http://www.icojam.com/blog/?p=177

===========================================================================

For the appropriate license, see

Apache Software License (ASL), Version 2.0 
	http://www.apache.org/licenses/LICENSE-2.0
	http://www.gwtproject.org/terms.html (GWT)

BSD or BSD-style Licenses
	http://asm.ow2.org/license.html (ASM Core)
	http://www.antlr.org/license.html (AntLR Parser Generator)
	http://freemarker.org/docs/app_license.html (FreeMaker)
	http://opensource.org/licenses/bsd-license.php (Protocol Buffer Java API, biweekly)
	http://dom4j.sourceforge.net/dom4j-1.6.1/license.html (dom4j)
	https://github.com/Leaflet/Leaflet/blob/master/LICENSE (Leaflet)
	http://jaxen.codehaus.org/license.html (jaxen)

Common Development and Distribution License (CDDL), Version 1.0
	http://opensource.org/licenses/CDDL-1.0

Creative Commons Attribution 3.0 License
	http://creativecommons.org/licenses/by/3.0

Eclipse Public License (EPL), Version 1.0
	http://www.eclipse.org/legal/epl-v10.html

GNU Lesser General Public License (LGPL), Version 2.1
	https://www.gnu.org/licenses/old-licenses/lgpl-2.1.html

GNU Lesser General Public License (LGPL), Version 3
	https://www.gnu.org/licenses/lgpl.html

JA-SIG License for Use
	http://www.jasig.org/cas/license

Public Domain
	http://www.json.org/license.html (JSON)
	http://creativecommons.org/publicdomain/mark/1.0 (Free web development icons, Onebit icon sets)

MIT License
	http://www.slf4j.org/license.html (SLF4J)

Mozilla Public License (MPL), Version 1.1
	http://www.mozilla.org/MPL/1.1
