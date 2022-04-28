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

package org.apache.inlong.manager.common.pojo.commonserver;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.inlong.manager.common.beans.PageRequest;

/**
 * DB source query conditions
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel("DB source query conditions")
public class CommonDbServerPageRequest extends PageRequest {

    @ApiModelProperty(value = "DB Server IP")
    private String dbServerIp;

    @ApiModelProperty(value = "current user", hidden = true)
    private String currentUser;

    @ApiModelProperty(value = "Weather current user have admin role", hidden = true)
    private Boolean isAdminRole = false;

    @ApiModelProperty(value = "The group the current user belongs to")
    private List<String> userGroups;
}
