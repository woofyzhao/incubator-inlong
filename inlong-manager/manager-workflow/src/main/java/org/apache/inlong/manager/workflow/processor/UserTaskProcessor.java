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

package org.apache.inlong.manager.workflow.processor;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.manager.common.enums.TaskStatus;
import org.apache.inlong.manager.common.exceptions.WorkflowException;
import org.apache.inlong.manager.common.util.JsonUtils;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.dao.entity.WorkflowProcessEntity;
import org.apache.inlong.manager.dao.entity.WorkflowTaskEntity;
import org.apache.inlong.manager.dao.mapper.WorkflowTaskEntityMapper;
import org.apache.inlong.manager.workflow.WorkflowAction;
import org.apache.inlong.manager.workflow.WorkflowContext;
import org.apache.inlong.manager.workflow.core.impl.WorkflowEventNotifier;
import org.apache.inlong.manager.workflow.definition.Element;
import org.apache.inlong.manager.workflow.definition.UserTask;
import org.apache.inlong.manager.workflow.event.task.TaskEvent;
import org.apache.inlong.manager.workflow.event.task.TaskEventNotifier;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * User task processor
 */
@Slf4j
public class UserTaskProcessor extends AbstractTaskProcessor<UserTask> {

    private static final Set<WorkflowAction> SHOULD_CHECK_OPERATOR_ACTIONS = ImmutableSet
            .of(WorkflowAction.APPROVE, WorkflowAction.REJECT, WorkflowAction.TRANSFER);

    private static final Set<WorkflowAction> SUPPORT_ACTIONS = ImmutableSet.of(
            WorkflowAction.APPROVE, WorkflowAction.REJECT, WorkflowAction.TRANSFER, WorkflowAction.CANCEL,
            WorkflowAction.TERMINATE
    );
    private final TaskEventNotifier taskEventNotifier;

    public UserTaskProcessor(WorkflowTaskEntityMapper taskEntityMapper, WorkflowEventNotifier eventNotifier) {
        super(taskEntityMapper);
        this.taskEventNotifier = eventNotifier.getTaskEventNotifier();
    }

    @Override
    public Class<UserTask> watch() {
        return UserTask.class;
    }

    @Override
    public void create(UserTask userTask, WorkflowContext context) {
        List<String> approvers = userTask.getApproverAssign().assign(context);
        log.info("==> create user task {}, approvers = {}", userTask, approvers);
        Preconditions.checkNotEmpty(approvers, "cannot assign approvers for task: " + userTask.getDisplayName()
                + ", as the approvers was empty");

        if (!userTask.isNeedAllApprove()) {
            log.info("==> need not all to approve, coalesce approvers");
            approvers = Collections.singletonList(StringUtils.join(approvers, WorkflowTaskEntity.APPROVERS_DELIMITER));
        }

        WorkflowProcessEntity processEntity = context.getProcessEntity();
        approvers.stream()
                .map(approver -> {
                    WorkflowTaskEntity entity = saveTaskEntity(userTask, processEntity, approver);
                    log.info("==> task entity saved: {}", entity);
                    return entity;
                }).forEach(context.getNewTaskList()::add);

        taskEventNotifier.notify(TaskEvent.CREATE, context);
    }

    @Override
    public boolean pendingForAction(WorkflowContext context) {
        return true;
    }

    @Override
    public boolean complete(WorkflowContext context) {
        WorkflowContext.ActionContext actionContext = context.getActionContext();
        Preconditions.checkTrue(SUPPORT_ACTIONS.contains(actionContext.getAction()),
                "UserTask not support action:" + actionContext.getAction());

        WorkflowTaskEntity workflowTaskEntity = actionContext.getTaskEntity();
        Preconditions.checkTrue(TaskStatus.PENDING.name().equalsIgnoreCase(workflowTaskEntity.getStatus()),
                "task status should be pending");

        log.info("==> user task {} complete", workflowTaskEntity.getName());
        checkOperator(actionContext);
        log.info("==> check operator ok");
        completeTaskInstance(actionContext);

        log.info("==> notify task event {}", toTaskEvent(actionContext.getAction()));
        this.taskEventNotifier.notify(toTaskEvent(actionContext.getAction()), context);
        return true;
    }

    @Override
    public List<Element> next(UserTask userTask, WorkflowContext context) {
        WorkflowContext.ActionContext actionContext = context.getActionContext();
        if (userTask.isNeedAllApprove()) {
            WorkflowTaskEntity workflowTaskEntity = actionContext.getTaskEntity();
            int pendingCount = taskEntityMapper.countByStatus(workflowTaskEntity.getProcessId(),
                    workflowTaskEntity.getName(), TaskStatus.PENDING);

            if (pendingCount > 0) {
                log.warn("==> user task {} not approved by all approvers, pendingCount = {}", userTask.getName(),
                        pendingCount);
                return Lists.newArrayList();
            }
            log.info("==> user task {} approved by all approvers", userTask.getName());
        }

        return super.next(userTask, context);
    }

    private WorkflowTaskEntity saveTaskEntity(UserTask task, WorkflowProcessEntity processEntity, String approvers) {
        WorkflowTaskEntity taskEntity = new WorkflowTaskEntity();

        taskEntity.setType(UserTask.class.getSimpleName());
        taskEntity.setProcessId(processEntity.getId());
        taskEntity.setProcessName(processEntity.getName());
        taskEntity.setProcessDisplayName(processEntity.getDisplayName());
        taskEntity.setApplicant(processEntity.getApplicant());
        taskEntity.setName(task.getName());
        taskEntity.setDisplayName(task.getDisplayName());
        taskEntity.setApprovers(approvers);
        taskEntity.setStatus(TaskStatus.PENDING.name());
        taskEntity.setStartTime(new Date());

        taskEntityMapper.insert(taskEntity);
        Preconditions.checkNotNull(taskEntity.getId(), "task saved failed");
        return taskEntity;
    }

    private void checkOperator(WorkflowContext.ActionContext actionContext) {
        WorkflowTaskEntity workflowTaskEntity = actionContext.getTaskEntity();
        if (!SHOULD_CHECK_OPERATOR_ACTIONS.contains(actionContext.getAction())) {
            return;
        }

        boolean operatorIsApprover = ArrayUtils.contains(
                workflowTaskEntity.getApprovers().split(WorkflowTaskEntity.APPROVERS_DELIMITER),
                actionContext.getOperator()
        );

        if (!operatorIsApprover) {
            throw new WorkflowException(
                    String.format("current operator %s not in approvers list: %s", actionContext.getOperator(),
                            workflowTaskEntity.getApprovers()));
        }
    }

    private void completeTaskInstance(WorkflowContext.ActionContext actionContext) {
        WorkflowTaskEntity taskEntity = actionContext.getTaskEntity();

        TaskStatus taskStatus = toTaskState(actionContext.getAction());
        taskEntity.setStatus(taskStatus.name());
        taskEntity.setOperator(actionContext.getOperator());
        taskEntity.setRemark(actionContext.getRemark());

        UserTask userTask = (UserTask) actionContext.getTask();
        if (needForm(userTask, actionContext.getAction())) {
            Preconditions.checkNotNull(actionContext.getForm(), "form cannot be null");
            Preconditions.checkTrue(actionContext.getForm().getClass().isAssignableFrom(userTask.getFormClass()),
                    "form type not match, should be class " + userTask.getFormClass());
            actionContext.getForm().validate();
            taskEntity.setFormData(JsonUtils.toJson(actionContext.getForm()));
        } else {
            Preconditions.checkNull(actionContext.getForm(), "no form required");
        }
        taskEntity.setEndTime(new Date());
        taskEntity.setExtParams(handlerExt(actionContext, taskEntity.getExtParams()));
        taskEntityMapper.update(taskEntity);
        log.info("==> task entity updated: {}", taskEntity);
    }

    private boolean needForm(UserTask userTask, WorkflowAction workflowAction) {
        if (userTask.getFormClass() == null) {
            return false;
        }

        return WorkflowAction.APPROVE.equals(workflowAction) || WorkflowAction.COMPLETE.equals(workflowAction);
    }

    private String handlerExt(WorkflowContext.ActionContext actionContext, String oldExt) {
        Map<String, Object> extMap = Optional.ofNullable(oldExt)
                .map(e -> JsonUtils.parseMap(oldExt, String.class, Object.class))
                .orElseGet(Maps::newHashMap);

        if (WorkflowAction.TRANSFER.equals(actionContext.getAction())) {
            extMap.put(WorkflowTaskEntity.EXT_TRANSFER_USER_KEY, actionContext.getTransferToUsers());
        }

        return JsonUtils.toJson(extMap);
    }

    private TaskStatus toTaskState(WorkflowAction workflowAction) {
        switch (workflowAction) {
            case APPROVE:
                return TaskStatus.APPROVED;
            case REJECT:
                return TaskStatus.REJECTED;
            case CANCEL:
                return TaskStatus.CANCELED;
            case TRANSFER:
                return TaskStatus.TRANSFERRED;
            case TERMINATE:
                return TaskStatus.TERMINATED;
            default:
                throw new WorkflowException("unknown workflowAction " + this);
        }
    }

    private TaskEvent toTaskEvent(WorkflowAction workflowAction) {
        switch (workflowAction) {
            case APPROVE:
                return TaskEvent.APPROVE;
            case REJECT:
                return TaskEvent.REJECT;
            case CANCEL:
                return TaskEvent.CANCEL;
            case TRANSFER:
                return TaskEvent.TRANSFER;
            case TERMINATE:
                return TaskEvent.TERMINATE;
            default:
                throw new WorkflowException("unknown workflow action " + this);
        }
    }

}
