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

package org.apache.inlong.dataproxy.config;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.inlong.common.pojo.dataproxy.DataProxyTopicInfo;
import org.apache.inlong.common.pojo.dataproxy.MQClusterInfo;
import org.apache.inlong.dataproxy.config.holder.FileConfigHolder;
import org.apache.inlong.dataproxy.config.holder.GroupIdPropertiesHolder;
import org.apache.inlong.dataproxy.config.holder.MQClusterConfigHolder;
import org.apache.inlong.dataproxy.config.holder.MxPropertiesHolder;
import org.apache.inlong.dataproxy.config.holder.PropertiesConfigHolder;
import org.apache.inlong.dataproxy.config.pojo.MQClusterConfig;
import org.apache.inlong.dataproxy.consts.AttributeConstants;
import org.apache.inlong.dataproxy.consts.ConfigConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Config manager class.
 */
@Slf4j
public class ConfigManager {

    public static final List<ConfigHolder> CONFIG_HOLDER_LIST = new ArrayList<>();
    private static volatile boolean isInit = false;
    private static ConfigManager instance = null;

    private final PropertiesConfigHolder commonConfig = new PropertiesConfigHolder("common.properties");
    private final MQClusterConfigHolder mqClusterConfigHolder = new MQClusterConfigHolder("mq_cluster.properties");
    private final PropertiesConfigHolder topicConfig = new PropertiesConfigHolder("topics.properties");
    private final MxPropertiesHolder mxConfig = new MxPropertiesHolder("mx.properties");

    private final GroupIdPropertiesHolder groupIdConfig = new GroupIdPropertiesHolder("groupid_mapping.properties");
    private final PropertiesConfigHolder dcConfig = new PropertiesConfigHolder("dc_mapping.properties");
    private final PropertiesConfigHolder transferConfig = new PropertiesConfigHolder("transfer.properties");
    private final PropertiesConfigHolder tubeSwitchConfig = new PropertiesConfigHolder("tube_switch.properties");
    private final PropertiesConfigHolder weightHolder = new PropertiesConfigHolder("weight.properties");
    private final FileConfigHolder blackListConfig = new FileConfigHolder("blacklist.properties");

    /**
     * get instance for config manager
     */
    public static ConfigManager getInstance() {
        log.info("===> create repository for {}" + ConfigManager.class.getSimpleName());
        if (isInit && instance != null) {
            return instance;
        }
        synchronized (ConfigManager.class) {
            if (!isInit) {
                instance = new ConfigManager();
                for (ConfigHolder holder : CONFIG_HOLDER_LIST) {
                    holder.loadFromFileToHolder();
                }
                ReloadConfigWorker reloadProperties = ReloadConfigWorker.create(instance);
                reloadProperties.setDaemon(true);
                reloadProperties.start();
            }
            isInit = true;
        }
        return instance;
    }

    public Map<String, String> getWeightProperties() {
        return weightHolder.getHolder();
    }

    public Map<String, String> getTopicProperties() {
        return topicConfig.getHolder();
    }

    /**
     * update old maps, reload local files if changed.
     *
     * @param result - map pending to be added
     * @param holder - property holder
     * @param addElseRemove - if add(true) else remove(false)
     * @return true if changed else false.
     */
    private boolean updatePropertiesHolder(Map<String, String> result, PropertiesConfigHolder holder,
            boolean addElseRemove) {
        Map<String, String> tmpHolder = holder.forkHolder();
        boolean changed = false;

        for (Map.Entry<String, String> entry : result.entrySet()) {
            if (addElseRemove) {
                String oldValue = tmpHolder.put(entry.getKey(), entry.getValue());
                if (!ObjectUtils.equals(oldValue, entry.getValue())) {
                    changed = true;
                }
            } else {
                String oldValue = tmpHolder.remove(entry.getKey());
                if (oldValue != null) {
                    changed = true;
                }
            }
        }

        if (changed) {
            return holder.loadFromHolderToFile(tmpHolder);
        } else {
            return false;
        }
    }

    public boolean addTopicProperties(Map<String, String> result) {
        return updatePropertiesHolder(result, topicConfig, true);
    }

    public boolean deleteTopicProperties(Map<String, String> result) {
        return updatePropertiesHolder(result, topicConfig, false);
    }

    public boolean updateMQClusterProperties(Map<String, String> result) {
        return updatePropertiesHolder(result, mqClusterConfigHolder, true);
    }

    public Map<String, String> getMxProperties() {
        return mxConfig.getHolder();
    }

    public boolean addMxProperties(Map<String, String> result) {
        return updatePropertiesHolder(result, mxConfig, true);
    }

    public boolean deleteMxProperties(Map<String, String> result) {
        return updatePropertiesHolder(result, mxConfig, false);
    }

    public Map<String, String> getDcMappingProperties() {
        return dcConfig.getHolder();
    }

    public Map<String, String> getTransferProperties() {
        return transferConfig.getHolder();
    }

    public Map<String, String> getTubeSwitchProperties() {
        return tubeSwitchConfig.getHolder();
    }

    public Map<String, Map<String, String>> getMxPropertiesMaps() {
        return mxConfig.getMxPropertiesMaps();
    }

    public Map<String, String> getGroupIdMappingProperties() {
        return groupIdConfig.getGroupIdMappingProperties();
    }

    public Map<String, Map<String, String>> getStreamIdMappingProperties() {
        return groupIdConfig.getStreamIdMappingProperties();
    }

    public Map<String, String> getGroupIdEnableMappingProperties() {
        return groupIdConfig.getGroupIdEnableMappingProperties();
    }

    public Map<String, String> getCommonProperties() {
        return commonConfig.getHolder();
    }

    public PropertiesConfigHolder getTopicConfig() {
        return topicConfig;
    }

    public MQClusterConfigHolder getMqClusterHolder() {
        return mqClusterConfigHolder;
    }

    public MQClusterConfig getMqClusterConfig() {
        return mqClusterConfigHolder.getClusterConfig();
    }

    public Map<String, String> getMqClusterUrl2Token() {
        return mqClusterConfigHolder.getUrl2token();
    }

    /**
     * load worker
     */
    public static class ReloadConfigWorker extends Thread {

        private static final Logger LOG = LoggerFactory.getLogger(ReloadConfigWorker.class);
        private final ConfigManager configManager;
        private final CloseableHttpClient httpClient;
        private final Gson gson = new Gson();
        private boolean isRunning = true;

        private ReloadConfigWorker(ConfigManager managerInstance) {
            this.configManager = managerInstance;
            this.httpClient = constructHttpClient();
        }

        public static ReloadConfigWorker create(ConfigManager managerInstance) {
            return new ReloadConfigWorker(managerInstance);
        }

        private synchronized CloseableHttpClient constructHttpClient() {
            long timeoutInMs = TimeUnit.MILLISECONDS.toMillis(50000);
            RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectTimeout((int) timeoutInMs)
                    .setSocketTimeout((int) timeoutInMs).build();
            HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
            httpClientBuilder.setDefaultRequestConfig(requestConfig);
            return httpClientBuilder.build();
        }

        public int getRandom(int min, int max) {
            return (int) (Math.random() * (max + 1 - min)) + min;
        }

        private long getSleepTime() {
            String sleepTimeInMsStr = configManager.getCommonProperties().get("configCheckInterval");
            long sleepTimeInMs = 10000;
            try {
                if (sleepTimeInMsStr != null) {
                    sleepTimeInMs = Long.parseLong(sleepTimeInMsStr);
                }
            } catch (Exception e) {
                LOG.info("ignored exception ", e);
            }
            return sleepTimeInMs + getRandom(0, 5000);
        }

        public void close() {
            isRunning = false;
        }

        private void checkLocalFile() {
            for (ConfigHolder holder : CONFIG_HOLDER_LIST) {
                boolean isChanged = holder.checkAndUpdateHolder();
                if (isChanged) {
                    holder.executeCallbacks();
                }
            }
        }

        private boolean checkWithManager(String host, String clusterName) {
            if (StringUtils.isEmpty(clusterName)) {
                LOG.error("proxyClusterName is null");
                return false;
            }

            HttpGet httpGet = null;
            try {
                String url = "http://" + host + ConfigConstants.MANAGER_PATH + ConfigConstants.MANAGER_GET_CONFIG_PATH
                        + "?clusterName=" + clusterName;
                LOG.info("===> start to request {} to get config info", url);
                httpGet = new HttpGet(url);
                httpGet.addHeader(HttpHeaders.CONNECTION, "close");

                // request with post
                CloseableHttpResponse response = httpClient.execute(httpGet);
                String returnStr = EntityUtils.toString(response.getEntity());
                // get groupId <-> topic and m value.
                RemoteConfigJson configJson = gson.fromJson(returnStr, RemoteConfigJson.class);
                Map<String, String> groupIdToTopic = new HashMap<>();
                Map<String, String> groupIdToMValue = new HashMap<>();
                // include url2token and other params
                Map<String, String> mqConfig = new HashMap<>();

                // get config success
                if (configJson.isSuccess() && configJson.getData() != null) {
                    LOG.info("===> getConfig result: {}", returnStr);
                    /*
                     * get mqUrls <->token maps;
                     * if mq is pulsar, store format: mq_cluster.index1=cluster1url1,cluster1url2=token
                     * if mq is tubemq, token is "", store format: mq_cluster.index1=cluster1url1,cluster1url2=
                     */
                    int index = 1;
                    List<MQClusterInfo> clusterSet = configJson.getData().getMqClusterList();
                    if (clusterSet == null || clusterSet.isEmpty()) {
                        LOG.error("===> getConfig from manager: no available mq config");
                        return false;
                    }
                    for (MQClusterInfo mqCluster : clusterSet) {
                        String key = MQClusterConfigHolder.URL_STORE_PREFIX + index;
                        String value =
                                mqCluster.getUrl() + AttributeConstants.KEY_VALUE_SEPARATOR + mqCluster.getToken();
                        mqConfig.put(key, value);
                        ++index;
                    }

                    for (DataProxyTopicInfo topic : configJson.getData().getTopicList()) {
                        if (!StringUtils.isEmpty(topic.getM())) {
                            groupIdToMValue.put(topic.getInlongGroupId(), topic.getM());
                        }
                        if (!StringUtils.isEmpty(topic.getTopic())) {
                            groupIdToTopic.put(topic.getInlongGroupId(), topic.getTopic());
                        }
                    }
                    configManager.addMxProperties(groupIdToMValue);
                    configManager.addTopicProperties(groupIdToTopic);
                    // other params for mq
                    mqConfig.putAll(clusterSet.get(0).getParams());
                    configManager.updateMQClusterProperties(mqConfig);

                    // store mq common configs and url2token
                    configManager.getMqClusterConfig().putAll(mqConfig);
                    configManager.getMqClusterHolder()
                            .setUrl2token(configManager.getMqClusterHolder().getUrl2token());
                } else {
                    LOG.error("===> getConfig from manager error: {}", configJson.getErrMsg());
                }
            } catch (Exception ex) {
                LOG.error("exception caught", ex);
                return false;
            } finally {
                if (httpGet != null) {
                    httpGet.releaseConnection();
                }
            }
            return true;
        }

        private void checkRemoteConfig() {
            try {
                String managerHosts = configManager.getCommonProperties().get("manager.hosts");
                String proxyClusterName = configManager.getCommonProperties().get("proxy.cluster.name");
                if (StringUtils.isEmpty(managerHosts) || StringUtils.isEmpty(proxyClusterName)) {
                    return;
                }
                String[] hostList = StringUtils.split(managerHosts, ",");
                for (String host : hostList) {
                    if (checkWithManager(host, proxyClusterName)) {
                        break;
                    }
                }
            } catch (Exception ex) {
                LOG.error("exception caught", ex);
            }
        }

        @Override
        public void run() {
            long count = 0;
            while (isRunning) {
                long sleepTimeInMs = getSleepTime();
                count += 1;
                try {
                    checkLocalFile();
                    // wait for 30 seconds to update remote config
                    if (count % 3 == 0) {
                        checkRemoteConfig();
                        count = 0;
                    }
                    TimeUnit.MILLISECONDS.sleep(sleepTimeInMs);
                } catch (Exception ex) {
                    LOG.error("exception caught", ex);
                }
            }
        }
    }
}
