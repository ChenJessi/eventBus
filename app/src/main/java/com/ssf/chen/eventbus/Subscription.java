package com.ssf.chen.eventbus;

/**
 * Created by CHEN on 2018/8/13.
 * 订阅者和订阅方法
 */

final class Subscription {
    final Object subscriber;                //订阅者
    final SubscriberMethod subscriberMethod;        //订阅元素

    volatile boolean active;

    Subscription(Object subscriber, SubscriberMethod subscriberMethod) {
        this.subscriber = subscriber;
        this.subscriberMethod = subscriberMethod;
        active = true;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Subscription){
            Subscription subscription = (Subscription) obj;
            return subscriber == subscription.subscriber
                    && subscriberMethod.equals(subscription.subscriberMethod);
        }else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return subscriber.hashCode() + subscriberMethod.methodString.hashCode();
    }
}
