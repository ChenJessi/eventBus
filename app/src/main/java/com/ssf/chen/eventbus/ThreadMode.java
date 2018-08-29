package com.ssf.chen.eventbus;

/**
 * Created by CHEN on 2018/8/13.
 * 线程枚举
 */

public enum  ThreadMode {
    POSTING,      // 默认线程     发送接收在同一线程

    MAIN,           //主线程

    BACKGROUND,         //子线程接收

    ASYNC               //新开一个线程    执行耗时操作
}
