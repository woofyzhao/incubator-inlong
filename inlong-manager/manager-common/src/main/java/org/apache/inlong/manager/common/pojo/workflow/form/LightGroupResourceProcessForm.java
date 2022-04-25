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

package org.apache.inlong.manager.common.pojo.workflow.form;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.enums.GroupOperateType;
import org.apache.inlong.manager.common.exceptions.FormValidateException;
import org.apache.inlong.manager.common.pojo.group.InlongGroupInfo;
import org.apache.inlong.manager.common.pojo.stream.InlongStreamInfo;
import org.apache.inlong.manager.common.util.Preconditions;

import java.util.List;

/**
 * Form of create lightweight inlong group resource
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class LightGroupResourceProcessForm extends BaseProcessForm {

    public static final String FORM_NAME = "LightGroupResourceProcessForm";

    private InlongGroupInfo groupInfo;

    private List<InlongStreamInfo> streamInfos;

    private GroupOperateType groupOperateType = GroupOperateType.INIT;

    @Override
    public void validate() throws FormValidateException {
        Preconditions.checkNotNull(groupInfo, ErrorCodeEnum.GROUP_NOT_FOUND.getMessage());
    }

    @Override
    public String getFormName() {
        return FORM_NAME;
    }

    @Override
    public String getInlongGroupId() {
        return groupInfo.getInlongGroupId();
    }
}
