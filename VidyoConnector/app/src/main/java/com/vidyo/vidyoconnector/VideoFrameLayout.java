/**
 * {file:
 * {name: VideoFrameLayout.java}
 * {description: .}
 * {copyright:
 * (c) 2016-2018 Vidyo, Inc.,
 * 433 Hackensack Avenue, 7th Floor,
 * Hackensack, NJ  07601.
 * <p>
 * All rights reserved.
 * <p>
 * The information contained herein is proprietary to Vidyo, Inc.
 * and shall not be reproduced, copied (in whole or in part), adapted,
 * modified, disseminated, transmitted, transcribed, stored in a retrieval
 * system, or translated into any language in any form by any means
 * without the express written consent of Vidyo, Inc.
 * **** CONFIDENTIAL *****
 * }
 * }
 */
package com.vidyo.vidyoconnector;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.FrameLayout;

public class VideoFrameLayout extends FrameLayout {

    private static final float SCROLL_THRESHOLD = 10;

    private IVideoFrameListener mListener;

    private float mDownX;
    private float mDownY;

    private boolean isOnClick;

    public VideoFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public void Register(IVideoFrameListener listener) {
        mListener = listener;
    }

    // When this view is tapped, need to notify the the MainActivity. Need to
    // differentiate between a tap versus other touch events.
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        switch (ev.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                mDownX = ev.getX();
                mDownY = ev.getY();
                isOnClick = true;
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                if (isOnClick) {
                    // call back to the MainActivity
                    mListener.onVideoFrameClicked();
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (isOnClick && (Math.abs(mDownX - ev.getX()) > SCROLL_THRESHOLD || Math.abs(mDownY - ev.getY()) > SCROLL_THRESHOLD)) {
                    isOnClick = false;
                }
                break;
            default:
                break;
        }
        return super.dispatchTouchEvent(ev);
    }
}