package top.ibase4j.core.config;

import java.util.ResourceBundle;

import javax.sql.DataSource;

import org.apache.ibatis.logging.slf4j.Slf4jImpl;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mybatis.spring.mapper.MapperScannerConfigurer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.baomidou.mybatisplus.MybatisConfiguration;
import com.baomidou.mybatisplus.entity.GlobalConfiguration;
import com.baomidou.mybatisplus.enums.IdType;
import com.baomidou.mybatisplus.mapper.AutoSqlInjector;
import com.baomidou.mybatisplus.plugins.PaginationInterceptor;
import com.baomidou.mybatisplus.spring.MybatisSqlSessionFactoryBean;

import top.ibase4j.core.util.CacheUtil;
import top.ibase4j.core.util.DataUtil;
import top.ibase4j.core.util.PropertiesUtil;
import top.ibase4j.mapper.LockMapper;

@Configuration
@ConditionalOnClass(value = {MapperScannerConfigurer.class, DataSourceTransactionManager.class})
@EnableTransactionManagement(proxyTargetClass = true)
@EnableScheduling
public class MyBatisConfig {
    private final Logger logger = LogManager.getLogger();

    private LockMapper lockMapper;

    private ResourceBundle config;

    private String get(String key) {
        String value = PropertiesUtil.getString(key);
        if (DataUtil.isEmpty(value)) {
            if (config == null) {
                config = ResourceBundle.getBundle("config/jdbc");
            }
            value = config.getString(key);
        }
        return value;
    }

    /**
     * 根据数据源创建SqlSessionFactory
     */
    @Bean(name = "sqlSessionFactory")
    @ConditionalOnBean(DataSource.class)
    public MybatisSqlSessionFactoryBean sqlSessionFactory(DataSource dataSource) throws Exception {
        MybatisSqlSessionFactoryBean sessionFactory = new MybatisSqlSessionFactoryBean();
        sessionFactory.setDataSource(dataSource);

        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        sessionFactory.setMapperLocations(resolver.getResources(get("mybatis.mapperLocations")));
        sessionFactory.setTypeAliasesPackage(get("mybatis.typeAliasesPackage"));

        PaginationInterceptor page = new PaginationInterceptor();
        page.setDialectType(get("mybatis.dialectType"));
        sessionFactory.setPlugins(new Interceptor[]{page});

        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.setLogImpl(Slf4jImpl.class);
        configuration.setCallSettersOnNulls(true);
        sessionFactory.setConfiguration(configuration);

        String idType = get("mybatis.idType");
        GlobalConfiguration config = new GlobalConfiguration();
        config.setDbColumnUnderline(true);
        config.setSqlInjector(new AutoSqlInjector());
        if (DataUtil.isEmpty(idType)) {
            config.setIdType(IdType.AUTO.getKey());
        } else {
            config.setIdType(IdType.valueOf(idType).getKey());
        }
        sessionFactory.setGlobalConfig(config);
        return sessionFactory;
    }

    @Bean
    public MapperScannerConfigurer configurer() {
        MapperScannerConfigurer configurer = new MapperScannerConfigurer();
        configurer.setSqlSessionFactoryBeanName("sqlSessionFactory");
        configurer.setBasePackage(get("mybatis.mapperBasePackage"));
        return configurer;
    }

    @Bean
    @ConditionalOnBean(DataSource.class)
    public DataSourceTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean
    public Object setLockMapper(LockMapper lockMapper) {
        this.lockMapper = lockMapper;
        CacheUtil.setLockMapper(lockMapper);
        return lockMapper;
    }

    /** 定时清除会话信息 */
    @Scheduled(cron = "0 0/1 * * * *")
    public void cleanExpiredLock() {
        if (lockMapper != null) {
            logger.info("cleanExpiredLock");
            lockMapper.cleanExpiredLock();
        }
    }
}
