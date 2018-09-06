package top.ibase4j.core.base;

import java.util.List;
import java.util.Map;

import org.springframework.transaction.annotation.Transactional;

import top.ibase4j.core.support.Pagination;

/**
 * @author ShenHuaJie
 * @version 2016年5月20日 下午3:19:19
 */
public interface BaseService<T extends BaseModel> {
    /**
     * 修改
     * @param record
     * @return
     */
    @Transactional
    T update(T record);

    /**
     * 逻辑删除批量
     * @param ids
     * @param userId
     */
    @Transactional
    void del(List<Long> ids, Long userId);

    /**
     * 逻辑删除单条
     * @param id
     * @param userId
     */
    @Transactional
    void del(Long id, Long userId);

    /**
     * 物理删除
     * @param id
     */
    @Transactional
    void delete(Long id);

    /**
     * 物理删除
     * @param t
     * @return
     */
    @Transactional
    Integer deleteByEntity(T t);

    /**
     * 物理删除
     * @param columnMap
     * @return
     */
    @Transactional
    Integer deleteByMap(Map<String, Object> columnMap);

    /**
     * 根据ID查询
     * @param id
     * @return
     */
    T queryById(Long id);

    /**
     * 分页查询
     * @param params
     * @return
     */
    Pagination<T> query(Map<String, Object> params);

    Pagination<T> query(T entity, Pagination<T> rowBounds);

    List<T> queryList(Map<String, Object> params);

    List<T> queryList(List<Long> ids);

    <K> List<K> queryList(List<Long> ids, Class<K> cls);

    List<T> queryList(T entity);

    T selectOne(T entity);

    T updateAllColumn(T record);

    boolean updateAllColumnBatch(List<T> entityList);

    boolean updateAllColumnBatch(List<T> entityList, int batchSize);

    boolean updateBatch(List<T> entityList);

    boolean updateBatch(List<T> entityList, int batchSize);
}
