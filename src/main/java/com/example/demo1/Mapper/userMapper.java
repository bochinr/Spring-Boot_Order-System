package com.example.demo1.Mapper;

import com.example.demo1.Entity.user;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface userMapper {

    @Select("select * from user")
    public List<user> findAll();

    @Select("select * from user where id = #{id}") // 根据ID查询
    public user findById(Integer id);

    @Select("select * from user where name = #{username}") // 根据用户名查询
    public user findByname(String username);

    @Select("select * from user where phone = #{phone}") // 根据手机号查询
    public user findByPhone(String phone);

    @Select("select * from user where email = #{email}") // 根据邮箱查询
    public user findByEmail(String email);

    @Select("select * from user where wechat_openid = #{openId}") // 根据微信OpenID查询
    public user findByWechatOpenId(String openId);

    @Select("select * from user where alipay_userid = #{userId}") // 根据支付宝UserID查询
    public user findByAlipayUserId(String userId);

    @Insert("insert into user(name, age, password) values(#{username}, #{age}, #{password})")
    public void register(String username, String password, int age); // 新增用户

    @Insert("INSERT INTO user(name, password, age, wechat_openid, alipay_userid) VALUES(#{name}, #{password}, #{age}, #{wechatOpenid}, #{alipayUserid})")
    public int createUser(user user);

    @Update("UPDATE user SET wechat_openid = #{wechatOpenid} WHERE id = #{id}")
    public int updateWechatOpenId(int id, String wechatOpenid);

    @Update("UPDATE user SET alipay_userid = #{alipayUserid} WHERE id = #{id}")
    public int updateAlipayUserId(int id, String alipayUserid);

    @Update("UPDATE user SET password = #{password} WHERE id = #{id}")
    public int updatePassword(int id, String password);

    @Delete("delete from user where id = #{id}")
    public void deleteById(Integer id); // 删除用户
}
