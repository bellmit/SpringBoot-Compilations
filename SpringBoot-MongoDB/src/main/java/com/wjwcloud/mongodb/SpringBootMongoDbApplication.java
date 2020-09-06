package com.wjwcloud.mongodb;

import com.wjwcloud.mongodb.study.dao.CustomerDao;
import com.wjwcloud.mongodb.study.dao.CustomerModelDao;
import com.wjwcloud.mongodb.study.entity.Customer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import javax.annotation.Resource;
import java.util.Map;

@SpringBootApplication
public class SpringBootMongoDbApplication implements CommandLineRunner {

	@Autowired
	private CustomerDao customerDao;
	@Resource(name = "customerModelDaoImpl")
	private CustomerModelDao customerModelDao;

	public static void main(String[] args) {
		SpringApplication.run(SpringBootMongoDbApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		//全部清除
		customerDao.deleteAll();
		//添加数据
		customerDao.save(new Customer("name111",22));
		customerDao.save(new Customer("name222",23));
		//查询数据
		for (Customer customer : customerDao.findAll()) {
			System.out.println(customer);
		}
		//example条件查询+分页数据
		int pageNumber = 1;
		int pageSize = 20;

		PageRequest pageable = PageRequest.of(pageNumber - 1, pageSize);
		Customer customer = new Customer();
		customer.setName("name222");
		ExampleMatcher matcher = ExampleMatcher.matching().withIgnorePaths("age","createTime");
		Example<Customer> example = Example.of(customer,matcher);

		Page<Customer> page = customerDao.findAll(example,pageable);
		long count = customerDao.count(example);
		System.out.println(count);

		//模糊+分页查询
		Page<Customer> page1 = customerDao.findPageByRegex("name",pageable);
		System.out.println();
		//自定义dao实现
		Map<String,Object> map = customerModelDao.findPageBySearch(pageNumber,pageSize,"name");
		System.out.println();
	}
}
