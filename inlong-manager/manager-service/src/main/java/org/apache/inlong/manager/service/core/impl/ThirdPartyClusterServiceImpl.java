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

package org.apache.inlong.manager.service.core.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.gson.Gson;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.common.pojo.dataproxy.DataProxyConfig;
import org.apache.inlong.common.pojo.dataproxy.ThirdPartyClusterDTO;
import org.apache.inlong.common.pojo.dataproxy.ThirdPartyClusterInfo;
import org.apache.inlong.manager.common.beans.ClusterBean;
import org.apache.inlong.manager.common.enums.Constant;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.enums.GlobalConstants;
import org.apache.inlong.manager.common.enums.GroupStatus;
import org.apache.inlong.manager.common.enums.MQType;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.pojo.cluster.ClusterPageRequest;
import org.apache.inlong.manager.common.pojo.cluster.ClusterRequest;
import org.apache.inlong.manager.common.pojo.cluster.ClusterResponse;
import org.apache.inlong.manager.common.pojo.dataproxy.DataProxyResponse;
import org.apache.inlong.manager.common.settings.InlongGroupSettings;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.common.util.InlongStringUtils;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.dao.entity.InlongGroupEntity;
import org.apache.inlong.manager.dao.entity.InlongGroupPulsarEntity;
import org.apache.inlong.manager.dao.entity.InlongStreamEntity;
import org.apache.inlong.manager.dao.entity.ThirdPartyClusterEntity;
import org.apache.inlong.manager.dao.mapper.InlongGroupEntityMapper;
import org.apache.inlong.manager.dao.mapper.InlongGroupPulsarEntityMapper;
import org.apache.inlong.manager.dao.mapper.InlongStreamEntityMapper;
import org.apache.inlong.manager.dao.mapper.ThirdPartyClusterEntityMapper;
import org.apache.inlong.manager.service.core.ThirdPartyClusterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Implementation of cluster service
 */
@Service
public class ThirdPartyClusterServiceImpl implements ThirdPartyClusterService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ThirdPartyClusterServiceImpl.class);
    private static final Gson GSON = new Gson();

    @Autowired
    private ClusterBean clusterBean;
    @Autowired
    private InlongGroupEntityMapper groupMapper;
    @Autowired
    private InlongGroupPulsarEntityMapper pulsarEntityMapper;
    @Autowired
    private InlongStreamEntityMapper streamMapper;
    @Autowired
    private ThirdPartyClusterEntityMapper thirdPartyClusterMapper;

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public Integer save(ClusterRequest request, String operator) {
        LOGGER.info("begin to insert a cluster info cluster={}", request);
        Preconditions.checkNotNull(request, "cluster is empty");
        ThirdPartyClusterEntity exist = thirdPartyClusterMapper.selectByName(request.getName());
        Preconditions.checkTrue(exist == null, "cluster name already exist");

        ThirdPartyClusterEntity entity = CommonBeanUtils.copyProperties(request, ThirdPartyClusterEntity::new);
        if (operator != null) {
            entity.setCreator(operator);
        }
        Preconditions.checkNotNull(entity.getCreator(), "cluster creator is empty");
        entity.setCreateTime(new Date());
        entity.setIsDeleted(GlobalConstants.UN_DELETED);
        thirdPartyClusterMapper.insert(entity);
        LOGGER.info("success to add a cluster");
        return entity.getId();
    }

    @Override
    public ClusterResponse get(Integer id) {
        LOGGER.info("begin to get cluster by id={}", id);
        Preconditions.checkNotNull(id, "cluster id is empty");
        ThirdPartyClusterEntity entity = thirdPartyClusterMapper.selectByPrimaryKey(id);
        if (entity == null) {
            LOGGER.error("cluster not found by id={}", id);
            throw new BusinessException(ErrorCodeEnum.CLUSTER_NOT_FOUND);
        }
        ClusterResponse clusterResponse = CommonBeanUtils.copyProperties(entity, ClusterResponse::new);
        LOGGER.info("success to get cluster info");
        return clusterResponse;
    }

    @Override
    public PageInfo<ClusterResponse> list(ClusterPageRequest request) {
        PageHelper.startPage(request.getPageNum(), request.getPageSize());
        Page<ThirdPartyClusterEntity> entityPage = (Page<ThirdPartyClusterEntity>)
                thirdPartyClusterMapper.selectByCondition(request);
        List<ClusterResponse> clusterList = CommonBeanUtils.copyListProperties(entityPage,
                ClusterResponse::new);
        PageInfo<ClusterResponse> page = new PageInfo<>(clusterList);
        page.setTotal(entityPage.getTotal());

        LOGGER.debug("success to list cluster by {}", request);
        return page;
    }

    @Override
    public List<String> listClusterIpByType(String type) {
        ClusterPageRequest request = new ClusterPageRequest();
        request.setType(type);
        List<ThirdPartyClusterEntity> entityList = thirdPartyClusterMapper.selectByCondition(request);
        List<String> ipList = new ArrayList<>(entityList.size());
        for (ThirdPartyClusterEntity entity : entityList) {
            ipList.add(entity.getIp());
        }
        return ipList;
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public Boolean update(ClusterRequest request, String operator) {
        Preconditions.checkNotNull(request, "cluster is empty");
        Integer id = request.getId();
        Preconditions.checkNotNull(id, "cluster id is empty");
        ThirdPartyClusterEntity entity = thirdPartyClusterMapper.selectByPrimaryKey(id);
        if (entity == null) {
            LOGGER.error("cluster not found by id={}", id);
            throw new BusinessException(ErrorCodeEnum.CLUSTER_NOT_FOUND);
        }
        CommonBeanUtils.copyProperties(request, entity, true);
        entity.setModifier(operator);
        thirdPartyClusterMapper.updateByPrimaryKeySelective(entity);

        LOGGER.info("success to update cluster={}", request);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public Boolean delete(Integer id, String operator) {
        Preconditions.checkNotNull(id, "cluster id is empty");
        ThirdPartyClusterEntity entity = thirdPartyClusterMapper.selectByPrimaryKey(id);
        if (entity == null) {
            LOGGER.error("cluster not found by id={}", id);
            throw new BusinessException(ErrorCodeEnum.CLUSTER_NOT_FOUND);
        }
        entity.setIsDeleted(id);
        entity.setStatus(GlobalConstants.DELETED_STATUS);
        entity.setModifier(operator);
        thirdPartyClusterMapper.updateByPrimaryKey(entity);
        LOGGER.info("success to delete cluster by id={}", id);
        return true;
    }

    @Override
    public List<DataProxyResponse> getIpList(String clusterName) {
        LOGGER.debug("begin to list data proxy by clusterName={}", clusterName);
        ThirdPartyClusterEntity entity;
        if (StringUtils.isNotBlank(clusterName)) {
            entity = thirdPartyClusterMapper.selectByName(clusterName);
        } else {
            List<ThirdPartyClusterEntity> list = thirdPartyClusterMapper.selectByType(
                    InlongGroupSettings.CLUSTER_DATA_PROXY);
            if (CollectionUtils.isEmpty(list)) {
                LOGGER.warn("data proxy cluster not found by type=" + InlongGroupSettings.CLUSTER_DATA_PROXY);
                return null;
            }
            entity = list.get(0);
        }

        if (entity == null || StringUtils.isBlank(entity.getIp())) {
            LOGGER.warn("data proxy cluster not found by name={}", clusterName);
            return null;
        }
        if (!InlongGroupSettings.CLUSTER_DATA_PROXY.equals(entity.getType())) {
            LOGGER.warn("expected cluster type is DATA_PROXY, but found {}", entity.getType());
            return null;
        }

        String ipStr = entity.getIp();
        while (ipStr.startsWith(Constant.URL_SPLITTER) || ipStr.endsWith(Constant.URL_SPLITTER)
                || ipStr.startsWith(Constant.HOST_SPLITTER) || ipStr.endsWith(Constant.HOST_SPLITTER)) {
            ipStr = InlongStringUtils.trimFirstAndLastChar(ipStr, Constant.URL_SPLITTER);
            ipStr = InlongStringUtils.trimFirstAndLastChar(ipStr, Constant.HOST_SPLITTER);
        }

        List<DataProxyResponse> responseList = new ArrayList<>();
        Integer id = entity.getId();
        Integer defaultPort = entity.getPort();
        int index = ipStr.indexOf(Constant.URL_SPLITTER);
        if (index <= 0) {
            DataProxyResponse response = new DataProxyResponse();
            response.setId(id);
            setIpAndPort(ipStr, defaultPort, response);
            responseList.add(response);
        } else {
            String[] urlArr = ipStr.split(Constant.URL_SPLITTER);
            for (String url : urlArr) {
                DataProxyResponse response = new DataProxyResponse();
                response.setId(id);
                setIpAndPort(url, defaultPort, response);
                responseList.add(response);
            }
        }

        LOGGER.debug("success to list data proxy cluster={}", responseList);
        return responseList;
    }

    private void setIpAndPort(String url, Integer defaultPort, DataProxyResponse response) {
        int idx = url.indexOf(Constant.HOST_SPLITTER);
        if (idx <= 0) {
            response.setIp(url);
            response.setPort(defaultPort);
        } else {
            response.setIp(url.substring(0, idx));
            response.setPort(Integer.valueOf(url.substring(idx + 1)));
        }
    }

    @Override
    public List<DataProxyConfig> getConfig() {
        // get all configs with inlong group status of 130, that is, config successful
        // TODO Optimize query conditions
        List<InlongGroupEntity> groupEntityList = groupMapper.selectAll(GroupStatus.CONFIG_SUCCESSFUL.getCode());
        List<DataProxyConfig> configList = new ArrayList<>();
        for (InlongGroupEntity groupEntity : groupEntityList) {
            String groupId = groupEntity.getInlongGroupId();
            String bizResource = groupEntity.getMqResourceObj();

            DataProxyConfig config = new DataProxyConfig();
            config.setM(groupEntity.getSchemaName());
            MQType mqType = MQType.forType(groupEntity.getMiddlewareType());
            if (mqType == MQType.TUBE) {
                config.setInlongGroupId(groupId);
                config.setTopic(bizResource);
            } else if (mqType == MQType.PULSAR || mqType == MQType.TDMQ_PULSAR) {
                List<InlongStreamEntity> streamList = streamMapper.selectByGroupId(groupId);
                for (InlongStreamEntity stream : streamList) {
                    String topic = stream.getMqResourceObj();
                    String streamId = stream.getInlongStreamId();
                    config.setInlongGroupId(groupId + "/" + streamId);
                    config.setTopic("persistent://" + clusterBean.getDefaultTenant() + "/" + bizResource + "/" + topic);
                }
            }
            configList.add(config);
        }

        return configList;
    }

    /**
     * query data proxy config by cluster name, result includes pulsar/tube cluster configs and topic etc
     */
    @Override
    public ThirdPartyClusterDTO getConfigV2(String clusterName) {
        ThirdPartyClusterEntity clusterEntity = thirdPartyClusterMapper.selectByName(clusterName);
        if (clusterEntity == null) {
            throw new BusinessException("data proxy cluster not found by name=" + clusterName);
        }

        // TODO Optimize query conditions use dataProxyClusterId
        ThirdPartyClusterDTO object = new ThirdPartyClusterDTO();
        List<InlongGroupEntity> groupEntityList = groupMapper.selectAll(GroupStatus.CONFIG_SUCCESSFUL.getCode());
        if (CollectionUtils.isEmpty(groupEntityList)) {
            String msg = "not found any inlong group with success status for proxy cluster name = " + clusterName;
            LOGGER.warn(msg);
            return object;
        }

        // third-party-cluster type
        String mqType = "";
        if (!groupEntityList.isEmpty()) {
            mqType = groupEntityList.get(0).getMiddlewareType();
        }

        // Get topic list by group id
        List<DataProxyConfig> topicList = new ArrayList<>();
        for (InlongGroupEntity groupEntity : groupEntityList) {
            final String groupId = groupEntity.getInlongGroupId();
            final String mqResource = groupEntity.getMqResourceObj();
            MQType type = MQType.forType(mqType);
            if (type == MQType.PULSAR || type == MQType.TDMQ_PULSAR) {
                List<InlongStreamEntity> streamList = streamMapper.selectByGroupId(groupId);
                for (InlongStreamEntity stream : streamList) {
                    DataProxyConfig topicConfig = new DataProxyConfig();
                    String streamId = stream.getInlongStreamId();
                    String topic = stream.getMqResourceObj();
                    String tenant = clusterBean.getDefaultTenant();
                    InlongGroupPulsarEntity pulsarEntity = pulsarEntityMapper.selectByGroupId(groupId);
                    if (pulsarEntity != null && StringUtils.isNotEmpty(pulsarEntity.getTenant())) {
                        tenant = pulsarEntity.getTenant();
                    }
                    topicConfig.setInlongGroupId(groupId + "/" + streamId);
                    topicConfig.setTopic("persistent://" + tenant + "/" + mqResource + "/" + topic);
                    topicList.add(topicConfig);
                }
            } else if (type == MQType.TUBE) {
                DataProxyConfig topicConfig = new DataProxyConfig();
                topicConfig.setInlongGroupId(groupId);
                topicConfig.setTopic(mqResource);
                topicList.add(topicConfig);
            }
        }

        // construct pulsarSet info
        List<ThirdPartyClusterInfo> mqSet = new ArrayList<>();
        List<String> clusterType = Arrays.asList(Constant.CLUSTER_TUBE, Constant.CLUSTER_PULSAR,
                Constant.CLUSTER_TDMQ_PULSAR);
        List<ThirdPartyClusterEntity> clusterList = thirdPartyClusterMapper.selectMQCluster(
                clusterEntity.getMqSetName(), clusterType);
        for (ThirdPartyClusterEntity cluster : clusterList) {
            ThirdPartyClusterInfo clusterInfo = new ThirdPartyClusterInfo();
            clusterInfo.setUrl(cluster.getUrl());
            clusterInfo.setToken(cluster.getToken());
            Map<String, String> configParams = GSON.fromJson(cluster.getExtParams(), Map.class);
            clusterInfo.setParams(configParams);

            mqSet.add(clusterInfo);
        }

        object.setMqSet(mqSet);
        object.setTopicList(topicList);

        return object;
    }

}
