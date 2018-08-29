package com.ssf.chen.eventbus;

import android.os.Handler;
import android.os.Looper;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by CHEN on 2018/8/20.
 * 方法执行帮助类
 */

public final class InvokeHelper {
    ;
    private final static ExecutorService DEFAULT_EXECUTOR_SERVICE = Executors.newCachedThreadPool();
    private static InvokeHelper ourInstance;
    private HandlerPoster handlerPoster;

    public static InvokeHelper getDefault() {
        if (ourInstance == null) {
            synchronized (InvokeHelper.class) {
                if (ourInstance == null) {
                    ourInstance = new InvokeHelper();
                }
            }
        }
        return ourInstance;
    }

    private InvokeHelper() {
        handlerPoster = new HandlerPoster(Looper.getMainLooper());
    }

    /**
     * 发送消息
     *
     * @param subscription
     * @param event
     * @param isMainThread
     */
    public void post(final Subscription subscription, final Object event, boolean isMainThread) {
        switch (subscription.subscriberMethod.threadMode) {
            case POSTING:
                //直接执行
                invokeSubscriber(subscription, event);
                break;
            case MAIN:
                if (isMainThread) {
                    invokeSubscriber(subscription, event);
                } else {
                    handlerPoster.post(new Runnable() {
                        @Override
                        public void run() {
                            invokeSubscriber(subscription, event);
                        }
                    });
                }
                break;
            case ASYNC:
                //放在异步线程内执行
                getExecutorService().execute(new Runnable() {
                    @Override
                    public void run() {
                        invokeSubscriber(subscription, event);
                    }
                });
                break;
            case BACKGROUND:
                if (isMainThread){
                    handlerPoster.post(new Runnable() {
                        @Override
                        public void run() {
                            invokeSubscriber(subscription, event);
                        }
                    });
                }else {
                    invokeSubscriber(subscription, event);
                }
                break;
                default:
                    throw new IllegalStateException("Unknown thread mode: " + subscription.subscriberMethod.threadMode);
        }
    }

    /**
     * 发送消息
     *
     * @param subscription
     * @param event
     */
    private void invokeSubscriber(Subscription subscription, final Object event) {
        try {
            subscription.subscriberMethod.method.invoke(subscription.subscriber, event);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Unexpected exception", e);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException("Unexpected exception", e);
        }
    }

    ExecutorService getExecutorService() {
        return DEFAULT_EXECUTOR_SERVICE;
    }

    class HandlerPoster extends Handler {
        HandlerPoster(Looper looper) {
            super(looper);
        }
    }
}
