package az.millikart.apusspring.repository;

import az.millikart.apusspring.model.Session;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Mapper
@Repository
public interface SessionRepository {

    @Select("SELECT * FROM sessions_new WHERE value = #{value}")
    Optional<Session> getByValue(String value);

    @Select("SELECT * FROM sessions_new WHERE id = #{id}")
    Optional<Session> getById(Integer id);

    @Insert("insert into sessions_new(value, bin, amount, fee, currency, date, " +
            "type, committer, redirect, ip, language, status,\n" +
            "                     description, blocked, closed)\n" +
            "VALUES (#{value}, #{bin}, #{amount}, #{fee}, #{currency}, #{date}, " +
            "#{type}, #{commitUrl}, #{redirectUrl}, #{ip}, #{language}, #{status},\n" +
            "        #{description}, #{blocked}, #{closed})")
    @Options(useGeneratedKeys = true, keyColumn = "id", keyProperty = "id")
    void save(Session session);

    @Update({
            "<script>",
            "update sessions_new",
            "<set>",
            "<if test='value != null'>value = #{value,jdbcType=VARCHAR},</if>",
            "<if test='bin != null'>bin = #{bin,jdbcType=VARCHAR},</if>",
            "<if test='amount != null'>amount = #{amount,jdbcType=VARCHAR},</if>",
            "<if test='fee != null'>fee = #{fee,jdbcType=VARCHAR},</if>",
            "<if test='currency != null'>currency = #{currency,jdbcType=VARCHAR},</if>",
            "<if test='date != null'>date = #{date,jdbcType=VARCHAR},</if>",
            "<if test='type != null'>type = #{type,jdbcType=VARCHAR},</if>",
            "<if test='commitUrl != null'>committer = #{commitUrl,jdbcType=VARCHAR},</if>",
            "<if test='redirectUrl != null'>redirect = #{redirectUrl,jdbcType=VARCHAR},</if>",
            "<if test='ip != null'>ip = #{ip,jdbcType=VARCHAR},</if>",
            "<if test='language != null'>language = #{language,jdbcType=VARCHAR},</if>",
            "<if test='status != null'>status = #{status,jdbcType=VARCHAR},</if>",
            "<if test='description != null'>description = #{description,jdbcType=VARCHAR},</if>",
            "<if test='blocked != null'>blocked = #{blocked,jdbcType=VARCHAR},</if>",
            "<if test='closed != null'>closed = #{closed,jdbcType=VARCHAR},</if>",
            "</set>",
            "where id = #{id, jdbcType=INTEGER}",
            "</script>"
    })
    void update(Session session);
}
