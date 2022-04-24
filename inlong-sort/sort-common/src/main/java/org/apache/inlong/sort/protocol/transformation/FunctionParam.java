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

package org.apache.inlong.sort.protocol.transformation;

import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonSubTypes;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.apache.inlong.sort.protocol.BuiltInFieldInfo;
import org.apache.inlong.sort.protocol.FieldInfo;
import org.apache.inlong.sort.protocol.transformation.function.HopEndFunction;
import org.apache.inlong.sort.protocol.transformation.function.HopFunction;
import org.apache.inlong.sort.protocol.transformation.function.HopStartFunction;
import org.apache.inlong.sort.protocol.transformation.function.MultiValueFilterFunction;
import org.apache.inlong.sort.protocol.transformation.function.SessionEndFunction;
import org.apache.inlong.sort.protocol.transformation.function.SessionFunction;
import org.apache.inlong.sort.protocol.transformation.function.SessionStartFunction;
import org.apache.inlong.sort.protocol.transformation.function.SingleValueFilterFunction;
import org.apache.inlong.sort.protocol.transformation.function.SplitIndexFunction;
import org.apache.inlong.sort.protocol.transformation.function.TumbleEndFunction;
import org.apache.inlong.sort.protocol.transformation.function.TumbleFunction;
import org.apache.inlong.sort.protocol.transformation.function.TumbleStartFunction;
import org.apache.inlong.sort.protocol.transformation.operator.AndOperator;
import org.apache.inlong.sort.protocol.transformation.operator.EmptyOperator;
import org.apache.inlong.sort.protocol.transformation.operator.EqualOperator;
import org.apache.inlong.sort.protocol.transformation.operator.InOperator;
import org.apache.inlong.sort.protocol.transformation.operator.IsNotNullOperator;
import org.apache.inlong.sort.protocol.transformation.operator.IsNullOperator;
import org.apache.inlong.sort.protocol.transformation.operator.LessThanOperator;
import org.apache.inlong.sort.protocol.transformation.operator.LessThanOrEqualOperator;
import org.apache.inlong.sort.protocol.transformation.operator.MoreThanOperator;
import org.apache.inlong.sort.protocol.transformation.operator.MoreThanOrEqualOperator;
import org.apache.inlong.sort.protocol.transformation.operator.NotEqualOperator;
import org.apache.inlong.sort.protocol.transformation.operator.NotInOperator;
import org.apache.inlong.sort.protocol.transformation.operator.OrOperator;

/**
 * FunctionParam is used to define and encapsulate function parameters
 * and it is the top-level interface for function parameters.
 * It mainly includes several categories such as fields, constants, functions, operators etc
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = FieldInfo.class, name = "base"),
        @JsonSubTypes.Type(value = BuiltInFieldInfo.class, name = "builtin"),
        @JsonSubTypes.Type(value = ConstantParam.class, name = "constant"),
        @JsonSubTypes.Type(value = TimeUnitConstantParam.class, name = "timeUnitConstant"),
        @JsonSubTypes.Type(value = StringConstantParam.class, name = "stringConstant"),
        @JsonSubTypes.Type(value = AndOperator.class, name = "and"),
        @JsonSubTypes.Type(value = OrOperator.class, name = "or"),
        @JsonSubTypes.Type(value = EmptyOperator.class, name = "empty"),
        @JsonSubTypes.Type(value = EqualOperator.class, name = "equal"),
        @JsonSubTypes.Type(value = NotEqualOperator.class, name = "notEqual"),
        @JsonSubTypes.Type(value = IsNotNullOperator.class, name = "isNotNull"),
        @JsonSubTypes.Type(value = IsNullOperator.class, name = "isNull"),
        @JsonSubTypes.Type(value = LessThanOperator.class, name = "lessThan"),
        @JsonSubTypes.Type(value = LessThanOrEqualOperator.class, name = "lessThanOrEqual"),
        @JsonSubTypes.Type(value = MoreThanOperator.class, name = "moreThan"),
        @JsonSubTypes.Type(value = MoreThanOrEqualOperator.class, name = "moreThanOrEqual"),
        @JsonSubTypes.Type(value = InOperator.class, name = "in"),
        @JsonSubTypes.Type(value = NotInOperator.class, name = "notIn"),
        @JsonSubTypes.Type(value = WatermarkField.class, name = "watermark"),
        @JsonSubTypes.Type(value = HopStartFunction.class, name = "hopStart"),
        @JsonSubTypes.Type(value = HopEndFunction.class, name = "hopEnd"),
        @JsonSubTypes.Type(value = TumbleStartFunction.class, name = "tumbleStart"),
        @JsonSubTypes.Type(value = TumbleEndFunction.class, name = "tumbleEnd"),
        @JsonSubTypes.Type(value = SessionStartFunction.class, name = "sessionStart"),
        @JsonSubTypes.Type(value = SessionEndFunction.class, name = "sessionEnd"),
        @JsonSubTypes.Type(value = SessionFunction.class, name = "session"),
        @JsonSubTypes.Type(value = TumbleFunction.class, name = "tumble"),
        @JsonSubTypes.Type(value = HopFunction.class, name = "hop"),
        @JsonSubTypes.Type(value = SingleValueFilterFunction.class, name = "singleValueFilter"),
        @JsonSubTypes.Type(value = MultiValueFilterFunction.class, name = "multiValueFilter"),
        @JsonSubTypes.Type(value = SplitIndexFunction.class, name = "splitIndex")
})
public interface FunctionParam {

    /**
     * Function param name
     *
     * @return The name of this function param
     */
    @JsonIgnore
    String getName();

    /**
     * Format used for sql
     *
     * @return The format value in sql
     */
    String format();

}
