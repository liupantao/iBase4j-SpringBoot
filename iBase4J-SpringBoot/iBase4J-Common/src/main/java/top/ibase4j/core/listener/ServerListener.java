package top.ibase4j.core.listener;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author ShenHuaJie
 * @since 2018年4月21日 下午12:49:47
 */
@Deprecated
public class ServerListener implements ServletContextListener {
    protected final Logger logger = LogManager.getLogger();
    @Override
    public void contextInitialized(ServletContextEvent contextEvent) {
        logger.info("=================================");
        logger.info("系统[{}]启动完成!!!", contextEvent.getServletContext().getServletContextName());
        logger.info("=================================");
    }

    @Override
    public void contextDestroyed(ServletContextEvent contextEvent) {
    }
}
