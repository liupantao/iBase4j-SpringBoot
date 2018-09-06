package top.ibase4j.core.base.provider;

import java.util.Date;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.ModelMap;

import top.ibase4j.core.Constants;
import top.ibase4j.core.base.BaseModel;
import top.ibase4j.core.util.InstanceUtil;

public abstract class AppBaseController<T extends BaseProvider> extends BaseController<T> {
    protected final Logger logger = LogManager.getLogger(this.getClass());
    @Autowired
    protected T provider;

    @Override
    protected Long getCurrUser(HttpServletRequest request) {
        Object id = request.getAttribute(Constants.CURRENT_USER);
        if (id == null) {
            return null;
        } else {
            return Long.parseLong(id.toString());
        }
    }

    public Object update(HttpServletRequest request, BaseModel param) {
        return update(request, new ModelMap(), param);
    }

    public Object update(HttpServletRequest request, ModelMap modelMap, BaseModel param) {
        Long userId = getCurrUser(request);
        if (param.getId() == null) {
            param.setCreateBy(userId);
            param.setCreateTime(new Date());
        }
        param.setUpdateBy(userId);
        param.setUpdateTime(new Date());
        Parameter parameter = new Parameter(getService(), "update", param);
        logger.info("{} execute update start...", parameter.getNo());
        Object record = provider.execute(parameter).getResult();
        logger.info("{} execute update end.", parameter.getNo());
        Map<String, Object> result = InstanceUtil.newHashMap("bizeCode", 1);
        result.put("record", record);
        return setSuccessModelMap(modelMap, result);
    }
}