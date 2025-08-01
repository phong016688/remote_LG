/*
 * AirPlayServiceConfig
 * Connect SDK
 * 
 * Copyright (c) 2020 LG Electronics.
 * Created by Seokhee Lee on 28 Aug 2020
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.connectsdk.service.config;

import org.json.JSONException;
import org.json.JSONObject;

public class AirPlayServiceConfig extends ServiceConfig {
    public static final String KEY_AUTH_TOKEN = "authToken";
    String authToken;

    public AirPlayServiceConfig(JSONObject json) {
        super(json);

        authToken = json.optString(KEY_AUTH_TOKEN);
    }

    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
        notifyUpdate();
    }

    @Override
    public JSONObject toJSONObject() {
        JSONObject jsonObj = super.toJSONObject();

        try {
            jsonObj.put(KEY_AUTH_TOKEN, authToken);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return jsonObj;
    }

}
