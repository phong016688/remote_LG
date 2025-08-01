/*
 * Copyright (C) 2009 Google Inc.  All rights reserved.
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

package com.mentos_koder.remote;


import com.mentos_koder.exception.PairingException;

/**
 * Listener interface for handling events within a pairing session.
 */
public interface RemoteListener {


    void onConnected();

    void onDisconnected();

    void onVolume();

    void onPerformInputDeviceRole() throws PairingException;

    void onPerformOutputDeviceRole(byte[] gamma)
            throws PairingException;


    void onSessionEnded();


    void onError(String message);

    void onLog(String message);

    void sSLException();

}
