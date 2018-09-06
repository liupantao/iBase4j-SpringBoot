package top.ibase4j.core.base;

import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.InitBinder;

import com.alibaba.fastjson.JSON;

import top.ibase4j.core.Constants;
import top.ibase4j.core.exception.BaseException;
import top.ibase4j.core.exception.IllegalParameterException;
import top.ibase4j.core.support.DateFormat;
import top.ibase4j.core.support.HttpCode;
import top.ibase4j.core.support.context.StringEscapeEditor;
import top.ibase4j.core.util.PropertiesUtil;

/**
 * @author ShenHuaJie
 * @since 2018年5月24日 上午9:24:09
 */
@ControllerAdvice
public class AdviceController {
    private Logger logger = LogManager.getLogger();
    @InitBinder
    public void initBinder(WebDataBinder binder) {
        /**
         * 自动转换日期类型的字段格式
         */
        binder.registerCustomEditor(Date.class, new CustomDateEditor(new DateFormat("yyyy-MM-dd HH:mm:ss"), true));
        /**
         * 防止XSS攻击
         */
        binder.registerCustomEditor(String.class,
            new StringEscapeEditor(PropertiesUtil.getBoolean("spring.mvc.htmlEscape", false),
                PropertiesUtil.getBoolean("spring.mvc.javaScriptEscape", false)));
    }

    /** 异常处理 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ModelMap> exceptionHandler(HttpServletRequest request, HttpServletResponse response,
        Exception ex) throws Exception {
        logger.error(Constants.Exception_Head, ex);
        ModelMap modelMap = new ModelMap();
        if (ex instanceof BaseException) {
            ((BaseException)ex).handler(modelMap);
        } else if (ex instanceof IllegalArgumentException) {
            new IllegalParameterException(ex.getMessage()).handler(modelMap);
        } else if ("org.apache.shiro.authz.UnauthorizedException".equals(ex.getClass().getName())) {
            modelMap.put("code", HttpCode.FORBIDDEN.value().toString());
            modelMap.put("msg", HttpCode.FORBIDDEN.msg());
        } else {
            modelMap.put("code", HttpCode.INTERNAL_SERVER_ERROR.value().toString());
            String msg = StringUtils.defaultIfBlank(ex.getMessage(), HttpCode.INTERNAL_SERVER_ERROR.msg());
            logger.debug(msg);
            modelMap.put("msg", msg.length() > 100 ? "系统走神了,请稍候再试." : msg);
        }
        modelMap.put("timestamp", System.currentTimeMillis());
        logger.info("response===>" + JSON.toJSON(modelMap));
        return ResponseEntity.ok(modelMap);
    }
}
