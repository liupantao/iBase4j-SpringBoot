package org.ibase4j;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

import com.alibaba.dubbo.common.Constants;

import top.ibase4j.core.util.SecurityUtil;

/**
 * @author ShenHuaJie
 * @since 2018年4月21日 下午3:29:03
 */
@SpringBootApplication(scanBasePackages = {"top.ibase4j", "org.ibase4j"})
public class SysWebApplication extends SpringBootServletInitializer {
	public static void main(String[] args) {
		SpringApplication.run(SysWebApplication.class, args);
	}
	/*public static void main(String[] args) {
		//encrypt 加密
		String key=SecurityUtil.encryptDes("buzhidao",top.ibase4j.core.Constants.DB_KEY.getBytes());
	    System.out.println(key);
	    String keytrue=SecurityUtil.encryptDes("root",top.ibase4j.core.Constants.DB_KEY.getBytes());
	    System.out.println(keytrue); 
	}*/
}