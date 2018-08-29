package com.ssf.chen.eventbus.utils;

import com.ssf.chen.eventbus.Subscribe;
import com.ssf.chen.eventbus.SubscriberMethod;
import com.ssf.chen.eventbus.ThreadMode;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by CHEN on 2018/8/14.
 * 订阅方法查找类
 */

public class SubscriberMethodFinder {
    private static final int MODIFIERS_IGNORE = Modifier.ABSTRACT | Modifier.STATIC;

    //每个class订阅元素
    private final Map<Class<?>, List<SubscriberMethod>> METHOD_CACHE = new ConcurrentHashMap<>();


    /**
     *
     * @param subscriberClass
     * @return  class所有订阅
     */
    public List<SubscriberMethod> findSubscriberMethods(Class<?> subscriberClass){
        List<SubscriberMethod> subscriberMethods =  METHOD_CACHE.get(subscriberClass);
        if (subscriberMethods != null) {
            return subscriberMethods;
        }
        subscriberMethods = findUsingReflection(subscriberClass);
        if (subscriberMethods.isEmpty()){
            throw new EventBusException("Subscriber " + subscriberClass
                    + " and its super classes have no public methods with the @Subscribe annotation");
        }else {
            METHOD_CACHE.put(subscriberClass, subscriberMethods);
            return subscriberMethods;
        }
    }

    /**
     * 查找class所有订阅的方法
     * @param subscriberClass
     * @return
     */
    private List<SubscriberMethod> findUsingReflection(Class<?> subscriberClass){
        List<SubscriberMethod> subscriberMethods = new ArrayList<>();
        Class<?> clazz = subscriberClass;
        boolean skipSuperClasses = false;
        while (clazz != null){
            Method[] methods;
            try {
                methods = subscriberClass.getDeclaredMethods();             //获取class里所有方法
            }catch (Throwable th){
                methods = subscriberClass.getMethods();
                skipSuperClasses = true;
            }

            for (Method method : methods){
                int modifiers =method.getModifiers();           //方法的修饰符
                //修饰符是public   并且 不是  abstract  和  static 的方法
                if ((modifiers & Modifier.PUBLIC) != 0 && (modifiers & MODIFIERS_IGNORE) == 0){
                    //获取该方法的参数
                    Class<?>[] parameterTypes = method.getParameterTypes();
                    if (parameterTypes.length == 1){
                        //获取该方法的注解
                        Subscribe subscribeAnnotation = method.getAnnotation(Subscribe.class);
                        if (subscribeAnnotation != null){

                            Class<?> eventType = parameterTypes[0];
                            //获取该方法的线程
                            ThreadMode threadMode = subscribeAnnotation.threadMode();
                            subscriberMethods.add(new SubscriberMethod(method,threadMode,eventType,subscribeAnnotation.priority(),subscribeAnnotation.sticky()));
//
                        }
                    }else  if (method.isAnnotationPresent(Subscribe.class)){    //已被注解的方法  但是参数超过了一个
                        String methodName = method.getDeclaringClass().getName() + "." + method.getName();
                        throw new EventBusException("@Subscribe method " + methodName +
                                "must have exactly 1 parameter but has " + parameterTypes.length);
                    }
                }else if (method.isAnnotationPresent(Subscribe.class)){             //方法被注解  但不是public 或者是abstra  static
                    String methodName = method.getDeclaringClass().getName() + "." + method.getName();
                    throw new EventBusException(methodName +
                            " is a illegal @Subscribe method: must be public, non-static, and non-abstract");
                }
            }
            if (skipSuperClasses){      //如果是父类中的方法
                clazz = null;
            }else {
                clazz = clazz.getSuperclass();  //clazz 取它的父类
                String clazzName = clazz.getName();
                /** Skip system classes, this just degrades performance. */
                /**
                 * 若是系统类
                 */
                if (clazzName.startsWith("java.") || clazzName.startsWith("javax.") || clazzName.startsWith("android.")) {
                    clazz = null;
                }
            }
        }
        return subscriberMethods;
    }
}
