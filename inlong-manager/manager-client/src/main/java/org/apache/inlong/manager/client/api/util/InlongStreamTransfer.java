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

package org.apache.inlong.manager.client.api.util;

import com.google.common.collect.Lists;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.manager.client.api.InlongStreamConf;
import org.apache.inlong.manager.common.pojo.stream.StreamField;
import org.apache.inlong.manager.common.pojo.group.InlongGroupInfo;
import org.apache.inlong.manager.common.pojo.stream.InlongStreamFieldInfo;
import org.apache.inlong.manager.common.pojo.stream.InlongStreamInfo;
import org.apache.inlong.manager.common.util.CommonBeanUtils;

import java.util.List;
import java.util.stream.Collectors;

public class InlongStreamTransfer {

    public static InlongStreamInfo createStreamInfo(InlongStreamConf streamConf, InlongGroupInfo groupInfo) {
        InlongStreamInfo dataStreamInfo = new InlongStreamInfo();
        dataStreamInfo.setInlongGroupId(groupInfo.getInlongGroupId());
        final String streamId = "b_" + streamConf.getName();
        dataStreamInfo.setInlongStreamId(streamId);
        dataStreamInfo.setName(streamConf.getName());
        dataStreamInfo.setDataEncoding(streamConf.getCharset().name());
        if (StringUtils.isEmpty(streamConf.getTopic())) {
            dataStreamInfo.setMqResourceObj(streamId);
        } else {
            dataStreamInfo.setMqResourceObj(streamConf.getTopic());
        }
        dataStreamInfo.setSyncSend(streamConf.isStrictlyOrdered() ? 1 : 0);
        dataStreamInfo.setDataSeparator(String.valueOf(streamConf.getDataSeparator().getAsciiCode()));
        dataStreamInfo.setDescription(streamConf.getDescription());
        dataStreamInfo.setCreator(groupInfo.getCreator());
        dataStreamInfo.setDailyRecords(streamConf.getDailyRecords());
        dataStreamInfo.setDailyStorage(streamConf.getDailyStorage());
        dataStreamInfo.setPeakRecords(streamConf.getPeakRecords());
        dataStreamInfo.setHavePredefinedFields(0);
        if (CollectionUtils.isNotEmpty(streamConf.getStreamFields())) {
            dataStreamInfo.setFieldList(createStreamFields(streamConf.getStreamFields(), dataStreamInfo));
        }
        return dataStreamInfo;
    }

    public static List<InlongStreamFieldInfo> createStreamFields(
            List<StreamField> fieldList, InlongStreamInfo streamInfo) {
        if (CollectionUtils.isEmpty(fieldList)) {
            return Lists.newArrayList();
        }
        return fieldList.stream().map(field -> {
            InlongStreamFieldInfo fieldInfo = new InlongStreamFieldInfo();
            fieldInfo.setInlongStreamId(streamInfo.getInlongStreamId());
            fieldInfo.setInlongGroupId(streamInfo.getInlongGroupId());
            fieldInfo.setFieldName(field.getFieldName());
            fieldInfo.setFieldType(field.getFieldType().toString());
            fieldInfo.setFieldComment(field.getFieldComment());
            fieldInfo.setFieldValue(field.getFieldValue());
            fieldInfo.setIsMetaField(field.getIsMetaField());
            fieldInfo.setFieldFormat(field.getFieldFormat());
            return fieldInfo;
        }).collect(Collectors.toList());
    }

    public static List<StreamField> parseStreamFields(List<InlongStreamFieldInfo> fields) {
        if (CollectionUtils.isEmpty(fields)) {
            return null;
        }
        return fields.stream().map(fieldInfo -> CommonBeanUtils.copyProperties(fieldInfo, StreamField::new))
                .collect(Collectors.toList());
    }
}
