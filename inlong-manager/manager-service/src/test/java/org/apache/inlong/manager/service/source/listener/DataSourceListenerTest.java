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

package org.apache.inlong.manager.service.source.listener;

import org.apache.inlong.manager.common.enums.GroupOperateType;
import org.apache.inlong.manager.common.enums.GroupStatus;
import org.apache.inlong.manager.common.enums.ProcessStatus;
import org.apache.inlong.manager.common.enums.SourceStatus;
import org.apache.inlong.manager.common.enums.SourceType;
import org.apache.inlong.manager.common.pojo.group.InlongGroupInfo;
import org.apache.inlong.manager.common.pojo.source.SourceResponse;
import org.apache.inlong.manager.common.pojo.source.binlog.BinlogSourceRequest;
import org.apache.inlong.manager.common.pojo.stream.InlongStreamInfo;
import org.apache.inlong.manager.common.pojo.workflow.ProcessResponse;
import org.apache.inlong.manager.common.pojo.workflow.WorkflowResult;
import org.apache.inlong.manager.common.pojo.workflow.form.UpdateGroupProcessForm;
import org.apache.inlong.manager.service.source.StreamSourceService;
import org.apache.inlong.manager.service.workflow.ProcessName;
import org.apache.inlong.manager.service.workflow.WorkflowServiceImplTest;
import org.apache.inlong.manager.workflow.WorkflowContext;
import org.apache.inlong.manager.workflow.definition.ServiceTask;
import org.apache.inlong.manager.workflow.definition.WorkflowProcess;
import org.apache.inlong.manager.workflow.definition.WorkflowTask;
import org.apache.inlong.manager.workflow.util.WorkflowBeanUtils;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class DataSourceListenerTest extends WorkflowServiceImplTest {

    public UpdateGroupProcessForm form;

    public InlongGroupInfo groupInfo;

    @Autowired
    private StreamSourceService streamSourceService;

    public Integer createBinlogSource(InlongGroupInfo groupInfo) {
        final InlongStreamInfo stream = createStreamInfo(groupInfo);
        BinlogSourceRequest sourceRequest = new BinlogSourceRequest();
        sourceRequest.setInlongGroupId(stream.getInlongGroupId());
        sourceRequest.setInlongStreamId(stream.getInlongStreamId());
        sourceRequest.setSourceName("binlog-collect");
        return streamSourceService.save(sourceRequest, OPERATOR);
    }

    @Test
    public void testFrozenSource() {
        groupInfo = initGroupForm("PULSAR");
        groupService.updateStatus(GROUP_ID, GroupStatus.CONFIG_SUCCESSFUL.getCode(), OPERATOR);
        groupService.update(groupInfo.genRequest(), OPERATOR);

        final int sourceId = createBinlogSource(groupInfo);
        streamSourceService.updateStatus(groupInfo.getInlongGroupId(), null,
                SourceStatus.SOURCE_NORMAL.getCode(), OPERATOR);

        form = new UpdateGroupProcessForm();
        form.setGroupInfo(groupInfo);
        form.setGroupOperateType(GroupOperateType.SUSPEND);
        WorkflowContext context = workflowEngine.processService()
                .start(ProcessName.SUSPEND_GROUP_PROCESS.name(), applicant, form);
        WorkflowResult result = WorkflowBeanUtils.result(context);
        ProcessResponse response = result.getProcessInfo();
        Assert.assertSame(response.getStatus(), ProcessStatus.COMPLETED);

        WorkflowProcess process = context.getProcess();
        WorkflowTask task = process.getTaskByName("stopSource");
        Assert.assertTrue(task instanceof ServiceTask);
        SourceResponse sourceResponse = streamSourceService.get(sourceId, SourceType.BINLOG.toString());
        Assert.assertSame(SourceStatus.forCode(sourceResponse.getStatus()), SourceStatus.TO_BE_ISSUED_FROZEN);
    }

    @Test
    public void testRestartSource() {
        // testFrozenSource();
        groupInfo = initGroupForm("PULSAR");
        groupService.updateStatus(GROUP_ID, GroupStatus.CONFIG_SUCCESSFUL.getCode(), OPERATOR);
        groupService.update(groupInfo.genRequest(), OPERATOR);
        groupService.updateStatus(GROUP_ID, GroupStatus.SUSPENDED.getCode(), OPERATOR);
        groupService.update(groupInfo.genRequest(), OPERATOR);

        final int sourceId = createBinlogSource(groupInfo);
        streamSourceService.updateStatus(groupInfo.getInlongGroupId(), null,
                SourceStatus.SOURCE_NORMAL.getCode(), OPERATOR);

        form = new UpdateGroupProcessForm();
        form.setGroupInfo(groupInfo);
        form.setGroupOperateType(GroupOperateType.RESTART);
        WorkflowContext context = workflowEngine.processService()
                .start(ProcessName.RESTART_GROUP_PROCESS.name(), applicant, form);
        WorkflowResult result = WorkflowBeanUtils.result(context);
        ProcessResponse response = result.getProcessInfo();
        Assert.assertSame(response.getStatus(), ProcessStatus.COMPLETED);

        WorkflowProcess process = context.getProcess();
        WorkflowTask task = process.getTaskByName("restartSource");
        Assert.assertTrue(task instanceof ServiceTask);
        SourceResponse sourceResponse = streamSourceService.get(sourceId, SourceType.BINLOG.toString());
        Assert.assertSame(SourceStatus.forCode(sourceResponse.getStatus()), SourceStatus.TO_BE_ISSUED_ACTIVE);
    }

}
