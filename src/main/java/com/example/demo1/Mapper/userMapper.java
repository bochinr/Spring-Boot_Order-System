package com.example.demo1.Mapper;

import com.example.demo1.Entity.user;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface userMapper {

    @Select("select * from user")
    public List<user> findAll();

    @Select("select * from user where name = #{username}")//根据用户名查询
    public  user findByname(String username);

    @Insert("insert into user(name,age,password) values(#{username},#{age},#{password})")
    public void register(String username,String password,int age);//新增用户

    @Delete("delete from user where id = #{id}")
    public void deleteById(Integer id);//删除用户
}
