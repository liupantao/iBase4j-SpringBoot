/**
 *
 */
package top.ibase4j.core.base.provider;

import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.ModelMap;
import org.springframework.util.Assert;

import top.ibase4j.core.base.AbstractController;
import top.ibase4j.core.base.BaseModel;

/**
 * 控制器基类
 *
 * @author ShenHuaJie
 * @version 2016年5月20日 下午3:47:58
 */
public abstract class BaseController<T extends BaseProvider> extends AbstractController {
    @Autowired
    protected T provider;

    public abstract String getService();

    public Object query(Map<String, Object> param) {
        return query(new ModelMap(), param);
    }

    public Object query(ModelMap modelMap, Map<String, Object> param) {
        if (param.get("keyword") == null && param.get("search") != null) {
            param.put("keyword", param.get("search"));
            param.remove("search");
        }
        Parameter parameter = new Parameter(getService(), "query", param);
        logger.info("{} execute query start...", parameter.getNo());
        Object result = provider.execute(parameter).getResult();
        logger.info("{} execute query end.", parameter.getNo());
        return setSuccessModelMap(modelMap, result);
    }

    public Object queryList(Map<String, Object> param) {
        return query(new ModelMap(), param);
    }

    public Object queryList(ModelMap modelMap, Map<String, Object> param) {
        Parameter parameter = new Parameter(getService(), "queryList", param);
        logger.info("{} execute queryList start...", parameter.getNo());
        List<?> list = provider.execute(parameter).getResultList();
        logger.info("{} execute queryList end.", parameter.getNo());
        return setSuccessModelMap(modelMap, list);
    }

    public Object get(BaseModel param) {
        return get(new ModelMap(), param);
    }

    public Object get(ModelMap modelMap, BaseModel param) {
        Parameter parameter = new Parameter(getService(), "queryById", param.getId());
        logger.info("{} execute queryById start...", parameter.getNo());
        Object result = provider.execute(parameter).getResult();
        logger.info("{} execute queryById end.", parameter.getNo());
        return setSuccessModelMap(modelMap, result);
    }

    public Object update(BaseModel param) {
        return update(new ModelMap(), param);
    }

    public Object update(ModelMap modelMap, BaseModel param) {
        Long userId = getCurrUser();
        if (param.getId() == null) {
            param.setCreateBy(userId);
            param.setCreateTime(new Date());
        }
        param.setUpdateBy(userId);
        param.setUpdateTime(new Date());
        Parameter parameter = new Parameter(getService(), "update", param);
        logger.info("{} execute update start...", parameter.getNo());
        provider.execute(parameter);
        logger.info("{} execute update end.", parameter.getNo());
        return setSuccessModelMap(modelMap);
    }

    /** 物理删除 */
    public Object delete(BaseModel param) {
        return delete(new ModelMap(), param);
    }

    /** 物理删除 */
    public Object delete(ModelMap modelMap, BaseModel param) {
        Assert.notNull(param.getId(), "ID不能为空");
        Parameter parameter = new Parameter(getService(), "delete", param.getId());
        logger.info("{} execute delete start...", parameter.getNo());
        provider.execute(parameter);
        logger.info("{} execute delete end.", parameter.getNo());
        return setSuccessModelMap(modelMap);
    }

    /** 逻辑删除 */
    public Object del(HttpServletRequest request, BaseModel param) {
        return del(new ModelMap(), request, param);
    }

    /** 逻辑删除 */
    public Object del(ModelMap modelMap, HttpServletRequest request, BaseModel param) {
        Parameter parameter = new Parameter(getService(), "del", param.getId() != null ? param.getId() : param.getIds(),
            getCurrUser(request));
        logger.info("{} execute del start...", parameter.getNo());
        provider.execute(parameter);
        logger.info("{} execute del end.", parameter.getNo());
        return setSuccessModelMap(modelMap);
    }
}
