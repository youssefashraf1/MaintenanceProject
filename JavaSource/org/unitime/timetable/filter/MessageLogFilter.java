/*
 * Licensed to The Apereo Foundation under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.
 *
 * The Apereo Foundation licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
*/
package org.unitime.timetable.filter;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.logging.log4j.ThreadContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.unitime.timetable.security.UserContext;

/**
 * @author Tomas Muller
 */
public class MessageLogFilter implements Filter {
	private String iHost = null;
	
	@Override
	public void init(FilterConfig config) throws ServletException {
		try {
			iHost = InetAddress.getLocalHost().getHostName();
			if (iHost.indexOf('.') > 0)
				iHost = iHost.substring(0, iHost.indexOf('.'));
		} catch (UnknownHostException e) { 
		}
	}

	@Override
	public void destroy() {
	}
	
	private UserContext getUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof UserContext)
			return (UserContext)authentication.getPrincipal();
		return null;
	}
	
	private void populateThreadContext() {
		try {
			UserContext user = getUser();
			if (user != null) {
				ThreadContext.push("uid:"+ user.getTrueExternalUserId());
				if (user.getCurrentAuthority() != null) {
					ThreadContext.push("role:"+ user.getCurrentAuthority().getRole());
					Long sessionId = user.getCurrentAcademicSessionId();
					if (sessionId != null) {
						ThreadContext.push("sid:"+ sessionId);
					}
				}
			}
			if (iHost != null)
				ThreadContext.push("host:"+ iHost);
		} catch (Exception e) {}
	}
	
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws ServletException, IOException {
		try {
			populateThreadContext();
			
			chain.doFilter(request,response);
		} finally {
			ThreadContext.removeStack();
		}
	}

}
