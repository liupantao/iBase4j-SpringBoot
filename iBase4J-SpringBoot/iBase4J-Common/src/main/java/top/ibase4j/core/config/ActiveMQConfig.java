package top.ibase4j.core.config;

import java.util.Map;
import java.util.Properties;

import org.apache.activemq.spring.ActiveMQConnectionFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.core.JmsTemplate;

import top.ibase4j.core.util.PropertiesUtil;

/**
 * @author ShenHuaJie
 * @since 2018年4月28日 上午11:19:52
 */
@Configuration
@ConditionalOnClass(ActiveMQConnectionFactory.class)
public class ActiveMQConfig {
    @Bean
    public CachingConnectionFactory jmsConnectionFactory() {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
        ActiveMQConnectionFactory mqConnectionFactory = new ActiveMQConnectionFactory();
        Properties properties = new Properties();
        Map<String, String> config = PropertiesUtil.getProperties();
        for (String key : config.keySet()) {
            if (key.startsWith("mq.")) {
                properties.put(key.replace("mq.", ""), config.get(key));
            }
        }
        mqConnectionFactory.buildFromProperties(properties);
        connectionFactory.setTargetConnectionFactory(mqConnectionFactory);
        return connectionFactory;
    }

    @Bean
    public JmsTemplate jmsQueueTemplate(CachingConnectionFactory connectionFactory) {
        JmsTemplate jmsTemplate = new JmsTemplate(connectionFactory);
        jmsTemplate.setReceiveTimeout(PropertiesUtil.getLong("mq.receiveTimeout", 10000));
        jmsTemplate.setPubSubDomain(false);
        return jmsTemplate;
    }

    @Bean
    public JmsTemplate jmsTopicTemplate(CachingConnectionFactory connectionFactory) {
        JmsTemplate jmsTemplate = new JmsTemplate(connectionFactory);
        jmsTemplate.setReceiveTimeout(PropertiesUtil.getLong("mq.receiveTimeout", 10000));
        jmsTemplate.setPubSubDomain(true);
        return jmsTemplate;
    }
}
