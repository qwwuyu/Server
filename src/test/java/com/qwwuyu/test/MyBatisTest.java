package com.qwwuyu.test;

import javax.annotation.Resource;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.qwwuyu.server.bean.User;
import com.qwwuyu.server.service.IUserService;
import com.qwwuyu.server.utils.J2EEUtil;

//表示继承了SpringJUnit4ClassRunner类
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:spring-mybatis.xml" })
public class MyBatisTest {
	private static Logger logger = Logger.getLogger(MyBatisTest.class);
	@Resource
	private IUserService service = null;

	@Test
	public void test1() {
		long time = System.currentTimeMillis();
		User user = service.selectByUser(new User().setName("qwwuyu"));
		logger.info(J2EEUtil.parseToken(user.getToken()));
		System.out.println(System.currentTimeMillis() - time);
	}
}
