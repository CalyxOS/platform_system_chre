/*
 * Copyright (C) 2016 The Android Open Source Project
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

#include "chre_api/chre/gnss.h"

#include "chre/core/event_loop_manager.h"
#include "chre/util/macros.h"
#include "chre/util/system/napp_permissions.h"
#include "chre/util/time.h"

using chre::EventLoopManager;
using chre::EventLoopManagerSingleton;
using chre::Milliseconds;
using chre::NanoappPermissions;

DLL_EXPORT uint32_t chreGnssGetCapabilities() {
#ifdef CHRE_GNSS_SUPPORT_ENABLED
  return chre::EventLoopManagerSingleton::get()
      ->getGnssManager()
      .getCapabilities();
#else
  return CHRE_GNSS_CAPABILITIES_NONE;
#endif  // CHRE_GNSS_SUPPORT_ENABLED
}

DLL_EXPORT bool chreGnssLocationSessionStartAsync(
    [[maybe_unused]] uint32_t minIntervalMs,
    [[maybe_unused]] uint32_t minTimeToNextFixMs,
    [[maybe_unused]] const void *cookie) {
#ifdef CHRE_GNSS_SUPPORT_ENABLED
  chre::Nanoapp *nanoapp = EventLoopManager::validateChreApiCall(__func__);
  return nanoapp->permitPermissionUse(NanoappPermissions::CHRE_PERMS_GNSS) &&
         chre::EventLoopManagerSingleton::get()
             ->getGnssManager()
             .getLocationSession()
             .addRequest(nanoapp, Milliseconds(minIntervalMs),
                         Milliseconds(minTimeToNextFixMs), cookie);
#else
  return false;
#endif  // CHRE_GNSS_SUPPORT_ENABLED
}

DLL_EXPORT bool chreGnssLocationSessionStopAsync(
    [[maybe_unused]] const void *cookie) {
#ifdef CHRE_GNSS_SUPPORT_ENABLED
  chre::Nanoapp *nanoapp = EventLoopManager::validateChreApiCall(__func__);
  return nanoapp->permitPermissionUse(NanoappPermissions::CHRE_PERMS_GNSS) &&
         chre::EventLoopManagerSingleton::get()
             ->getGnssManager()
             .getLocationSession()
             .removeRequest(nanoapp, cookie);
#else
  return false;
#endif  // CHRE_GNSS_SUPPORT_ENABLED
}

DLL_EXPORT bool chreGnssMeasurementSessionStartAsync(
    [[maybe_unused]] uint32_t minIntervalMs, [[maybe_unused]] const void *cookie) {
#ifdef CHRE_GNSS_SUPPORT_ENABLED
  chre::Nanoapp *nanoapp = EventLoopManager::validateChreApiCall(__func__);
  return nanoapp->permitPermissionUse(NanoappPermissions::CHRE_PERMS_GNSS) &&
         chre::EventLoopManagerSingleton::get()
             ->getGnssManager()
             .getMeasurementSession()
             .addRequest(nanoapp, Milliseconds(minIntervalMs),
                         Milliseconds(0) /* minTimeToNext */, cookie);
#else
  return false;
#endif  // CHRE_GNSS_SUPPORT_ENABLED
}

DLL_EXPORT bool chreGnssMeasurementSessionStopAsync(
    [[maybe_unused]] const void *cookie) {
#ifdef CHRE_GNSS_SUPPORT_ENABLED
  chre::Nanoapp *nanoapp = EventLoopManager::validateChreApiCall(__func__);
  return nanoapp->permitPermissionUse(NanoappPermissions::CHRE_PERMS_GNSS) &&
         chre::EventLoopManagerSingleton::get()
             ->getGnssManager()
             .getMeasurementSession()
             .removeRequest(nanoapp, cookie);
#else
  return false;
#endif  // CHRE_GNSS_SUPPORT_ENABLED
}

DLL_EXPORT bool chreGnssConfigurePassiveLocationListener(
    [[maybe_unused]] bool enable) {
#ifdef CHRE_GNSS_SUPPORT_ENABLED
  chre::Nanoapp *nanoapp = EventLoopManager::validateChreApiCall(__func__);
  return nanoapp->permitPermissionUse(NanoappPermissions::CHRE_PERMS_GNSS) &&
         chre::EventLoopManagerSingleton::get()
             ->getGnssManager()
             .configurePassiveLocationListener(nanoapp, enable);
#else
  return false;
#endif  // CHRE_GNSS_SUPPORT_ENABLED
}
