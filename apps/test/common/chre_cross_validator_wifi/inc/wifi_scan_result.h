/*
 * Copyright (C) 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef WIFI_SCAN_RESULT_H_
#define WIFI_SCAN_RESULT_H_

#include <cinttypes>

#include <pb_decode.h>

#include "chre_api/chre.h"
#include "chre_cross_validation_wifi.nanopb.h"

class WifiScanResult {
 public:
  WifiScanResult() {
  }  // Need for default ctor of WifiScanResult arrays in manager

  /**
   * The ctor for the scan result for the AP.
   *
   * @param apWifiScanResultStream The wifi istream build from the nanoapp
   * message buffer from AP.
   */
  WifiScanResult(pb_istream_t *apWifiScanResultStream);

  /**
   * The ctor for the scan result for CHRE.
   *
   * @param apScanResult The wifi scan result struct received from CHRE apis.
   */
  WifiScanResult(const chreWifiScanResult &chreScanResult);

  static bool areEqual(const WifiScanResult &result1,
                       const WifiScanResult &result2);

  static bool bssidsAreEqual(const WifiScanResult &result1,
                             const WifiScanResult &result2);

  uint8_t getResultIndex() const {
    return mResultIndex;
  }
  uint8_t getTotalNumResults() const {
    return mTotalNumResults;
  }

  // TODO(b/154271547): Remove this method and check that all scan results are
  // valid instead
  bool isLastMessage() const {
    return mResultIndex >= mTotalNumResults - 1;
  }

  bool getSeen() const {
    return mSeen;
  }

  void didSee() {
    mSeen = true;
  }

  const uint8_t *getBssid() const {
    return mBssid;
  }

  const char *getSsid() const {
    return mSsid;
  }

 private:
  char mSsid[CHRE_WIFI_SSID_MAX_LEN];
  uint8_t mBssid[CHRE_WIFI_BSSID_LEN];

  uint8_t mTotalNumResults = 0;
  uint8_t mResultIndex = 0;

  //! If true then, a scan result with this bssid has been seen before in the
  //! other set of scan results.
  bool mSeen = false;

  static bool decodeString(pb_istream_t *stream, const pb_field_t * /*field*/,
                           void **arg);
};

#endif  // WIFI_SCAN_RESULT_H_
