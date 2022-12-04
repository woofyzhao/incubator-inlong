/**
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

package org.apache.inlong.dataproxy.sink.mq.pulsar;

import lombok.extern.slf4j.Slf4j;
import org.apache.pulsar.client.api.Producer;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;
import org.apache.pulsar.client.api.SizeUnit;

import java.security.SecureRandom;

@Slf4j
public class Test {

    public static void main(String[] args) throws PulsarClientException, InterruptedException {
        new Test().runTest(null, "public/tg_1203_007/s_1203_007");
    }

    public PulsarClient runTest(PulsarClient client, String producerTopic)
            throws PulsarClientException, InterruptedException {
        if (client == null) {
            client = PulsarClient.builder()
                    .serviceUrl("pulsar://127.0.0.1:6650")
                    .authentication(null)
                    // .allowTlsInsecureConnection(true)
                    .ioThreads(6)
                    .memoryLimit(1073741824L, SizeUnit.BYTES)
                    .connectionsPerBroker(10)
                    .build();
        }

        // Thread.sleep(1000 * 60 * 3);

        // ProducerBuilder<byte[]> baseBuilder = client.newProducer();
        // baseBuilder
        // .sendTimeout(0, TimeUnit.MILLISECONDS)
        // .maxPendingMessages(500)
        // .maxPendingMessagesAcrossPartitions(60000);
        // baseBuilder
        // .batchingMaxMessages(500)
        // .batchingMaxPublishDelay(100, TimeUnit.MILLISECONDS)
        // .batchingMaxBytes(131072);
        // baseBuilder
        // .accessMode(ProducerAccessMode.Shared)
        // .messageRoutingMode(MessageRoutingMode.RoundRobinPartition)
        // .blockIfQueueFull(true);
        // baseBuilder
        // .roundRobinRouterBatchingPartitionSwitchFrequency(60)
        // .enableBatching(true)
        // .compressionType(CompressionType.SNAPPY);

        SecureRandom secureRandom = new SecureRandom(
                (producerTopic + System.currentTimeMillis()).getBytes());
        String producerName = producerTopic + "-" + secureRandom.nextLong();

        Producer<byte[]> producer = client.newProducer()
                .topic(producerTopic)
                .producerName(producerName)
                .create();

        // Producer<byte[]> producer = baseBuilder.clone().topic(producerTopic)
        // .producerName(producerName)
        // .create();

        producer.send(("test msg of " + producerName).getBytes());

        log.info("===> RUN TEST OK!");
        // System.out.println("bye bye");
        // System.exit(0);
        return client;
    }
}
