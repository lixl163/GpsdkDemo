package com.gpsdk.demo.gpsdkdemo;

import android.app.Application;
import android.content.Context;

/**
 * Created by Administrator
 *
 * @author 猿史森林
 *         Date: 2017/11/28
 *         Class description:
 */
public class App extends Application {

    private static Context mContext;

    @Override
    public void onCreate() {
        mContext = getApplicationContext();
    }

    public static Context getContext() {
        return mContext;
    }
}
