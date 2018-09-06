package top.ibase4j.core.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import javax.servlet.ServletRequest;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.NamedThreadLocal;
import org.springframework.web.util.WebUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;

import top.ibase4j.core.Constants;

/**
 * Web层辅助类
 *
 * @author ShenHuaJie
 * @version 2016年4月2日 下午4:19:28
 */
public final class WebUtil {
    private WebUtil() {
    }

    public static ThreadLocal<HttpServletRequest> REQUEST = new NamedThreadLocal<HttpServletRequest>(
            "ThreadLocalRequest");

    private static Logger logger = LogManager.getLogger();

    /**
     * 获取指定Cookie的值
     *
     * @param request
     * @param cookieName
     *            cookie名字
     * @param defaultValue
     *            缺省值
     * @return
     */
    public static final String getCookieValue(HttpServletRequest request, String cookieName, String defaultValue) {
        Cookie cookie = WebUtils.getCookie(request, cookieName);
        if (cookie == null) {
            return defaultValue;
        }
        return cookie.getValue();
    }

    /** 获取当前用户 */
    public static final Object getCurrentUser(HttpServletRequest request) {
        try {
            HttpSession session = request.getSession();
            if (null != session) {
                return session.getAttribute(Constants.CURRENT_USER);
            }
        } catch (Exception e) {
            logger.error(ExceptionUtil.getStackTraceAsString(e));
        }
        return null;
    }

    /** 保存当前用户 */
    public static final void saveCurrentUser(HttpServletRequest request, Object user) {
        setSession(request, Constants.CURRENT_USER, user);
    }

    /**
     * 将一些数据放到Session中,以便于其它地方使用
     * 比如Controller,使用时直接用HttpSession.getAttribute(key)就可以取到
     */
    public static final void setSession(HttpServletRequest request, String key, Object value) {
        HttpSession session = request.getSession();
        if (null != session) {
            session.setAttribute(key, value);
        }
    }

    /**
     * 直接用HttpSession.getAttribute(key)取
     */
    public static final Object getSession(HttpServletRequest request, String key) {
        HttpSession session = request.getSession();
        if (null != session) {
            return session.getAttribute(key);
        }
        return null;
    }

    /** 移除当前用户 */
    public static final void removeCurrentUser(HttpServletRequest request) {
        request.getSession().removeAttribute(Constants.CURRENT_USER);
    }

    /**
     * 获得国际化信息
     *
     * @param key
     *            键
     * @param request
     * @return
     */
    public static final String getApplicationResource(String key, HttpServletRequest request) {
        ResourceBundle resourceBundle = ResourceBundle.getBundle("ApplicationResources", request.getLocale());
        return resourceBundle.getString(key);
    }

    /**
     * 获得参数Map
     *
     * @param request
     * @return
     */
    public static final Map<String, Object> getParameterMap(HttpServletRequest request) {
        return WebUtils.getParametersStartingWith(request, null);
    }

    public static String getRequestBody(ServletRequest request) {
        String str, body = (String)request.getAttribute(Constants.REQUEST_BODY);
        if (DataUtil.isEmpty(body)) {
            body = "";
            try {
                BufferedReader br = request.getReader();
                while ((str = br.readLine()) != null) {
                    body += str;
                }
                logger.info("request body===>{}", body);
                request.setAttribute(Constants.REQUEST_BODY, body);
            } catch (Exception e) {
                logger.error(ExceptionUtil.getStackTraceAsString(e));
            }
        }
        return body;
    }

    public static Map<String, Object> getRequestParam(String param) {
        Map<String, Object> paramMap = InstanceUtil.newHashMap();
        if (null != param) {
            String[] params = param.split("&");
            for (String param2 : params) {
                String[] p = param2.split("=");
                if (p.length == 2) {
                    paramMap.put(p[0], p[1]);
                }
            }
        }
        return paramMap;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> getParameter(HttpServletRequest request) {
        String body = getRequestBody(request);
        if (DataUtil.isNotEmpty(body)) {
            try {
                return JSON.parseObject(body, Map.class);
            } catch (Exception e) {
                logger.error(ExceptionUtil.getStackTraceAsString(e));
                try {
                    return XmlUtil.parseXml2Map(body);
                } catch (Exception e1) {
                    logger.error(ExceptionUtil.getStackTraceAsString(e));
                    return getRequestParam(body);
                }
            }
        }
        return getParameterMap(request);
    }

    public static <T> T getParameter(HttpServletRequest request, Class<T> cls) {
        String body = getRequestBody(request);
        if (DataUtil.isNotEmpty(body)) {
            try {
                return JSON.parseObject(body, cls);
            } catch (Exception e) {
                logger.error(ExceptionUtil.getStackTraceAsString(e));
                try {
                    return InstanceUtil.parse(body, cls);
                } catch (Exception e1) {
                    logger.error(ExceptionUtil.getStackTraceAsString(e));
                }
            }
        }
        return InstanceUtil.transMap2Bean(getParameterMap(request), cls);
    }

    /** 获取客户端IP */
    public static final String getHost(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (DataUtil.isEmpty(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (DataUtil.isEmpty(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (DataUtil.isEmpty(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (DataUtil.isEmpty(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (DataUtil.isEmpty(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (DataUtil.isEmpty(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.indexOf(",") > 0) {
            logger.info(ip);
            // 对于通过多个代理的情况，第一个IP为客户端真实IP,多个IP按照','分割
            String[] ips = ip.split(",");
            for (String ip2 : ips) {
                String strIp = ip2;
                if (!"unknown".equalsIgnoreCase(strIp)) {
                    ip = strIp;
                    break;
                }
            }
        }
        if ("127.0.0.1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip)) {
            InetAddress inet = null;
            try { // 根据网卡取本机配置的IP
                inet = InetAddress.getLocalHost();
            } catch (UnknownHostException e) {
                logger.error("getCurrentIP", e);
            }
            if (inet != null) {
                ip = inet.getHostAddress();
            }
        }
        logger.info("getRemoteAddr ip: " + ip);
        return ip;
    }

    /** 设置文件名 */
    public static void setResponseFileName(HttpServletRequest request, HttpServletResponse response,
        String displayName) {
        String userAgent = request.getHeader("User-Agent");
        boolean isIE = false;
        if (userAgent != null && userAgent.toLowerCase().contains("msie")) {
            isIE = true;
        }
        String displayName2;
        try {
            if (isIE) {
                displayName2 = URLEncoder.encode(displayName, "UTF-8");
                displayName2 = displayName2.replaceAll("\\+", "%20");// 修正URLEncoder将空格转换成+号的BUG
                response.setHeader("Content-Disposition", "attachment;filename=" + displayName2);
            } else {
                displayName2 = new String(displayName.getBytes("UTF-8"), "ISO8859-1");
                response.setHeader("Content-Disposition", "attachment;filename=\"" + displayName2 + "\"");// firefox空格截断
            }
            String extStr = displayName2.substring(displayName2.indexOf(".") + 1);
            if ("xls".equalsIgnoreCase(extStr)) {
                response.setContentType("application/vnd.ms-excel charset=UTF-8");
            } else {
                response.setContentType("application/octet-stream");
            }
        } catch (UnsupportedEncodingException e) {
            logger.error("设置文件名发生错误", e);
        }
    }

    /** 判断是否是白名单 */
    public static boolean isWhiteRequest(String url, int size, List<String> whiteUrls) {
        if (url == null || "".equals(url) || size == 0) {
            return true;
        } else {
            url = url.toLowerCase();
            for (String urlTemp : whiteUrls) {
                if (url.indexOf(urlTemp.toLowerCase()) > -1) {
                    return true;
                }
            }
        }
        return false;
    }

    /** 写出响应 */
    public static boolean write(HttpServletResponse response, Integer code, String msg) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        Map<String, Object> modelMap = InstanceUtil.newLinkedHashMap();
        modelMap.put("code", code.toString());
        modelMap.put("msg", msg);
        modelMap.put("timestamp", System.currentTimeMillis());
        logger.info("response===>" + JSON.toJSON(modelMap));
        response.getOutputStream().write(JSON.toJSONBytes(modelMap, SerializerFeature.DisableCircularReferenceDetect));
        return false;
    }
}
