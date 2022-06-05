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

package org.apache.inlong.manager.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.inlong.manager.client.api.ClientConfiguration;
import org.apache.inlong.manager.client.api.InlongClient;
import org.apache.inlong.manager.client.api.InlongGroup;
import org.apache.inlong.manager.client.api.InlongGroupContext;
import org.apache.inlong.manager.client.api.InlongStreamBuilder;
import org.apache.inlong.manager.common.enums.FieldType;
import org.apache.inlong.manager.common.pojo.group.InlongGroupInfo;
import org.apache.inlong.manager.common.pojo.sink.SinkField;
import org.apache.inlong.manager.common.pojo.sink.hbase.HBaseColumnFamilyInfo;
import org.apache.inlong.manager.common.pojo.sink.hbase.HBaseSink;
import org.apache.inlong.manager.common.pojo.source.file.FileSource;
import org.apache.inlong.manager.common.pojo.stream.StreamField;
import org.apache.shiro.util.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Test class for file to hbase.
 */
@Slf4j
public class File2HBaseExample extends BaseExample {

    @Test
    public void testCreateGroupForHBase() {
        ClientConfiguration configuration = new ClientConfiguration();
        configuration.setWriteTimeout(10);
        configuration.setReadTimeout(10);
        configuration.setConnectTimeout(10);
        configuration.setTimeUnit(TimeUnit.SECONDS);
        configuration.setAuthentication(super.getInlongAuth());
        InlongClient inlongClient = InlongClient.create(super.getServiceUrl(), configuration);

        InlongGroupInfo groupInfo = super.createGroupInfo();
        try {
            InlongGroup group = inlongClient.forGroup(groupInfo);
            InlongStreamBuilder streamBuilder = group.createStream(createStreamInfo());
            streamBuilder.fields(createStreamFields());
            streamBuilder.source(createAgentFileSource());
            streamBuilder.sink(createHBaseSink());
            streamBuilder.initOrUpdate();
            // start group
            InlongGroupContext inlongGroupContext = group.init();
            Assert.notNull(inlongGroupContext);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testStopGroup() {
        ClientConfiguration configuration = new ClientConfiguration();
        configuration.setWriteTimeout(10);
        configuration.setReadTimeout(10);
        configuration.setConnectTimeout(10);
        configuration.setTimeUnit(TimeUnit.SECONDS);
        configuration.setAuthentication(super.getInlongAuth());
        InlongClient inlongClient = InlongClient.create(super.getServiceUrl(), configuration);
        InlongGroupInfo groupInfo = createGroupInfo();
        try {
            InlongGroup group = inlongClient.forGroup(groupInfo);
            InlongGroupContext groupContext = group.delete();
            Assert.notNull(groupContext);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private FileSource createAgentFileSource() {
        FileSource fileSource = new FileSource();
        fileSource.setSourceName("{source.name}");
        fileSource.setAgentIp("{agent.ip}");
        fileSource.setPattern("/a/b/*.txt");
        fileSource.setTimeOffset("-1h");
        return fileSource;
    }

    private List<StreamField> createStreamFields() {
        List<StreamField> streamFieldList = Lists.newArrayList();
        streamFieldList.add(new StreamField(0, FieldType.STRING.toString(), "name", null, null));
        streamFieldList.add(new StreamField(1, FieldType.INT.toString(), "age", null, null));
        streamFieldList.add(new StreamField(2, FieldType.DECIMAL.toString(), "score", null, null));
        return streamFieldList;
    }

    /**
     * Create iceberg sink
     */
    public HBaseSink createHBaseSink() throws Exception {
        HBaseSink sink = new HBaseSink();

        sink.setSinkName("{sink.name}");
        sink.setNamespace("{db.name}");
        sink.setTableName("{table.name}");
        sink.setZkQuorum("{ip:port}");
        sink.setZkNodeParent("{zk.node.path}");
        sink.setRowKey("{rowkey}");

        final SinkField field1 = new SinkField(0, FieldType.INT.toString(), "age", FieldType.INT.toString(), "age");
        final SinkField field2 = new SinkField(1, FieldType.STRING.toString(), "name", FieldType.STRING.toString(),
                "name");
        final SinkField field3 = new SinkField(2, FieldType.DECIMAL.toString(), "score", FieldType.DECIMAL.toString(),
                "score");

        // field ext param
        HBaseColumnFamilyInfo info1 = new HBaseColumnFamilyInfo();
        info1.setCfName("cf_1");
        String json1 = new ObjectMapper().writeValueAsString(info1);
        field1.setExtParams(json1);
        System.out.println("ext1 = " + json1);

        HBaseColumnFamilyInfo info2 = new HBaseColumnFamilyInfo();
        info2.setCfName("cf_2");
        String json2 = new ObjectMapper().writeValueAsString(info2);
        field2.setExtParams(json2);
        System.out.println("ext2 = " + json2);

        HBaseColumnFamilyInfo info3 = new HBaseColumnFamilyInfo();
        info3.setCfName("cf_3");
        String json3 = new ObjectMapper().writeValueAsString(info3);
        field3.setExtParams(json3);
        System.out.println("ext3 = " + json3);

        List<SinkField> fields = new ArrayList<>();
        fields.add(field1);
        fields.add(field2);
        fields.add(field3);
        sink.setFieldList(fields);

        sink.setProperties(new HashMap<>());
        return sink;
    }
}
