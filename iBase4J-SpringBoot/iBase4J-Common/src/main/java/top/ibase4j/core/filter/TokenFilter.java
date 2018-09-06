package top.ibase4j.core.filter;

import java.io.IOException;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import top.ibase4j.core.Constants;
import top.ibase4j.core.support.HttpCode;
import top.ibase4j.core.support.Token;
import top.ibase4j.core.util.DataUtil;
import top.ibase4j.core.util.FileUtil;
import top.ibase4j.core.util.PropertiesUtil;
import top.ibase4j.core.util.TokenUtil;
import top.ibase4j.core.util.WebUtil;

/**
 * APP登录TOKEN过滤器, expire有效期(秒),默认永远有效
 * @author ShenHuaJie
 * @since 2017年3月19日 上午10:21:59
 */
public class TokenFilter implements Filter {
    private Logger logger = LogManager.getLogger();
    private String expire;
    // 白名单
    private List<String> whiteUrls;
    private int _size = 0;

    @Override
    public void init(FilterConfig config) throws ServletException {
        logger.info("init TokenFilter..");
        // 读取文件
        String path = CsrfFilter.class.getResource("/").getFile();
        whiteUrls = FileUtil.readFile(path + "white/tokenWhite.txt");
        _size = null == whiteUrls ? 0 : whiteUrls.size();
        expire = config.getInitParameter("expire");
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest)servletRequest;
        boolean filter = DataUtil.isEmpty(PropertiesUtil.getString("token.filter.test"));
        String token = request.getHeader("UUID");
        logger.info("UUID==>" + token);
        if (StringUtils.isNotBlank(token)) {
            try {
                Token tokenInfo = TokenUtil.getTokenInfo(token);
                if (tokenInfo != null) {
                    request.setAttribute(Constants.CURRENT_USER, tokenInfo.getValue());
                    if (DataUtil.isNotEmpty(expire)) {
                        if (System.currentTimeMillis() - tokenInfo.getTime() > Long.valueOf(expire) * 1000) {
                            logger.info("UUID==>{}过期", token);
                            request.removeAttribute(Constants.CURRENT_USER);
                        }
                    }
                } else {
                    logger.info("UUID==>{}无效", token);
                }
            } catch (Exception e) {
                logger.error("token检查发生异常:", e);
            }
        }
        String url = request.getRequestURI();
        if (WebUtil.isWhiteRequest(url, _size, whiteUrls)) {
            chain.doFilter(request, response);
        } else if (DataUtil.isEmpty(request.getAttribute(Constants.CURRENT_USER)) && filter) {
            WebUtil.write((HttpServletResponse)response, HttpCode.UNAUTHORIZED.value(), HttpCode.UNAUTHORIZED.msg());
        } else {
            chain.doFilter(request, response);
        }
    }

    @Override
    public void destroy() {
        logger.info("destroy TokenFilter.");
    }
}
