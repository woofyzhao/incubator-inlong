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

package org.apache.inlong.manager.workflow.event.task;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.inlong.manager.dao.mapper.WorkflowEventLogEntityMapper;
import org.apache.inlong.manager.workflow.WorkflowContext;
import org.apache.inlong.manager.workflow.definition.WorkflowTask;
import org.apache.inlong.manager.workflow.event.EventListenerManager;
import org.apache.inlong.manager.workflow.event.EventListenerNotifier;
import org.apache.inlong.manager.workflow.event.LogableEventListener;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * WorkflowProcess event notifier
 */
@Slf4j
public class TaskEventNotifier implements EventListenerNotifier<TaskEvent> {

    private final ExecutorService executorService = new ThreadPoolExecutor(
            20,
            20,
            0L,
            TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(),
            new ThreadFactoryBuilder().setNameFormat("async-task-event-notifier-%s").build(),
            new CallerRunsPolicy());

    private final EventListenerManager<TaskEvent, TaskEventListener> eventListenerManager;
    private final WorkflowEventLogEntityMapper eventLogMapper;

    public TaskEventNotifier(TaskEventListenerManager listenerManager, WorkflowEventLogEntityMapper eventLogMapper) {
        this.eventListenerManager = listenerManager;
        this.eventLogMapper = eventLogMapper;
    }

    public void trace(WorkflowTask task, TaskEvent event, String source, List<TaskEventListener> listeners) {
        log.info("==> executing notified listeners for task {}, event {}, listener source {}, num {}",
                task.getName(), event, source, listeners.size());
        listeners.forEach(l -> log.info("To execute: {} listening on {}", l.name(), l.event()));
    }

    @Override
    public void notify(TaskEvent event, WorkflowContext sourceContext) {
        final WorkflowContext context = sourceContext.clone();
        WorkflowTask task = (WorkflowTask) context.getCurrentElement();

        trace(task, event, "eventListenerManager.syncListeners", eventListenerManager.syncListeners(event));
        eventListenerManager.syncListeners(event).forEach(syncLogableNotify(context));

        trace(task, event, "task.syncListeners", task.syncListeners(event));
        task.syncListeners(event).forEach(syncLogableNotify(context));

        trace(task, event, "eventListenerManager.asyncListeners", eventListenerManager.asyncListeners(event));
        eventListenerManager.asyncListeners(event).forEach(asyncLogableNotify(context));

        trace(task, event, "task.asyncListeners", task.asyncListeners(event));
        task.asyncListeners(event).forEach(asyncLogableNotify(context));
    }

    @Override
    public void notify(String listenerName, boolean forceSync, WorkflowContext sourceContext) {
        final WorkflowContext context = sourceContext.clone();

        log.info("==> listener {} notified by name, forceSync = {}, context = {}", listenerName, forceSync, context);

        Optional.ofNullable(this.eventListenerManager.listener(listenerName))
                .ifPresent(logableNotify(forceSync, context));

        WorkflowTask task = (WorkflowTask) context.getCurrentElement();
        Optional.ofNullable(task.listener(listenerName))
                .ifPresent(logableNotify(forceSync, context));
    }

    private Consumer<TaskEventListener> logableNotify(boolean forceSync, WorkflowContext context) {
        return listener -> {
            if (forceSync || !listener.async()) {
                syncLogableNotify(context).accept(listener);
                return;
            }

            asyncLogableNotify(context).accept(listener);
        };
    }

    private Consumer<TaskEventListener> asyncLogableNotify(WorkflowContext context) {
        return listener -> executorService.execute(() -> {
            try {
                log.info("==> start running async listener {} on {}", listener.name(), listener.event());
                logableEventListener(listener).listen(context);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private Consumer<TaskEventListener> syncLogableNotify(WorkflowContext context) {
        return listener -> {
            log.info("==> start running sync listener {} on {}", listener.name(), listener.event());
            logableEventListener(listener).listen(context);
            return;
        };
    }

    private LogableTaskEventListener logableEventListener(TaskEventListener listener) {
        if (listener instanceof LogableEventListener) {
            return (LogableTaskEventListener) listener;
        }
        return new LogableTaskEventListener(listener, eventLogMapper);
    }

}
