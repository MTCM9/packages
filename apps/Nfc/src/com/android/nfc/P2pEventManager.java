/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.nfc;

import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;

/**
 * Manages vibration, sound and animation for P2P events.
 */
public class P2pEventManager implements P2pEventListener, SendUi.Callback {
    static final String TAG = "NfcP2pEventManager";
    static final boolean DBG = true;

    static final long[] VIBRATION_PATTERN = {0, 100, 10000};

    final Context mContext;
    final NfcService mNfcService;
    final P2pEventListener.Callback mCallback;
    final Vibrator mVibrator;
    final NotificationManager mNotificationManager;
    final SendUi mSendUi;

    // only used on UI thread
    boolean mSending;
    boolean mNdefSent;
    boolean mNdefReceived;

    public P2pEventManager(Context context, P2pEventListener.Callback callback) {
        mNfcService = NfcService.getInstance();
        mContext = context;
        mCallback = callback;
        mVibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        mNotificationManager = (NotificationManager) mContext.getSystemService(
                Context.NOTIFICATION_SERVICE);

        mSending = false;
        mSendUi = new SendUi(context, this);
    }

    @Override
    public void onP2pInRange() {
        mNfcService.playSound(NfcService.SOUND_START);
        mNdefSent = false;
        mNdefReceived = false;

        mVibrator.vibrate(VIBRATION_PATTERN, -1);
        mSendUi.takeScreenshot();
    }

    @Override
    public void onP2pSendConfirmationRequested() {
        mSendUi.showPreSend();
    }

    @Override
    public void onP2pSendComplete() {
        mNfcService.playSound(NfcService.SOUND_END);
        mVibrator.vibrate(VIBRATION_PATTERN, -1);
        mSendUi.showPostSend();
        mSending = false;
        mNdefSent = true;
    }

    @Override
    public void onP2pReceiveComplete() {
        mVibrator.vibrate(VIBRATION_PATTERN, -1);
        mNfcService.playSound(NfcService.SOUND_END);
        // TODO we still don't have a nice receive solution
        // The sanest solution right now is just to scale back up what we had
        // and start the new activity. It is not perfect, but at least it is
        // consistent behavior. All other variants involve making the old
        // activity screenshot disappear, and then removing the animation
        // window hoping the new activity has started by then. This just goes
        // wrong too often and can looks weird.
        mSendUi.finish(SendUi.FINISH_SCALE_UP);
        mNdefReceived = true;
    }

    @Override
    public void onP2pOutOfRange() {
        if (mSending) {
            mNfcService.playSound(NfcService.SOUND_ERROR);
            mSending = false;
        }
        if (!mNdefSent && !mNdefReceived) {
            mSendUi.finish(SendUi.FINISH_SCALE_UP);
        }
    }

    @Override
    public void onSendConfirmed() {
        if (!mSending) {
            mSendUi.showStartSend();
            mCallback.onP2pSendConfirmed();
        }
        mSending = true;

    }
}
