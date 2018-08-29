package com.ssf.chen.eventbus;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by CHEN on 2018/8/13.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@Documented
public @interface Subscribe {
    // 线程  默认POSTING线程
    ThreadMode threadMode() default ThreadMode.POSTING;
    //粘性事件
    boolean sticky() default false;
    //优先级
    int priority() default 0;
}
