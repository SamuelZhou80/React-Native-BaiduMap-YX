/**
 * poi 搜索
 * Jiangj 20180820
 */
import {
  requireNativeComponent,
  NativeModules,
  Platform,
  DeviceEventEmitter,
  Alert,
} from 'react-native';

import React, {
  Component,
  PropTypes
} from 'react';


const _module = NativeModules.PoiSearchModule;

export default {
  
    // 开始搜索
    async poiSearch(keyWord, center, radius, loadIndex, poiSearchResult){
      let lon = center.lon;
      let lat = center.lat;
      try {
       await _module.searchNearbyProcess( keyWord, lon, lat, radius, loadIndex,
          (result) => {
           if(poiSearchResult){
             poiSearchResult(result);
           }
        });
      }
      catch (e) {
        return;
      }
    },
};
