/*
 *    Copyright 2018 APPNEXUS INC
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.appnexus.opensdk;

import com.appnexus.opensdk.utils.Clog;
import com.appnexus.opensdk.utils.Settings;
import com.appnexus.opensdk.viewability.ANOmidViewabilty;

import org.json.JSONObject;


class VideoImplementation {

    private AdWebView adWebView;
    private String vastXML;
    private boolean videoComplete = false;
    private boolean adReady = false;


    public VideoImplementation(AdWebView adWebView) {
        this.adWebView = adWebView;
    }

    void webViewFinishedLoading() {
        createVastPlayerWithContent();
    }


    protected void dispatchNativeCallback(String url) {
        url = url.replaceFirst("video://", "");

        try {
            JSONObject videoObject = new JSONObject(url);

            String eventName = videoObject.getString("event");
            JSONObject paramsDictionary = videoObject.getJSONObject("params");
            if (eventName.equals("adReady")) {
                adWebView.success();
                adReady = true;
            } else if (eventName.equals("videoStart")) {
                //
            } else if (eventName.equals("video-error") || eventName.equals("Timed-out")) {
                handleVideoError();
            } else if (eventName.equals("video-complete")) {
                videoComplete = true;
                stopOMIDAdSession();
            } else {
                Clog.e(Clog.videoLogTag, "Error: Unhandled event::" + url);
                return;
            }
        } catch (Exception e) {
            Clog.e(Clog.videoLogTag, "Exception: JsonError::" + url);
            return;
        }
    }

    private void handleVideoError() {
        if (adReady && !videoComplete) {
            //AdReady has been fired but video errored before Playback completion
            stopOMIDAdSession();
            adWebView.adView.getAdDispatcher().toggleAutoRefresh();
        } else {
            // AdReady has not been fired yet continue to do waterfall
            adWebView.fail();
        }
    }

    void stopOMIDAdSession(){
        adWebView.omidAdSession.stopAdSession();
    }


    protected void createVastPlayerWithContent() {
        String options = ANVideoPlayerSettings.getVideoPlayerSettings().fetchBannerSettings();
        String inject = String.format("javascript:window.createVastPlayerWithContent('%s','%s')",
                vastXML, options);
        adWebView.injectJavaScript(inject);
    }

    protected void fireViewableChangeEvent(){
        if (!adReady) return;
        boolean isCurrentlyViewable = adWebView.isVideoViewable();
        String inject = String.format("javascript:window.viewabilityUpdate('%s')",
                isCurrentlyViewable);
        adWebView.injectJavaScript(inject);
    }


    public void setVASTXML(String vastXML) {
        this.vastXML = vastXML;
    }
}