package top.ibase4j.core.interceptor.provider;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import top.ibase4j.core.base.provider.BaseProvider;
import top.ibase4j.core.base.provider.Parameter;
import top.ibase4j.model.SysEvent;

/**
 * 日志拦截器
 *
 * @author ShenHuaJie
 * @version 2016年6月14日 下午6:18:46
 */
public class EventInterceptor extends top.ibase4j.core.interceptor.EventInterceptor {
    @Autowired
    @Qualifier("sysProvider")
    protected BaseProvider sysProvider;

    @Override
    protected void saveEvent(SysEvent record) {
        sysProvider.execute(new Parameter("sysEventService", "update", record));
    }
}
