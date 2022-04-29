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

package org.apache.inlong.sort.singletenant.flink.pulsar;

import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.inlong.sort.configuration.Configuration;
import org.apache.inlong.sort.flink.pulsar.PulsarDeserializationSchema;
import org.apache.inlong.sort.flink.pulsar.PulsarSourceFunction;
import org.apache.inlong.sort.flink.pulsar.TDMQPulsarSourceFunction;
import org.apache.inlong.sort.protocol.source.PulsarSourceInfo;
import org.apache.inlong.sort.protocol.source.TDMQPulsarSourceInfo;
import org.apache.inlong.sort.singletenant.flink.SerializedRecord;
import org.apache.pulsar.client.api.Message;

import java.io.IOException;
import java.util.Map;

@Slf4j
public class PulsarSourceBuilder {

    public static PulsarSourceFunction<SerializedRecord> buildPulsarSource(
            PulsarSourceInfo sourceInfo,
            Configuration config,
            Map<String, Object> properties
    ) {

        Map<String, String> configMap = config.toMap();
        if (properties != null && !properties.isEmpty()) {
            for (Map.Entry<String, Object> entry : properties.entrySet()) {
                log.debug("==> pulsar source property entry {} = {}", entry.getKey(), entry.getValue());
                configMap.put(entry.getKey(), entry.getValue().toString());
            }
        }

        org.apache.flink.configuration.Configuration flinkConfig =
                org.apache.flink.configuration.Configuration.fromMap(configMap);
        log.debug("flink.configuration = {}", flinkConfig);

        return new PulsarSourceFunction<>(
                sourceInfo.getAdminUrl(),
                sourceInfo.getServiceUrl(),
                sourceInfo.getTopic(),
                sourceInfo.getSubscriptionName(),
                sourceInfo.getAuthentication(),
                new PulsarDeserializationSchemaImpl(),
                flinkConfig
        );
    }

    public static TDMQPulsarSourceFunction<SerializedRecord> buildTDMQPulsarSource(
            TDMQPulsarSourceInfo tdmqPulsarSourceInfo,
            Configuration config,
            Map<String, Object> properties
    ) {
        Map<String, String> configMap = config.toMap();
        if (properties != null && !properties.isEmpty()) {
            for (Map.Entry<String, Object> entry : properties.entrySet()) {
                configMap.put(entry.getKey(), entry.getValue().toString());
            }
        }

        org.apache.flink.configuration.Configuration flinkConfig =
                org.apache.flink.configuration.Configuration.fromMap(configMap);

        return new TDMQPulsarSourceFunction<>(
                tdmqPulsarSourceInfo.getServiceUrl(),
                tdmqPulsarSourceInfo.getTopic(),
                tdmqPulsarSourceInfo.getSubscriptionName(),
                tdmqPulsarSourceInfo.getAuthentication(),
                new PulsarDeserializationSchemaImpl(),
                flinkConfig
        );
    }

    public static class PulsarDeserializationSchemaImpl implements PulsarDeserializationSchema<SerializedRecord> {

        private static final long serialVersionUID = -3642110610339179932L;

        @Override
        public DeserializationResult<SerializedRecord> deserialize(
                @SuppressWarnings("rawtypes") Message message) throws IOException {
            final byte[] data = message.getData();
            return DeserializationResult.of(new SerializedRecord(message.getEventTime(), data), data.length);
        }

        @Override
        public TypeInformation<SerializedRecord> getProducedType() {
            return TypeInformation.of(SerializedRecord.class);
        }

    }

}
