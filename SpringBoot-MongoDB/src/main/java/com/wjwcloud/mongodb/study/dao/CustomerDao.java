package com.wjwcloud.mongodb.study.dao;

import com.wjwcloud.mongodb.study.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface CustomerDao extends MongoRepository<Customer, String> {

    /**
     * name模糊查询
     * ?0 标识匹配第一个参数
     * @param name
     * @param pageable
     * @return
     */
    @Query(value = "{name:{$regex:?0}}")
    Page<Customer> findPageByRegex(String name, Pageable pageable);

}
