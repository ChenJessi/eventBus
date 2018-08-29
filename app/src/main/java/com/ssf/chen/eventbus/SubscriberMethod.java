package com.ssf.chen.eventbus;

import java.lang.reflect.Method;

/**
 * Created by CHEN on 2018/8/13.
 * 订阅元素类
 */

public class SubscriberMethod {
    public final Method method;
    public final ThreadMode threadMode;
    public final Class<?> eventType;        //参数
    public final int priority;              //优先级
    public final boolean sticky;            //是否是粘性事件

    /**
     * 判断是否有效
     */
    public String methodString;

    public SubscriberMethod(Method method, ThreadMode threadMode, Class<?> eventType, int priority, boolean sticky) {
        this.method = method;
        this.threadMode = threadMode;
        this.eventType = eventType;
        this.priority = priority;
        this.sticky = sticky;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this ){
            return true;
        }else if (obj instanceof SubscriberMethod){
            checkMethodString();
            SubscriberMethod subscriberMethod = (SubscriberMethod) obj;
            subscriberMethod.checkMethodString();
            // Don't use method.equals because of http://code.google.com/p/android/issues/detail?id=7811#c6
            return methodString.equals(subscriberMethod.methodString);
        }else {
            return false;
        }
    }

    private synchronized void checkMethodString() {
        if (methodString == null){
            // Method.toString has more overhead, just take relevant parts of the method
            StringBuilder builder = new StringBuilder(64);
            builder.append(method.getDeclaringClass().getName());   //类名
            builder.append('#').append(method.getName());           //方法名
            builder.append('(').append(eventType.getName());        //参数名
            methodString = builder.toString();
        }
    }

    @Override
    public int hashCode() {
        return method.hashCode();
    }
}
