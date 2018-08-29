package com.ssf.chen.eventbus;

import android.os.Looper;
import android.util.Log;

import com.ssf.chen.eventbus.utils.EventBusException;
import com.ssf.chen.eventbus.utils.SubscriberMethodFinder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by CHEN on 2018/8/13.
 * 主类
 */

public class EventBus {
    public static final String TAG = "EventBus";

    private static EventBus ourInstance;
    //订阅者
    private final Map<Class<?>, CopyOnWriteArrayList<Subscription>> subscriptionsByEventType;
    //方法参数的class
    private final Map<Object, List<Class<?>>> typesBySubscriber;
    //粘性事件
    private final Map<Class<?>,Object> stickyEvents;

    private final SubscriberMethodFinder subscriberMethodFinder;
    //每个线程内的一个队列
    private final ThreadLocal<PostingThreadState> currentPostingThreadState = new ThreadLocal<PostingThreadState>(){
        @Override
        protected PostingThreadState initialValue() {
            return new PostingThreadState();
        }
    };

    public static EventBus getDefault(){
        if (ourInstance == null){
            synchronized (EventBus.class){
                if (ourInstance == null){
                    ourInstance = new EventBus();
                }
            }
        }
        return ourInstance;
    }
    private EventBus(){
        //初始化数据
        subscriptionsByEventType = new HashMap<>();
        typesBySubscriber = new HashMap<>();
        stickyEvents = new ConcurrentHashMap<>();
        subscriberMethodFinder = new SubscriberMethodFinder();
    }


    public void register(Object subscriber){
        //找到所有方法
        Class<?>  subscriberClass =  subscriber.getClass();
        List<SubscriberMethod> subscriberMethods = subscriberMethodFinder.findSubscriberMethods(subscriberClass);

        //2.保存订阅  subscriber();
        synchronized (this){
            for (SubscriberMethod subscriberMethod :subscriberMethods){
                subscribe(subscriber, subscriberMethod);
            }
        }
    }

    /**
     * 保存订阅方法
     * @param subscriber
     * @param subscriberMethod
     */
    private void subscribe(Object subscriber, SubscriberMethod subscriberMethod){
        //保存数据，如果重复  抛出异常
        Class<?> eventType = subscriberMethod.eventType;
        Subscription newSubscription = new Subscription(subscriber, subscriberMethod);
        CopyOnWriteArrayList<Subscription> subscriptions = subscriptionsByEventType.get(eventType);
        if (subscriptions == null){
            subscriptions = new CopyOnWriteArrayList<>();
            subscriptionsByEventType.put(eventType, subscriptions);
        }else {
            if (subscriptions.contains(subscriber)){
                throw new EventBusException("Subscriber " + subscriber.getClass() + " already registered to event "
                        + eventType);
            }
        }
        int size = subscriptions.size();
        for (int i = 0;i<= size; i++){
            if (i == size || subscriberMethod.priority > subscriptions.get(i).subscriberMethod.priority){
                subscriptions.add(i,newSubscription);
                break;
            }
        }
        List<Class<?>> subscribedEvents = typesBySubscriber.get(subscriber);
        if (subscribedEvents == null){
            subscribedEvents = new ArrayList<>();
            typesBySubscriber.put(subscriber,subscribedEvents);
        }
        subscribedEvents.add(eventType);


        //执行粘性事件
        if (subscriberMethod.sticky){
            Set<Map.Entry<Class<?>, Object>> entries = stickyEvents.entrySet();
            for (Map.Entry<Class<?>, Object> entry : entries){
                Class<?> candidateEventType = entry.getKey();
                if (eventType.isAssignableFrom(candidateEventType)){
                    Object stickyEvent = entry.getValue();
                    checkPostStickyEventToSubscription(newSubscription, stickyEvent);
                }
            }
        }
    }

    private void checkPostStickyEventToSubscription(Subscription newSubscription, Object stickyEvent){
        if (stickyEvent != null){
            //If the subscriber is trying to abort the event, it will fail (event is not tracked in posting state)
            // --> Strange corner case, which we don't take care of here.
            postToSubscription(newSubscription,stickyEvent, Looper.getMainLooper() == Looper.myLooper());
        }
    }

    private void postToSubscription(Subscription newSubscription, Object stickyEvent, boolean b) {
        InvokeHelper.getDefault().post(newSubscription, stickyEvent, b);
    };

    /**
     * 解绑订阅
     */
    public synchronized void unRegister(Object subscriber){
        List<Class<?>> subscribedTypes = typesBySubscriber.get(subscriber);
        if (subscribedTypes != null){
            for (Class<?> eventType : subscribedTypes){
                unsubscribeByEventType(subscriber,eventType);
            }
            typesBySubscriber.remove(subscriber);
        }else {
            Log.w(TAG, "Subscriber to unregister was not registered before: " + subscriber.getClass());
        }
    }

    private void unsubscribeByEventType(Object subscriber, Class<?> eventType) {
        List<Subscription> subscriptions = subscriptionsByEventType.get(eventType);
        if (subscriptions == null){
            int size = subscriptions.size();
            for (int i = 0;i < size; i++){
                Subscription subscription = subscriptions.get(i);
                if (subscription.subscriber == subscriber){
                    subscription.active = false;
                    subscriptions.remove(i);
                    size--;
                    i--;
                }
            }
        }
    }

    /**
     * 发送事件
     * @param event
     */
    public void post(Object event){
        //1.放入执行队列
        PostingThreadState postingState = currentPostingThreadState.get();
        List<Object> eventQueue = postingState.eventQueue;
        eventQueue.add(event);

        if (!postingState.isPosting){
            postingState.isMainThread = Looper.getMainLooper() == Looper.myLooper();
            postingState.isPosting = true;
            if (postingState.canceled) {
                throw new EventBusException("Internal error. Abort state was not reset");
            }
           try {
                while (!eventQueue.isEmpty()){
                   postSingleEvent(eventQueue.remove(0),postingState);
               }
           }finally {
               postingState.isPosting = false;
               postingState.isMainThread = false;
           }
        }
    }

    private void postSingleEvent(Object event, PostingThreadState postingState) throws Error{
        Class<?> eventClass = event.getClass();
         postSingleEventForEventType(event, postingState, eventClass);
    }

    private boolean postSingleEventForEventType(Object event, PostingThreadState postingState, Class<?> eventClass) {
        CopyOnWriteArrayList<Subscription> subscriptions;
        synchronized(this){
            subscriptions = subscriptionsByEventType.get(eventClass);
        }
        if (subscriptions!=null && !subscriptions.isEmpty()){
            for (Subscription subscription : subscriptions){
                postingState.event = event;
                postingState.subscription = subscription;
                boolean aborted = false;
                try {
                    postToSubscription(subscription,event,postingState.isMainThread);
                    aborted = postingState.canceled;
                } finally {
                    postingState.event = null;
                    postingState.subscription = null;
                    postingState.canceled = false;
                }
                if (aborted){
                    break;
                }
            }
            return true;
        }
        return false;
    }
    public void postSticky(Object event){
        //加入粘性缓存 stickyEvents
        synchronized (stickyEvents){
            stickyEvents.put(event.getClass(), event);
        }
        post(event);
    }

    public void cancelEventDelivery(Object event){
        PostingThreadState postingState = currentPostingThreadState.get();
        if (!postingState.isPosting){
            throw new EventBusException(
                    "This method may only be called from inside event handling methods on the posting thread");
        }else if (event == null){
            throw new EventBusException("Event may not be null");
        } else if (postingState.event != event) {
            throw new EventBusException("Only the currently handled event may be aborted");
        } else if(postingState.subscription.subscriberMethod.threadMode != ThreadMode.POSTING){
            throw new EventBusException(" event handlers may only abort the incoming event");
        }
        postingState.canceled = true;
    }
    /**
     * Gets the most recent sticky event for the given type.
     *
     * @see #postSticky(Object)
     */
    public <T> T getStickyEvent(Class<T> eventType ){
        synchronized (stickyEvents) {
            return eventType.cast(stickyEvents.get(eventType));
        }
    }

    /**
     * Remove and gets the recent sticky event for the given event type.
     *
     * @see #postSticky(Object)
     */
    public <T> T removeStickyEvent(Class<T> eventType){
        synchronized (stickyEvents) {
            return eventType.cast(stickyEvents.remove(eventType));
        }
    }

    /**
     * Removes the sticky event if it equals to the given event.
     *
     * @return true if the events matched and the sticky event was removed.
     */
    public boolean removeStickyEvent(Object event){
        synchronized (stickyEvents) {
            Class<?> eventType = event.getClass();
            Object existingEvent = stickyEvents.get(eventType);
            if (event.equals(existingEvent)){
                stickyEvents.remove(eventType);
                return true;
            }else {
                return false;
            }
        }
    }
    /**
     * Removes all sticky events.
     */
    public void removeAllStickyEvents() {
        synchronized (stickyEvents) {
            stickyEvents.clear();
        }
    }
    private final static class PostingThreadState{
        final List<Object> eventQueue = new ArrayList<Object>();
        boolean isPosting;
        boolean isMainThread;
        Subscription subscription;
        Object event;
        boolean canceled;
    }

}
