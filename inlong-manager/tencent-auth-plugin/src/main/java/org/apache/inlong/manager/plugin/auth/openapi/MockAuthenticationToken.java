/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.manager.plugin.auth.openapi;

import org.apache.inlong.manager.plugin.common.enums.AuthenticationType;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;

/**
 * Mock authentication token
 */
public class MockAuthenticationToken implements BasicAuthenticationToken {

    public static final String HEADER_AUTHENTICATION = "authentication";

    private String username;

    public MockAuthenticationToken(HttpServletRequest httpServletRequest) {
        username = httpServletRequest.getHeader(HEADER_AUTHENTICATION);
        if (username == null) {
            username = httpServletRequest.getParameter(HEADER_AUTHENTICATION);
        }
    }

    public MockAuthenticationToken(String username) {
        this.username = username;
    }

    public boolean isEmpty() {
        return StringUtils.isEmpty(username);
    }

    @Override
    public Object getPrincipal() {
        return username;
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public AuthenticationType getType() {
        return AuthenticationType.MOCK;
    }
}
