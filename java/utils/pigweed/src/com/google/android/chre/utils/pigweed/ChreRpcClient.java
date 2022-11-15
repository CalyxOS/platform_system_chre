/*
 * Copyright (C) 2022 The Android Open Source Project
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
package com.google.android.chre.utils.pigweed;

import android.hardware.location.ContextHubClient;
import android.hardware.location.ContextHubClientCallback;
import android.hardware.location.ContextHubInfo;
import android.hardware.location.ContextHubManager;
import android.hardware.location.NanoAppRpcService;
import android.hardware.location.NanoAppState;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;
import java.util.Objects;

import dev.pigweed.pw_rpc.Channel;
import dev.pigweed.pw_rpc.Client;
import dev.pigweed.pw_rpc.MethodClient;
import dev.pigweed.pw_rpc.Service;

/**
 * Pigweed RPC Client Helper.
 *
 * See https://g3doc.corp.google.com/location/lbs/contexthub/g3doc/nanoapps/pw_rpc_host.md
 */
public class ChreRpcClient {
    @NonNull
    private final Client mRpcClient;
    @NonNull
    private final Channel mChannel;
    @NonNull
    private final ChreChannelOutput mChannelOutput;
    @Nullable
    private final ChreCallbackHandler mCallbackHandler;
    private final long mServerNanoappId;
    @NonNull
    private final ContextHubClient mContextHubClient;

    /**
     * Creates a ContextHubClient and initializes the helper.
     *
     * Use this constructor for persistent clients using callbacks.
     *
     * @param manager         The context manager used to create a client
     * @param serverNanoappId The ID of the RPC server nanoapp
     * @param services        The list of services provided by the server
     * @param callback        The callbacks receiving messages and life-cycle events from nanoapps
     */
    public ChreRpcClient(@NonNull ContextHubManager manager, @NonNull ContextHubInfo info,
            long serverNanoappId, @NonNull List<Service> services,
            @Nullable ContextHubClientCallback callback) {
        Objects.requireNonNull(manager);
        Objects.requireNonNull(info);
        Objects.requireNonNull(services);
        mCallbackHandler = new ChreCallbackHandler(serverNanoappId, callback);
        mContextHubClient = manager.createClient(info, mCallbackHandler);
        mServerNanoappId = serverNanoappId;
        mChannelOutput = new ChreChannelOutput(mContextHubClient, serverNanoappId);
        mChannel = new Channel(mChannelOutput.getChannelId(), mChannelOutput);
        mRpcClient = Client.create(List.of(mChannel), services);
        mCallbackHandler.lateInit(mRpcClient, mChannelOutput);
    }

    /**
     * Initializes the helper
     *
     * Use this constructor for non-persistent clients using intents.
     *
     * The application code must explicitly call the callbacks of ChreCallbackHandler.
     * The handler can be retrieved by calling getCallbackHandler().
     *
     * @param contextHubClient The context hub client providing the RPC server nanoapp
     * @param serverNanoappId  The ID of the RPC server nanoapp
     * @param services         The list of services provided by the server
     */
    public ChreRpcClient(@NonNull ContextHubClient contextHubClient, long serverNanoappId,
            @NonNull List<Service> services) {
        mContextHubClient = Objects.requireNonNull(contextHubClient);
        Objects.requireNonNull(services);
        mServerNanoappId = serverNanoappId;
        mChannelOutput = new ChreChannelOutput(contextHubClient, serverNanoappId);
        mChannel = new Channel(mChannelOutput.getChannelId(), mChannelOutput);
        mRpcClient = Client.create(List.of(mChannel), services);
        mCallbackHandler = new ChreCallbackHandler(serverNanoappId, null);
        mCallbackHandler.lateInit(mRpcClient, mChannelOutput);
    }

    /**
     * Returns a callback handler.
     *
     * Pass each callback invocation to the corresponding method in the handler public APIs.
     */
    public ChreCallbackHandler getCallbackHandler() {
        return mCallbackHandler;
    }

    /**
     * Returns the context hub client.
     */
    public ContextHubClient getContextHubClient() {
        return mContextHubClient;
    }

    /**
     * Shorthand for closing the underlying ContextHubClient.
     */
    public void close() {
        mContextHubClient.close();
    }

    /**
     * Returns a MethodClient.
     *
     * Use the client to invoke the service.
     *
     * @param methodName the method name as "package.Service.Method" or "package.Service/Method"
     * @return The MethodClient instance
     */
    public MethodClient getMethodClient(String methodName) {
        return mRpcClient.method(mChannel.id(), methodName);
    }


    /**
     * Returns whether the nanoapp supports the service.
     *
     * @param state   A nanoapp state
     * @param id      ID of the service
     * @param version Version of the service
     * @return whether the nanoapp provides the service at the given ID and version
     */
    public boolean hasService(NanoAppState state, long id, int version) {
        if (state.getNanoAppId() != mServerNanoappId) {
            return false;
        }

        for (NanoAppRpcService service : state.getRpcServices()) {
            if (service.getId() == id) {
                return service.getVersion() == version;
            }
        }

        return false;
    }
}
