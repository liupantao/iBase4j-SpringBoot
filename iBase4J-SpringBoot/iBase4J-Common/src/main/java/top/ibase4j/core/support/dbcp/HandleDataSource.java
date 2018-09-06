package top.ibase4j.core.support.dbcp;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import top.ibase4j.core.util.DataUtil;
import top.ibase4j.core.util.PropertiesUtil;

/**
 * @author ShenHuaJie
 * @version 2016年5月20日 下午3:18:04
 */
public class HandleDataSource {
    private static final Logger logger = LogManager.getLogger();
    /**  */
    static Map<String, List<String>> METHODTYPE = new HashMap<String, List<String>>();
    // 数据源名称线程池
    private static final ThreadLocal<String> holder = new ThreadLocal<String>();

    public static void putDataSource(String datasource) {
        holder.set(datasource);
    }

    public static String getDataSource() {
        return holder.get();
    }

    public static void clear() {
        holder.remove();
    }

    public static void setDataSource(String service, String method) {
        logger.info(service + "." + method + "=>start...");
        if (DataUtil.isNotEmpty(PropertiesUtil.getString("druid.reader.url"))) {
            try {
                L: for (String key : METHODTYPE.keySet()) {
                    for (String type : METHODTYPE.get(key)) {
                        if (method.startsWith(type)) {
                            logger.info(service + "." + method + "=>" + key);
                            putDataSource(key);
                            break L;
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("", e);
                putDataSource("write");
            }
        }
    }
}
