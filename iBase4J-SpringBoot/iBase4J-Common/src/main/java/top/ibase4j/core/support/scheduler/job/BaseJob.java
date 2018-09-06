/**
 *
 */
package top.ibase4j.core.support.scheduler.job;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import top.ibase4j.core.base.provider.BaseProvider;
import top.ibase4j.core.base.provider.Parameter;
import top.ibase4j.core.support.context.ApplicationContextHolder;
import top.ibase4j.core.support.generator.Sequence;
import top.ibase4j.core.support.scheduler.TaskScheduled.TaskType;
import top.ibase4j.core.util.CacheUtil;
import top.ibase4j.core.util.DataUtil;
import top.ibase4j.core.util.MathUtil;

/**
 * 默认调度(非阻塞)
 *
 * @author ShenHuaJie
 * @version 2016年12月29日 上午11:52:32
 */
public class BaseJob implements Job {
    private final Logger logger = LogManager.getLogger();

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        long start = System.currentTimeMillis();
        JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();
        String taskType = jobDataMap.getString("taskType");
        String targetObject = jobDataMap.getString("targetObject");
        String targetMethod = jobDataMap.getString("targetMethod");
        String key = targetMethod + "." + targetObject;
        try {
            logger.info("定时任务[{}.{}]开始", targetObject, targetMethod);
            String requestId = Sequence.next().toString();
            if (CacheUtil.getCache().lock(key, requestId, 60 * 60 * 5)) {
                try {
                    if (TaskType.local.equals(taskType)) {
                        Object refer = ApplicationContextHolder.getBean(targetObject);
                        refer.getClass().getDeclaredMethod(targetMethod).invoke(refer);
                    } else if (TaskType.dubbo.equals(taskType)) {
                        if (DataUtil.isEmpty(jobDataMap.getString("targetSystem"))) {
                            Object refer = ApplicationContextHolder.getBean(targetObject);
                            refer.getClass().getDeclaredMethod(targetMethod).invoke(refer);
                        } else {
                            BaseProvider provider = (BaseProvider)ApplicationContextHolder
                                    .getBean(jobDataMap.getString("targetSystem"));
                            provider.execute(new Parameter(targetObject, targetMethod));
                        }
                    } else {
                        Object refer = ApplicationContextHolder.getBean(targetObject);
                        refer.getClass().getDeclaredMethod(targetMethod).invoke(refer);
                    }
                    Double time = (System.currentTimeMillis() - start) / 1000.0;
                    logger.info("定时任务[{}.{}]用时：{}s", targetObject, targetMethod, time.toString());
                } finally {
                    unLock(key, requestId);
                }
            }
        } catch (Exception e) {
            throw new JobExecutionException(e);
        }
    }

    private void unLock(String key, String requestId) {
        try {
            CacheUtil.getCache().unlock(key, requestId);
        } catch (Exception e) {
            logger.error("", e);
            try {
                Thread.sleep(MathUtil.getRandom(100, 2000).longValue());
            } catch (Exception e2) {
                logger.error("", e2);
            }
            unLock(key, requestId);
        }
    }
}
