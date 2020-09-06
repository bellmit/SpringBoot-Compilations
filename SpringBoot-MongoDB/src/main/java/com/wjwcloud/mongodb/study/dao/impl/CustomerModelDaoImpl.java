package com.wjwcloud.mongodb.study.dao.impl;

import com.wjwcloud.mongodb.study.dao.CustomerModelDao;
import com.wjwcloud.mongodb.study.entity.Customer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Repository("customerModelDaoImpl")
public class CustomerModelDaoImpl implements CustomerModelDao{

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public Map<String, Object> findPageBySearch(int pageNo, int pageSize, String name) {
        Map<String,Object> result = new HashMap<>();
        Criteria criteria = new Criteria();
        if (!StringUtils.isEmpty(name)) {
            criteria.orOperator(Criteria.where("name").regex(".*?" + name + ".*"));
        }
        Query query = new Query(criteria);
        query.skip((pageNo - 1) * pageSize);
        query.limit(pageSize);
        query.with(Sort.by(new Sort.Order(Sort.Direction.DESC, "name")));
        List<Customer> modules = this.mongoTemplate.find(query, Customer.class);
        long count = this.mongoTemplate.count(query, Customer.class);
        result.put("recordsTotal", count);
        result.put("data", modules);
        return result;
    }
}
