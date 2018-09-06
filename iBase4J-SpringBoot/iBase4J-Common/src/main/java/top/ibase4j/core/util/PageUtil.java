package top.ibase4j.core.util;

import java.util.Map;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.plugins.Page;

/**
 * 分页辅助工具
 * @author ShenHuaJie
 * @since 2018年5月24日 下午6:32:32
 */
public final class PageUtil {

    private PageUtil() {
    }

    /** 分页查询参数封装 */
    @SuppressWarnings({"unchecked"})
    public static Page<Long> getPage(Map<String, Object> params) {
        Integer current = 1;
        Integer size = 10;
        String orderBy = "id_", sortAsc = null, openSort = "Y";
        if (DataUtil.isNotEmpty(params.get("pageNumber"))) {
            current = Integer.valueOf(params.get("pageNumber").toString());
        }
        if (DataUtil.isNotEmpty(params.get("pageIndex"))) {
            current = Integer.valueOf(params.get("pageIndex").toString());
        }
        if (DataUtil.isNotEmpty(params.get("pageSize"))) {
            size = Integer.valueOf(params.get("pageSize").toString());
        }
        if (DataUtil.isNotEmpty(params.get("limit"))) {
            size = Integer.valueOf(params.get("limit").toString());
        }
        if (DataUtil.isNotEmpty(params.get("offset"))) {
            current = Integer.valueOf(params.get("offset").toString()) / size + 1;
        }
        if (DataUtil.isNotEmpty(params.get("sort"))) {
            orderBy = (String)params.get("sort");
            params.remove("sort");
        }
        if (DataUtil.isNotEmpty(params.get("orderBy"))) {
            orderBy = (String)params.get("orderBy");
            params.remove("orderBy");
        }
        if (DataUtil.isNotEmpty(params.get("sortAsc"))) {
            sortAsc = (String)params.get("sortAsc");
            params.remove("sortAsc");
        }
        if (DataUtil.isNotEmpty(params.get("openSort"))) {
            openSort = (String)params.get("openSort");
            params.remove("openSort");
        }
        Object filter = params.get("filter");
        if (filter != null) {
            params.putAll(JSON.parseObject(filter.toString(), Map.class));
        }
        if (size == -1) {
            Page<Long> page = new Page<Long>();
            page.setOrderByField(orderBy);
            page.setAsc("Y".equals(sortAsc));
            page.setOpenSort("Y".equals(openSort));
            return page;
        }
        Page<Long> page = new Page<Long>(current, size, orderBy);
        page.setAsc("Y".equals(sortAsc));
        page.setOpenSort("Y".equals(openSort));
        return page;
    }
}
