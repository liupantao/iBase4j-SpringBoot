package top.ibase4j.core.base;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.ibatis.session.SqlSession;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.enums.SqlMethod;
import com.baomidou.mybatisplus.exceptions.MybatisPlusException;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.mapper.SqlHelper;
import com.baomidou.mybatisplus.mapper.Wrapper;
import com.baomidou.mybatisplus.plugins.Page;
import com.baomidou.mybatisplus.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.toolkit.ReflectionKit;

import top.ibase4j.core.Constants;
import top.ibase4j.core.exception.BusinessException;
import top.ibase4j.core.support.Pagination;
import top.ibase4j.core.support.cache.CacheKey;
import top.ibase4j.core.support.dbcp.HandleDataSource;
import top.ibase4j.core.support.generator.Sequence;
import top.ibase4j.core.util.CacheUtil;
import top.ibase4j.core.util.DataUtil;
import top.ibase4j.core.util.ExceptionUtil;
import top.ibase4j.core.util.InstanceUtil;
import top.ibase4j.core.util.MathUtil;
import top.ibase4j.core.util.PageUtil;

/**
 * @author ShenHuaJie
 * @since 2018年5月24日 下午6:18:41
 * @param <T>
 * @param <M>
 */
public class BaseServiceImpl<T extends BaseModel, M extends BaseMapper<T>> implements BaseService<T> {
    protected Logger logger = LogManager.getLogger();
    @Autowired
    protected M mapper;

    /** 逻辑批量删除 */
    @Override
    @Transactional
    public void del(List<Long> ids, Long userId) {
        ids.forEach(id -> del(id, userId));
    }

    /** 逻辑删除 */
    @Override
    @Transactional
    public void del(Long id, Long userId) {
        try {
            T record = this.getById(id);
            record.setEnable(0);
            record.setUpdateTime(new Date());
            record.setUpdateBy(userId);
            mapper.updateById(record);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /** 物理删除 */
    @Override
    @Transactional
    public void delete(Long id) {
        try {
            mapper.deleteById(id);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /** 物理删除 */
    @Override
    @Transactional
    public Integer deleteByEntity(T t) {
        Wrapper<T> wrapper = new EntityWrapper<T>(t);
        return mapper.delete(wrapper);
    }

    /** 物理删除 */
    @Override
    @Transactional
    public Integer deleteByMap(Map<String, Object> columnMap) {
        return mapper.deleteByMap(columnMap);
    }

    /** 根据Id查询(默认类型Map)  */
    public Pagination<Map<String, Object>> getPageMap(final Page<Long> ids) {
        if (ids != null) {
            Pagination<Map<String, Object>> page = new Pagination<Map<String, Object>>(ids.getCurrent(),
                    ids.getRecords().size());
            page.setTotal(ids.getTotal());
            final List<Map<String, Object>> records = InstanceUtil.newArrayList();
            final String datasource = HandleDataSource.getDataSource();
            IntStream.range(0, ids.getRecords().size()).forEach(i -> records.add(null));
            IntStream.range(0, ids.getRecords().size()).parallel().forEach(i -> {
                HandleDataSource.putDataSource(datasource);
                records.set(i, InstanceUtil.transBean2Map(getById(ids.getRecords().get(i))));
            });
            page.setRecords(records);
            return page;
        }
        return new Pagination<Map<String, Object>>();
    }

    /** 根据参数分页查询 */
    @Override
    public Pagination<T> query(Map<String, Object> params) {
        Page<Long> page = PageUtil.getPage(params);
        page.setRecords(mapper.selectIdPage(page, params));
        return getPage(page);
    }

    /** 根据实体参数分页查询 */
    @Override
    public Pagination<T> query(T entity, Pagination<T> rowBounds) {
        Page<T> page = new Page<T>();
        try {
            PropertyUtils.copyProperties(page, rowBounds);
        } catch (Exception e) {
            logger.error(ExceptionUtil.getStackTraceAsString(e));
        }
        Wrapper<T> wrapper = new EntityWrapper<T>(entity);
        List<T> list = mapper.selectPage(page, wrapper);
        list.forEach(t -> saveCache(t));
        Pagination<T> pager = new Pagination<T>(page.getCurrent(), page.getSize());
        pager.setRecords(list);
        pager.setTotal(mapper.selectCount(wrapper));
        return pager;
    }

    @Override
    /** 根据id查询实体 */
    public T queryById(Long id) {
        return queryById(id, 1);
    }

    @Override /** 根据参数查询 */
    public List<T> queryList(Map<String, Object> params) {
        if (DataUtil.isEmpty(params.get("orderBy"))) {
            params.put("orderBy", "id_");
        }
        if (DataUtil.isEmpty(params.get("sortAsc"))) {
            params.put("sortAsc", "desc");
        }
        List<Long> ids = mapper.selectIdPage(params);
        List<T> list = queryList(ids);
        return list;
    }

    @Override /** 根据实体参数查询 */
    public List<T> queryList(T params) {
        List<Long> ids = mapper.selectIdPage(params);
        List<T> list = queryList(ids);
        return list;
    }

    /** 根据Id查询(默认类型T)  */
    @Override
    public List<T> queryList(final List<Long> ids) {
        final List<T> list = InstanceUtil.newArrayList();
        if (ids != null) {
            final String datasource = HandleDataSource.getDataSource();
            IntStream.range(0, ids.size()).forEach(i -> list.add(null));
            IntStream.range(0, ids.size()).parallel().forEach(i -> {
                HandleDataSource.putDataSource(datasource);
                list.set(i, getById(ids.get(i)));
            });
        }
        return list;
    }

    /** 根据Id查询(cls返回类型Class) */
    @Override
    public <K> List<K> queryList(final List<Long> ids, final Class<K> cls) {
        final List<K> list = InstanceUtil.newArrayList();
        if (ids != null) {
            final String datasource = HandleDataSource.getDataSource();
            IntStream.range(0, ids.size()).forEach(i -> list.add(null));
            IntStream.range(0, ids.size()).parallel().forEach(i -> {
                HandleDataSource.putDataSource(datasource);
                T t = getById(ids.get(i));
                K k = InstanceUtil.to(t, cls);
                list.set(i, k);
            });
        }
        return list;
    }

    @Override /** 根据实体参数查询一条记录 */
    public T selectOne(T entity) {
        T t = mapper.selectOne(entity);
        saveCache(t);
        return t;
    }

    /** 修改/新增 */
    @Override
    @Transactional
    public T update(T record) {
        try {
            record.setUpdateTime(new Date());
            if (record.getId() == null) {
                record.setCreateTime(new Date());
                mapper.insert(record);
            } else {
                String requestId = Sequence.next().toString();
                String lockKey = getLockKey("U" + record.getId());
                if (CacheUtil.getLock(lockKey, "更新", requestId)) {
                    try {
                        mapper.updateById(record);
                    } finally {
                        CacheUtil.unLock(lockKey, requestId);
                    }
                } else {
                    throw new RuntimeException("数据不一致!请刷新页面重新编辑!");
                }
            }
            record = mapper.selectById(record.getId());
            saveCache(record);
        } catch (DuplicateKeyException e) {
            logger.error(Constants.Exception_Head, e);
            throw new BusinessException("已经存在相同的记录.");
        } catch (Exception e) {
            logger.error(Constants.Exception_Head, e);
            throw new RuntimeException(ExceptionUtil.getStackTraceAsString(e));
        }
        return record;
    }

    /** 修改所有字段/新增 */
    @Override
    @Transactional
    public T updateAllColumn(T record) {
        try {
            record.setUpdateTime(new Date());
            if (record.getId() == null) {
                record.setCreateTime(new Date());
                mapper.insert(record);
            } else {
                String requestId = Sequence.next().toString();
                String lockKey = getLockKey("U" + record.getId());
                if (CacheUtil.getLock(lockKey, "更新所有字段", requestId)) {
                    try {
                        mapper.updateAllColumnById(record);
                    } finally {
                        CacheUtil.unLock(lockKey, requestId);
                    }
                } else {
                    throw new RuntimeException("数据不一致!请刷新页面重新编辑!");
                }
            }
            record = mapper.selectById(record.getId());
            saveCache(record);
        } catch (DuplicateKeyException e) {
            logger.error(Constants.Exception_Head, e);
            throw new BusinessException("已经存在相同的记录.");
        } catch (Exception e) {
            logger.error(Constants.Exception_Head, e);
            throw new RuntimeException(ExceptionUtil.getStackTraceAsString(e));
        }
        return record;
    }

    /** 批量修改所有字段/新增 */
    @Override
    @Transactional
    public boolean updateAllColumnBatch(List<T> entityList) {
        return updateAllColumnBatch(entityList, 30);
    }

    /** 批量修改所有字段/新增 */
    @Override
    @Transactional
    public boolean updateAllColumnBatch(List<T> entityList, int batchSize) {
        return updateBatch(entityList, batchSize, false);
    }

    /** 批量修改/新增 */
    @Override
    @Transactional
    public boolean updateBatch(List<T> entityList) {
        return updateBatch(entityList, 30);
    }

    /** 批量修改/新增 */
    @Override
    @Transactional
    public boolean updateBatch(List<T> entityList, int batchSize) {
        return updateBatch(entityList, batchSize, true);
    }

    @SuppressWarnings("unchecked")
    protected Class<T> currentModelClass() {
        return ReflectionKit.getSuperClassGenricType(getClass(), 0);
    }

    /**
     * 获取缓存键值
     *
     * @param id
     * @return
     */
    protected String getLockKey(Object id) {
        CacheKey cacheKey = CacheKey.getInstance(getClass());
        StringBuilder sb = new StringBuilder();
        if (cacheKey == null) {
            sb.append(getClass().getName());
        } else {
            sb.append(cacheKey.getValue());
        }
        return sb.append(":LOCK:").append(id).toString();
    }

    /**
     * @param params
     * @param cls
     * @return
     */
    protected <P> Pagination<P> query(Map<String, Object> params, Class<P> cls) {
        Page<Long> page = PageUtil.getPage(params);
        page.setRecords(mapper.selectIdPage(page, params));
        return getPage(page, cls);
    }

    /**
     * @param millis
     */
    protected void sleep(int millis) {
        try {
            Thread.sleep(MathUtil.getRandom(10, millis).intValue());
        } catch (Exception e) {
            logger.error("", e);
        }
    }

    /**
     * <p>
     * 批量操作 SqlSession
     * </p>
     */
    protected SqlSession sqlSessionBatch() {
        return SqlHelper.sqlSessionBatch(currentModelClass());
    }

    /**
     * 获取SqlStatement
     *
     * @param sqlMethod
     * @return
     */
    protected String sqlStatement(SqlMethod sqlMethod) {
        return SqlHelper.table(currentModelClass()).getSqlStatement(sqlMethod.getMethod());
    }

    /** 根据Id查询(默认类型T) */
    private T getById(Long id) {
        return queryById(id, 1);
    }

    /** 根据Id批量查询(默认类型T) */
    protected Pagination<T> getPage(final Page<Long> ids) {
        if (ids != null) {
            Pagination<T> page = new Pagination<T>(ids.getCurrent(), ids.getRecords().size());
            page.setTotal(ids.getTotal());
            final List<T> records = InstanceUtil.newArrayList();
            final String datasource = HandleDataSource.getDataSource();
            IntStream.range(0, ids.getRecords().size()).forEach(i -> records.add(null));
            IntStream.range(0, ids.getRecords().size()).parallel().forEach(i -> {
                HandleDataSource.putDataSource(datasource);
                records.set(i, getById(ids.getRecords().get(i)));
            });
            page.setRecords(records);
            return page;
        }
        return new Pagination<T>();
    }

    /** 根据Id查询(cls返回类型Class) */
    private <K> Pagination<K> getPage(final Page<Long> ids, final Class<K> cls) {
        if (ids != null) {
            Pagination<K> page = new Pagination<K>(ids.getCurrent(), ids.getRecords().size());
            page.setTotal(ids.getTotal());
            final List<K> records = InstanceUtil.newArrayList();
            final String datasource = HandleDataSource.getDataSource();
            IntStream.range(0, ids.getRecords().size()).forEach(i -> records.add(null));
            IntStream.range(0, ids.getRecords().size()).parallel().forEach(i -> {
                HandleDataSource.putDataSource(datasource);
                records.set(i, InstanceUtil.to(getById(ids.getRecords().get(i)), cls));
            });
            page.setRecords(records);
            return page;
        }
        return new Pagination<K>();
    }

    /** 保存缓存 */
    private void saveCache(T record) {
        if (record == null) {
            return;
        }
        CacheKey key = CacheKey.getInstance(getClass());
        if (key != null) {
            try {
                CacheUtil.getCache().set(key.getValue() + ":" + record.getId(), record, key.getTimeToLive());
            } catch (Exception e) {
                logger.error(Constants.Exception_Head, e);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private T queryById(Long id, int times) {
        CacheKey key = CacheKey.getInstance(getClass());
        T record = null;
        if (key != null) {
            try {
                record = (T)CacheUtil.getCache().get(key.getValue() + ":" + id, key.getTimeToLive());
            } catch (Exception e) {
                logger.error(Constants.Exception_Head, e);
            }
        }
        if (record == null) {
            String lockKey = getLockKey(id);
            String requestId = Sequence.next().toString();
            if (CacheUtil.getLock(lockKey, "根据ID查询数据", requestId)) {
                try {
                    record = mapper.selectById(id);
                    saveCache(record);
                } finally {
                    CacheUtil.unLock(lockKey, requestId);
                }
            } else {
                if (times > 3) {
                    record = mapper.selectById(id);
                    saveCache(record);
                } else {
                    logger.debug(getClass().getSimpleName() + ":" + id + " retry getById.");
                    sleep(100);
                    return queryById(id, times + 1);
                }
            }
        }
        return record;
    }

    private boolean updateBatch(List<T> entityList, int batchSize, boolean selective) {
        if (CollectionUtils.isEmpty(entityList)) {
            throw new IllegalArgumentException("Error: entityList must not be empty");
        }
        try (SqlSession batchSqlSession = sqlSessionBatch()) {
            IntStream.range(0, entityList.size()).forEach(i -> {
                if (selective) {
                    update(entityList.get(i));
                } else {
                    updateAllColumn(entityList.get(i));
                }
                if (i >= 1 && i % batchSize == 0) {
                    batchSqlSession.flushStatements();
                }
            });
            batchSqlSession.flushStatements();
        } catch (Throwable e) {
            throw new MybatisPlusException("Error: Cannot execute insertOrUpdateBatch Method. Cause", e);
        }
        return true;
    }
}
