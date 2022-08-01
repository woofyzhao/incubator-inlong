/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import React from 'react';
import i18n from '@/i18n';
import StaffSelect from '@/components/StaffSelect';
import type { ClsConfigItemType, ClsTableItemType } from './types';
import { DataProxy } from './DataProxy';
import { Pulsar } from './Pulsar';
import { TubeMQ } from './TubeMQ';

export interface ClusterItemType {
  label: string;
  value: string;
  config: ClsConfigItemType[];
  tableColumns: ClsTableItemType[];
}

export const Clusters: ClusterItemType[] = [
  {
    label: 'DataProxy',
    value: 'DATAPROXY',
    config: DataProxy,
  },
  {
    label: 'Pulsar',
    value: 'PULSAR',
    config: Pulsar,
  },
  {
    label: 'TubeMQ',
    value: 'TUBEMQ',
    config: TubeMQ,
  },
].map(item => {
  const defaultConfig: ClsConfigItemType[] = [
    {
      type: 'input',
      label: i18n.t('pages.Clusters.Name'),
      name: 'name',
      rules: [{ required: true }],
      props: {
        maxLength: 128,
      },
      _inTable: true,
    },
    {
      type: 'select',
      label: i18n.t('pages.Clusters.Tag'),
      name: 'clusterTags',
      rules: [{ required: true }],
      props: {
        mode: 'multiple',
        filterOption: false,
        options: {
          requestTrigger: ['onOpen', 'onSearch'],
          requestService: keyword => ({
            url: '/cluster/tag/list',
            method: 'POST',
            data: {
              keyword,
              pageNum: 1,
              pageSize: 20,
            },
          }),
          requestParams: {
            formatResult: result =>
              result?.list?.map(item => ({
                ...item,
                label: item.clusterTag,
                value: item.clusterTag,
              })),
          },
        },
      },
      _inTable: true,
    },
    {
      type: <StaffSelect mode="multiple" />,
      label: i18n.t('pages.Clusters.InCharges'),
      name: 'inCharges',
      rules: [{ required: true }],
      _inTable: true,
    },
    {
      type: 'textarea',
      label: i18n.t('pages.Clusters.Description'),
      name: 'description',
      props: {
        maxLength: 256,
      },
    },
  ];
  const config = defaultConfig.concat(item.config);

  return {
    ...item,
    config,
    tableColumns: config
      .filter(k => k._inTable)
      .map(k => {
        if (typeof k._inTable === 'boolean') {
          return {
            title: k.label,
            dataIndex: k.name,
          };
        }
        return k._inTable;
      }),
  };
});
