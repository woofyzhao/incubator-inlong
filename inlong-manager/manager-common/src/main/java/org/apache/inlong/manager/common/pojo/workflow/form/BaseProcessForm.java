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

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import lombok.Data;

/**
 * The main form of the process-submitted when the process is initiated
 */
@Data
@JsonTypeInfo(use = Id.NAME, property = "formName")
@JsonSubTypes({
        @JsonSubTypes.Type(value = NewGroupProcessForm.class, name = NewGroupProcessForm.FORM_NAME),
        @JsonSubTypes.Type(value = NewConsumptionProcessForm.class, name = NewConsumptionProcessForm.FORM_NAME),
        @JsonSubTypes.Type(value = GroupResourceProcessForm.class, name = GroupResourceProcessForm.FORM_NAME),
        @JsonSubTypes.Type(value = UpdateGroupProcessForm.class, name = UpdateGroupProcessForm.FORM_NAME),
        @JsonSubTypes.Type(value = LightGroupResourceProcessForm.class, name = LightGroupResourceProcessForm.FORM_NAME),
})
public abstract class BaseProcessForm implements ProcessForm {

}
