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

#include "chre/core/event_loop.h"
#include "chre/core/event_loop_manager.h"
#include "chre/platform/assert.h"
#include "chre/platform/memory.h"
#include "chre/platform/shared/debug_dump.h"
#include "chre/platform/system_time.h"
#include "chre/util/macros.h"
#include "chre_api/chre/re.h"

using chre::EventLoopManager;
using chre::EventLoopManagerSingleton;

DLL_EXPORT uint32_t chreGetCapabilities() {
  uint32_t capabilities = CHRE_CAPABILITIES_NONE;

#ifdef CHRE_RELIABLE_MESSAGE_SUPPORT_ENABLED
  capabilities |= CHRE_CAPABILITIES_RELIABLE_MESSAGES;
#endif  // CHRE_RELIABLE_MESSAGE_SUPPORT_ENABLED

  return capabilities;
}

DLL_EXPORT uint32_t chreGetMessageToHostMaxSize() {
#ifdef CHRE_RELIABLE_MESSAGE_SUPPORT_ENABLED

#ifndef CHRE_LARGE_PAYLOAD_MAX_SIZE
  static_assert(false,
                "CHRE_LARGE_PAYLOAD_MAX_SIZE must be defined if "
                "CHRE_RELIABLE_MESSAGE_SUPPORT_ENABLED is enabled");
#else
  static_assert(CHRE_LARGE_PAYLOAD_MAX_SIZE >= CHRE_MESSAGE_TO_HOST_MAX_SIZE,
                "CHRE_LARGE_PAYLOAD_MAX_SIZE must be greater than or equal to "
                "CHRE_MESSAGE_TO_HOST_MAX_SIZE");

  static_assert(CHRE_LARGE_PAYLOAD_MAX_SIZE >= 32000,
                "CHRE_LARGE_PAYLOAD_MAX_SIZE must be greater than or equal to "
                "32000 when CHRE_RELIABLE_MESSAGE_SUPPORT_ENABLED is enabled");
  return CHRE_LARGE_PAYLOAD_MAX_SIZE;
#endif  // CHRE_LARGE_PAYLOAD_MAX_SIZE

#else
  return CHRE_MESSAGE_TO_HOST_MAX_SIZE;
#endif  // CHRE_RELIABLE_MESSAGE_SUPPORT_ENABLED
}

DLL_EXPORT uint64_t chreGetTime() {
  return chre::SystemTime::getMonotonicTime().toRawNanoseconds();
}

DLL_EXPORT int64_t chreGetEstimatedHostTimeOffset() {
  return chre::SystemTime::getEstimatedHostTimeOffset();
}

DLL_EXPORT uint64_t chreGetAppId(void) {
  chre::Nanoapp *nanoapp = EventLoopManager::validateChreApiCall(__func__);
  return nanoapp->getAppId();
}

DLL_EXPORT uint32_t chreGetInstanceId(void) {
  chre::Nanoapp *nanoapp = EventLoopManager::validateChreApiCall(__func__);
  return nanoapp->getInstanceId();
}

DLL_EXPORT uint32_t chreTimerSet(uint64_t duration, const void *cookie,
                                 bool oneShot) {
  chre::Nanoapp *nanoapp = EventLoopManager::validateChreApiCall(__func__);
  return EventLoopManagerSingleton::get()
      ->getEventLoop()
      .getTimerPool()
      .setNanoappTimer(nanoapp, chre::Nanoseconds(duration), cookie, oneShot);
}

DLL_EXPORT bool chreTimerCancel(uint32_t timerId) {
  chre::Nanoapp *nanoapp = EventLoopManager::validateChreApiCall(__func__);
  return EventLoopManagerSingleton::get()
      ->getEventLoop()
      .getTimerPool()
      .cancelNanoappTimer(nanoapp, timerId);
}

DLL_EXPORT void *chreHeapAlloc(uint32_t bytes) {
  chre::Nanoapp *nanoapp = EventLoopManager::validateChreApiCall(__func__);
  return chre::EventLoopManagerSingleton::get()
      ->getMemoryManager()
      .nanoappAlloc(nanoapp, bytes);
}

DLL_EXPORT void chreHeapFree(void *ptr) {
  chre::Nanoapp *nanoapp = EventLoopManager::validateChreApiCall(__func__);
  chre::EventLoopManagerSingleton::get()->getMemoryManager().nanoappFree(
      nanoapp, ptr);
}

DLL_EXPORT void platform_chreDebugDumpVaLog(const char *formatStr,
                                            va_list args) {
  chre::Nanoapp *nanoapp = EventLoopManager::validateChreApiCall(__func__);
  chre::EventLoopManagerSingleton::get()
      ->getDebugDumpManager()
      .appendNanoappLog(*nanoapp, formatStr, args);
}

DLL_EXPORT void chreDebugDumpLog(const char *formatStr, ...) {
  va_list args;
  va_start(args, formatStr);
  platform_chreDebugDumpVaLog(formatStr, args);
  va_end(args);
}
