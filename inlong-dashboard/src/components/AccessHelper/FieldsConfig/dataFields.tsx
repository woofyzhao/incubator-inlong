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
import { FormItemProps } from '@/components/FormGenerator';
import { pickObjectArray } from '@/utils';
import EditableTable from '@/components/EditableTable';
import i18n from '@/i18n';
import { fieldTypes as sourceFieldsTypes } from '@/components/MetaData/SourceDataFields';

type RestParams = {
  inlongGroupId?: string;
  // Whether to use true operation for data source management
  useDataSourcesActionRequest?: boolean;
  // Whether to use real operation for data storage management
  useDataStorageActionRequest?: boolean;
  // Whether the fieldList is in edit mode
  fieldListEditing?: boolean;
  readonly?: boolean;
};

export default (
  names: (string | FormItemProps)[],
  currentValues: Record<string, any> = {},
  {
    inlongGroupId,
    useDataSourcesActionRequest = false,
    useDataStorageActionRequest = false,
    fieldListEditing = true,
    readonly = false,
  }: RestParams = {},
): FormItemProps[] => {
  // const basicProps = {
  //   inlongGroupId: inlongGroupId,
  //   inlongStreamId: currentValues.inlongStreamId,
  // };

  const fields: FormItemProps[] = [
    {
      type: 'input',
      label: i18n.t('components.AccessHelper.FieldsConfig.dataFields.DataStreamID'),
      name: 'inlongStreamId',
      props: {
        maxLength: 32,
      },
      initialValue: currentValues.inlongStreamId,
      rules: [
        { required: true },
        {
          pattern: /^[0-9a-z_-]+$/,
          message: i18n.t('components.AccessHelper.FieldsConfig.dataFields.InlongStreamIdRules'),
        },
      ],
    },
    {
      type: 'input',
      label: i18n.t('components.AccessHelper.FieldsConfig.dataFields.DataStreamName'),
      name: 'name',
      initialValue: currentValues.name,
    },
    {
      type: 'textarea',
      label: i18n.t('components.AccessHelper.FieldsConfig.dataFields.DataFlowIntroduction'),
      name: 'description',
      props: {
        showCount: true,
        maxLength: 100,
      },
      initialValue: currentValues.desc,
    },
    {
      type: 'radio',
      label: i18n.t('components.AccessHelper.FieldsConfig.dataFields.DataType'),
      name: 'dataType',
      initialValue: currentValues.dataType ?? 'CSV',
      tooltip: i18n.t('components.AccessHelper.FieldsConfig.dataFields.DataTypeCsvHelp'),
      props: {
        options: [
          {
            label: 'CSV',
            value: 'CSV',
          },
          {
            label: 'KEY-VALUE',
            value: 'KEY-VALUE',
          },
          {
            label: 'JSON',
            value: 'JSON',
          },
          {
            label: 'AVRO',
            value: 'AVRO',
          },
        ],
      },
      rules: [{ required: true }],
      // visible: values => values.dataSourceType !== 'MYSQL_BINLOG',
    },
    {
      type: 'radio',
      label: i18n.t('components.AccessHelper.FieldsConfig.dataFields.DataEncoding'),
      name: 'dataEncoding',
      initialValue: currentValues.dataEncoding ?? 'UTF-8',
      props: {
        options: [
          {
            label: 'UTF-8',
            value: 'UTF-8',
          },
          {
            label: 'GBK',
            value: 'GBK',
          },
        ],
      },
      rules: [{ required: true }],
      // visible: values => values.dataSourceType === 'FILE' || values.dataSourceType === 'AUTO_PUSH',
    },
    {
      type: 'select',
      label: i18n.t('components.AccessHelper.FieldsConfig.dataFields.fileDelimiter'),
      name: 'dataSeparator',
      initialValue: '124',
      props: {
        dropdownMatchSelectWidth: false,
        options: [
          {
            label: i18n.t('components.AccessHelper.FieldsConfig.dataFields.Space'),
            value: '32',
          },
          {
            label: i18n.t('components.AccessHelper.FieldsConfig.dataFields.VerticalLine'),
            value: '124',
          },
          {
            label: i18n.t('components.AccessHelper.FieldsConfig.dataFields.Comma'),
            value: '44',
          },
          {
            label: i18n.t('components.AccessHelper.FieldsConfig.dataFields.Semicolon'),
            value: '59',
          },
          {
            label: i18n.t('components.AccessHelper.FieldsConfig.dataFields.Asterisk'),
            value: '42',
          },
          {
            label: i18n.t('components.AccessHelper.FieldsConfig.dataFields.DoubleQuotes'),
            value: '34',
          },
        ],
        useInput: true,
        useInputProps: {
          placeholder: 'ASCII',
        },
        style: { width: 100 },
      },
      rules: [
        {
          required: true,
          type: 'number',
          transform: val => +val,
          min: 0,
          max: 127,
        },
      ],
      // visible: values =>
      //   (values.dataSourceType === 'FILE' || values.dataSourceType === 'AUTO_PUSH') &&
      //   values.dataType === 'CSV',
    },
    {
      type: (
        <EditableTable
          size="small"
          editing={fieldListEditing}
          columns={[
            {
              title: i18n.t('components.AccessHelper.FieldsConfig.dataFields.FieldName'),
              dataIndex: 'fieldName',
              props: () => ({
                disabled: !fieldListEditing,
              }),
              rules: [
                { required: true },
                {
                  pattern: /^[a-zA-Z][a-zA-Z0-9_]*$/,
                  message: i18n.t('components.AccessHelper.FieldsConfig.dataFields.FieldNameRule'),
                },
              ],
            },
            {
              title: i18n.t('components.AccessHelper.FieldsConfig.dataFields.FieldType'),
              dataIndex: 'fieldType',
              type: 'select',
              initialValue: sourceFieldsTypes[0].value,
              props: () => ({
                disabled: !fieldListEditing,
                options: sourceFieldsTypes,
              }),
              rules: [{ required: true }],
            },
            {
              title: i18n.t('components.AccessHelper.FieldsConfig.dataFields.FieldComment'),
              dataIndex: 'fieldComment',
            },
          ]}
        />
      ),
      label: i18n.t('components.AccessHelper.FieldsConfig.dataFields.SourceDataField'),
      name: 'rowTypeFields',
      visible: () => !(currentValues.dataType as string[])?.includes('PB'),
    },
  ];

  return pickObjectArray(names, fields);
};
