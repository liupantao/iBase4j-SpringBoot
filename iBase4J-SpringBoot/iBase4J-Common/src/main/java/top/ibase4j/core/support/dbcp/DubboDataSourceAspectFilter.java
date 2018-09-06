package top.ibase4j.core.support.dbcp;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.extension.Activate;
import com.alibaba.dubbo.rpc.Filter;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcException;

/**
 * @author ShenHuaJie
 * @since 2018年4月24日 下午2:13:13
 */
@Activate(group = Constants.PROVIDER)
public class DubboDataSourceAspectFilter implements Filter {
    private static final Logger logger = LogManager.getLogger();
    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        String service = invoker.getInterface().getSimpleName();
        String method = invocation.getMethodName();
        HandleDataSource.setDataSource(service, method);
        Result result = invoker.invoke(invocation);
        logger.info(service + "." + method + "=>end.");
        return result;
    }
}
