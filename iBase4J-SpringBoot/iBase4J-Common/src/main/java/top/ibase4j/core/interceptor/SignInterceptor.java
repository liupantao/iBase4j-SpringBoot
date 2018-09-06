package top.ibase4j.core.interceptor;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.net.util.Base64;

import top.ibase4j.core.Constants;
import top.ibase4j.core.support.HttpCode;
import top.ibase4j.core.support.security.coder.MDCoder;
import top.ibase4j.core.support.security.coder.RSACoder;
import top.ibase4j.core.util.CacheUtil;
import top.ibase4j.core.util.DataUtil;
import top.ibase4j.core.util.FileUtil;
import top.ibase4j.core.util.WebUtil;

/**
 * 签名验证
 * @author ShenHuaJie
 * @since 2018年5月12日 下午10:40:38
 */
public class SignInterceptor extends BaseInterceptor {
    // 白名单
    private List<String> whiteUrls;
    private int _size = 0;

    public SignInterceptor() {
        // 读取文件
        String path = SignInterceptor.class.getResource("/").getFile();
        whiteUrls = FileUtil.readFile(path + "white/signWhite.txt");
        _size = null == whiteUrls ? 0 : whiteUrls.size();
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        // 请求秘钥的接口不需要签名
        String url = request.getRequestURL().toString();
        String refer = request.getHeader("Referer");
        if (url.contains("/app/secret.api") || refer != null && refer.contains("/swagger")
                || WebUtil.isWhiteRequest(url, _size, whiteUrls)) {
            logger.info("SignInterceptor skip");
            return super.preHandle(request, response, handler);
        }
        // 判断密钥是否过期
        String uuid = request.getHeader("UUID");
        if (DataUtil.isEmpty(uuid)) {
            return WebUtil.write(response, HttpCode.METHOD_NOT_ALLOWED.value(), "缺少签名必须条件");
        }
        String publicKey = (String)CacheUtil.getCache().get(Constants.SYSTEM_CACHE_NAMESPACE + "SIGN:" + uuid);
        if (DataUtil.isEmpty(publicKey)) {
            return WebUtil.write(response, HttpCode.NOT_EXTENDED.value(), "密钥已过期");
        }
        // 获取参数
        Map<String, Object> params = WebUtil.getParameterMap(request);
        String[] keys = params.keySet().toArray(new String[]{});
        Arrays.sort(keys);
        StringBuilder sb = new StringBuilder();
        for (String key : keys) {
            if (!"sign".equals(key) && !"dataFile".equals(key)) {
                if (sb.length() > 0) {
                    sb.append("&");
                }
                sb.append(key.length()).append("=").append(params.get(key).toString().length());
            }
        }
        String data = Base64.encodeBase64String(MDCoder.encodeMD5(sb.toString().getBytes("UTF-8")));
        String sign = (String)params.get("sign");
        if (sign == null) {
            return WebUtil.write(response, HttpCode.NOT_ACCEPTABLE.value(), "请求参数未签名");
        }

        sign = sign.replace(' ', '+');
        if (!RSACoder.verify(data.getBytes("UTF-8"), Base64.decodeBase64(publicKey), Base64.decodeBase64(sign))) {
            return WebUtil.write(response, HttpCode.FORBIDDEN.value(), HttpCode.FORBIDDEN.msg());
        }
        logger.info("SignInterceptor successful");
        return super.preHandle(request, response, handler);
    }
}
