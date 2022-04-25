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

package org.apache.inlong.tubemq.manager.service;

import static org.apache.inlong.tubemq.manager.controller.node.request.AddBrokersReq.getAddBrokerReq;

import com.google.common.collect.Lists;
import com.google.gson.Gson;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.inlong.tubemq.manager.controller.TubeMQResult;
import org.apache.inlong.tubemq.manager.controller.node.dto.MasterDto;
import org.apache.inlong.tubemq.manager.controller.node.request.AddBrokersReq;
import org.apache.inlong.tubemq.manager.controller.node.request.AddTopicReq;
import org.apache.inlong.tubemq.manager.controller.node.request.CloneBrokersReq;
import org.apache.inlong.tubemq.manager.controller.node.request.CloneTopicReq;
import org.apache.inlong.tubemq.manager.controller.node.request.QueryBrokerCfgReq;
import org.apache.inlong.tubemq.manager.entry.ClusterEntry;
import org.apache.inlong.tubemq.manager.entry.MasterEntry;
import org.apache.inlong.tubemq.manager.repository.MasterRepository;
import org.apache.inlong.tubemq.manager.service.interfaces.MasterService;
import org.apache.inlong.tubemq.manager.service.interfaces.NodeService;
import org.apache.inlong.tubemq.manager.service.interfaces.TopicService;
import org.apache.inlong.tubemq.manager.service.tube.AddBrokerResult;
import org.apache.inlong.tubemq.manager.service.tube.AddTopicsResult;
import org.apache.inlong.tubemq.manager.service.tube.BrokerConf;
import org.apache.inlong.tubemq.manager.service.tube.BrokerStatusInfo;
import org.apache.inlong.tubemq.manager.service.tube.IpIdRelation;
import org.apache.inlong.tubemq.manager.service.tube.TubeHttpBrokerInfoList;
import org.apache.inlong.tubemq.manager.service.tube.TubeHttpResponse;
import org.apache.inlong.tubemq.manager.service.tube.TubeHttpTopicInfoList;
import org.apache.inlong.tubemq.manager.utils.ConvertUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * node service to query broker/master/standby status of tube cluster.
 */
@Slf4j
@Component
public class NodeServiceImpl implements NodeService {

    private final CloseableHttpClient httpclient = HttpClients.createDefault();
    private final Gson gson = new Gson();

    @Value("${manager.max.configurable.broker.size:50}")
    private int maxConfigurableBrokerSize;

    @Autowired
    private MasterRepository masterRepository;

    @Autowired
    private TopicService topicService;

    @Autowired
    private MasterService masterService;

    /**
     * request node status via http.
     *
     * @param masterEntry - node entry
     * @return
     */
    @Override
    public TubeHttpBrokerInfoList requestBrokerStatus(MasterEntry masterEntry) {
        String url = TubeConst.SCHEMA + masterEntry.getIp() + ":"
                + masterEntry.getWebPort() + TubeConst.BROKER_RUN_STATUS;
        HttpGet httpget = new HttpGet(url);
        try (CloseableHttpResponse response = httpclient.execute(httpget)) {
            TubeHttpBrokerInfoList brokerInfoList =
                    gson.fromJson(new InputStreamReader(response.getEntity().getContent(), StandardCharsets.UTF_8),
                            TubeHttpBrokerInfoList.class);
            // request return normal.
            if (brokerInfoList.getCode() == TubeConst.SUCCESS_CODE) {
                // divide by state.
                brokerInfoList.divideBrokerListByState();
                return brokerInfoList;
            }
            log.error("query brokerInfo list fail with info returned by master {}", brokerInfoList.getErrMsg());
        } catch (Exception ex) {
            log.error("exception caught while requesting broker status, master may not be online", ex);
        }
        return null;
    }

    /**
     * clone source broker to generate brokers with the same config and copy the topics in it.
     *
     * @param req
     * @return
     * @throws Exception exception
     */
    @Override
    public TubeMQResult cloneBrokersWithTopic(CloneBrokersReq req) throws Exception {

        int clusterId = req.getClusterId();
        // 1. query source broker config
        QueryBrokerCfgReq queryReq = QueryBrokerCfgReq.getReq(req.getSourceBrokerId());
        MasterEntry masterEntry = masterService.getMasterNode(Long.valueOf(clusterId));
        BrokerStatusInfo brokerStatusInfo = getBrokerStatusInfo(queryReq, masterEntry);

        // 2. use source broker config to clone brokers
        BrokerConf sourceBrokerConf = brokerStatusInfo.getData().get(0);
        AddBrokersReq addBrokersReq = getBatchAddBrokersReq(req, clusterId, sourceBrokerConf);

        // 3. request master, return broker ids generated by master
        AddBrokerResult addBrokerResult = addBrokersToClusterWithId(addBrokersReq, masterEntry);

        // might have duplicate brokers
        if (addBrokerResult.getErrCode() != TubeConst.SUCCESS_CODE) {
            return TubeMQResult.errorResult(addBrokerResult.getErrMsg());
        }
        List<Integer> brokerIds = getBrokerIds(addBrokerResult);
        List<AddTopicReq> addTopicReqs = req.getAddTopicReqs();

        // 4. add topic to brokers
        return addTopicsToBrokers(masterEntry, brokerIds, addTopicReqs);
    }

    @Override
    public TubeMQResult addTopicsToBrokers(MasterEntry masterEntry, List<Integer> brokerIds,
                                           List<AddTopicReq> addTopicReqs) {
        TubeMQResult tubeResult = new TubeMQResult();
        AddTopicsResult addTopicsResult = new AddTopicsResult();

        if (CollectionUtils.isEmpty(addTopicReqs)) {
            return tubeResult;
        }
        addTopicReqs.forEach(addTopicReq -> {
            try {
                String brokerStr = StringUtils.join(brokerIds, ",");
                addTopicReq.setBrokerId(brokerStr);
                TubeMQResult result = addTopicToBrokers(addTopicReq, masterEntry);
                if (result.getErrCode() == TubeConst.SUCCESS_CODE) {
                    addTopicsResult.getSuccessTopics().add(addTopicReq.getTopicName());
                } else {
                    addTopicsResult.getFailTopics().add(addTopicReq.getTopicName());
                }
            } catch (Exception e) {
                log.error("add topic to brokers fail with exception", e);
                addTopicsResult.getFailTopics().add(addTopicReq.getTopicName());
            }
        });

        tubeResult.setData(addTopicsResult);
        return tubeResult;
    }

    private List<Integer> getBrokerIds(AddBrokerResult addBrokerResult) {
        List<IpIdRelation> ipids = addBrokerResult.getData();
        List<Integer> brokerIds = Lists.newArrayList();
        for (IpIdRelation ipid : ipids) {
            brokerIds.add(ipid.getBrokerId());
        }
        return brokerIds;
    }

    private AddBrokersReq getBatchAddBrokersReq(CloneBrokersReq req, int clusterId, BrokerConf sourceBrokerConf) {
        AddBrokersReq addBrokersReq = getAddBrokerReq(req.getConfModAuthToken(), clusterId);

        // generate add brokers req using given target broker ips
        List<BrokerConf> brokerConfs = Lists.newArrayList();
        req.getTargetIps().forEach(ip -> {
            BrokerConf brokerConf = new BrokerConf(sourceBrokerConf);
            brokerConf.setBrokerIp(ip);
            brokerConf.setBrokerId(0);
            brokerConfs.add(brokerConf);
        });
        addBrokersReq.setBrokerJsonSet(brokerConfs);
        return addBrokersReq;
    }

    private BrokerStatusInfo getBrokerStatusInfo(QueryBrokerCfgReq queryReq, MasterEntry masterEntry) throws Exception {
        String url = TubeConst.SCHEMA + masterEntry.getIp() + ":" + masterEntry.getWebPort()
                + "/" + TubeConst.TUBE_REQUEST_PATH + "?" + ConvertUtils.convertReqToQueryStr(queryReq);
        BrokerStatusInfo brokerStatusInfo = gson.fromJson(masterService.queryMaster(url),
                BrokerStatusInfo.class);
        return brokerStatusInfo;
    }

    @Override
    public TubeMQResult addTopicToBrokers(AddTopicReq req, MasterEntry masterEntry) throws Exception {
        String url = TubeConst.SCHEMA + masterEntry.getIp() + ":" + masterEntry.getWebPort()
                + "/" + TubeConst.TUBE_REQUEST_PATH + "?" + ConvertUtils.convertReqToQueryStr(req);
        return masterService.requestMaster(url);
    }

    @Override
    public boolean configBrokersForTopics(MasterEntry masterEntry,
                                          Set<String> topics, List<Integer> brokerList, int maxBrokers) {
        if (maxBrokers == 0) {
            return false;
        }
        List<Integer> finalBrokerList = brokerList.subList(0, maxBrokers);
        String brokerStr = StringUtils.join(finalBrokerList, ",");
        String topicStr = StringUtils.join(topics, ",");
        String url = TubeConst.SCHEMA + masterEntry.getIp() + ":" + masterEntry.getWebPort()
                + TubeConst.ADD_TUBE_TOPIC + TubeConst.TOPIC_NAME + topicStr + TubeConst.BROKER_ID + brokerStr
                + TubeConst.CONF_MOD_AUTH_TOKEN + masterEntry.getToken() + TubeConst.CREATE_USER + TubeConst.TUBEADMIN;
        HttpGet httpget = new HttpGet(url);
        log.info("config topics {} to brokers ids {}, masterEntry is : {}",
                topics, finalBrokerList, masterEntry.getIp());
        try (CloseableHttpResponse response = httpclient.execute(httpget)) {
            TubeHttpResponse result =
                    gson.fromJson(new InputStreamReader(response.getEntity().getContent(), StandardCharsets.UTF_8),
                            TubeHttpResponse.class);
            if (result.getErrCode() != TubeConst.SUCCESS_CODE) {
                log.error("config topics {} to brokers ids {} fail : master return with status {}",
                        topics, finalBrokerList, result.getErrMsg());
                return false;
            }
            return true;
        } catch (Exception ex) {
            log.error("exception caught while requesting broker status", ex);
        }
        return false;
    }

    /**
     * reload broker list, cannot exceed maxConfigurableBrokerSize each time.
     *
     * @param masterEntry
     * @param needReloadList
     */
    @Override
    public void handleReloadBroker(MasterEntry masterEntry, List<Integer> needReloadList, ClusterEntry clusterEntry) {
        // reload without exceed max broker.
        if (needReloadList.isEmpty()) {
            return;
        }
        int begin = 0;
        int end = 0;
        do {
            end = Math.min(clusterEntry.getReloadBrokerSize() + begin, needReloadList.size());
            List<Integer> brokerIdList = needReloadList.subList(begin, end);
            String brokerStr = StringUtils.join(brokerIdList, ",");
            String url = TubeConst.SCHEMA + masterEntry.getIp() + ":" + masterEntry.getWebPort()
                    + TubeConst.RELOAD_BROKER + TubeConst.BROKER_ID + brokerStr + TubeConst.CONF_MOD_AUTH_TOKEN
                    + masterEntry.getToken() + TubeConst.MODIFY_USER + "tubeadmin";
            HttpGet httpget = new HttpGet(url);
            try (CloseableHttpResponse response = httpclient.execute(httpget)) {
                TubeHttpResponse result =
                        gson.fromJson(new InputStreamReader(response.getEntity()
                                .getContent(), StandardCharsets.UTF_8), TubeHttpResponse.class);
                if (result.getErrCode() != TubeConst.SUCCESS_CODE) {
                    log.info("reload tube broker : {} to master {}, fail with msg: {}",
                            brokerStr, masterEntry.getIp(), result.getErrMsg());
                }
            } catch (Exception ex) {
                log.error("exception caught while requesting brokers {} status, master is {}",
                        brokerStr, masterEntry.getIp(), ex);
            }
            begin = end;
        } while (end < needReloadList.size());
    }

    @Override
    public void close() throws IOException {
        httpclient.close();
    }

    public AddBrokerResult addBrokersToClusterWithId(AddBrokersReq req, MasterEntry masterEntry) throws Exception {

        String url = TubeConst.SCHEMA + masterEntry.getIp() + ":" + masterEntry.getWebPort()
                + "/" + TubeConst.TUBE_REQUEST_PATH + "?" + ConvertUtils.convertReqToQueryStr(req);
        HttpGet httpget = new HttpGet(url);
        try (CloseableHttpResponse response = httpclient.execute(httpget)) {
            return gson.fromJson(new InputStreamReader(response.getEntity().getContent(),
                            StandardCharsets.UTF_8),
                    AddBrokerResult.class);
        } catch (Exception ex) {
            log.error("exception caught while requesting broker status", ex);
        }
        return null;
    }

    /**
     * given one topic, copy its config and clone to brokers
     * if no broker is is provided, topics will be cloned to all brokers in cluster
     *
     * @param req
     * @return
     * @throws Exception exception
     */
    @Override
    public TubeMQResult cloneTopicToBrokers(CloneTopicReq req) throws Exception {

        MasterEntry master = masterService.getMasterNode(req);
        if (master == null) {
            return TubeMQResult.errorResult(TubeMQErrorConst.NO_SUCH_CLUSTER);
        }
        // 1 query topic config
        TubeHttpTopicInfoList topicInfoList = topicService.requestTopicConfigInfo(master, req.getSourceTopicName());

        if (topicInfoList == null) {
            return TubeMQResult.errorResult("no such topic");
        }

        // 2 if there's no specific broker ids then clone to all of the brokers
        List<Integer> brokerId = req.getBrokerId();

        if (CollectionUtils.isEmpty(brokerId)) {
            TubeHttpBrokerInfoList brokerInfoList = requestBrokerStatus(master);
            if (brokerInfoList != null) {
                brokerId = brokerInfoList.getConfigurableBrokerIdList();
            }
        }

        // 3 generate add topic req
        AddTopicReq addTopicReq = topicInfoList.getAddTopicReq(brokerId,
                req.getTargetTopicName(), req.getConfModAuthToken());

        // 4 send to master
        return addTopicToBrokers(addTopicReq, master);

    }

    @Override
    public void addNode(MasterEntry masterEntry) {
        try {
            masterRepository.saveAndFlush(masterEntry);
        } catch (Exception e) {
            log.error("save masterEntry {} to db fail with ex ", e);
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public TubeMQResult modifyMasterNode(MasterDto masterDto) {
        try {
            MasterEntry masterEntry = masterService.getMasterNode(masterDto.getClusterId());
            masterEntry.setIp(masterDto.getIp());
            masterEntry.setStandby(masterDto.isStandBy());
            masterEntry.setToken(masterDto.getToken());
            masterEntry.setWebPort(masterDto.getWebPort());
            masterEntry.setIp(masterDto.getIp());
            masterRepository.save(masterEntry);
        } catch (Exception e) {
            log.error("modify master node error with ex", e);
            return TubeMQResult.errorResult(e.getMessage());
        }
        return TubeMQResult.successResult();
    }
}
