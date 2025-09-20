package az.millikart.apusspring.repository;

import az.millikart.apusspring.model.Payment;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Mapper
@Repository
public interface PaymentRepository {

    @Insert("insert into payments(session, pid, xid, aac, rrn, code, status)\n" +
            "VALUES (#{sessionId}, #{pid},#{xid}, #{aac}, #{rrn}, #{code}, #{status})")
    @Options(useGeneratedKeys = true, keyColumn = "id", keyProperty = "id")
    void save(Payment payment);

    @Update({
            "<script>",
            "update payments",
            "<set>",
            "<if test='sessionId != null'>session = #{sessionId,jdbcType=VARCHAR},</if>",
            "<if test='pid != null'>pid = #{pid,jdbcType=VARCHAR},</if>",
            "<if test='xid != null'>xid = #{xid,jdbcType=VARCHAR},</if>",
            "<if test='aac != null'>aac = #{aac,jdbcType=VARCHAR},</if>",
            "<if test='rrn != null'>rrn = #{rrn,jdbcType=VARCHAR},</if>",
            "<if test='code != null'>code = #{code,jdbcType=VARCHAR},</if>",
            "<if test='status != null'>status = #{status,jdbcType=VARCHAR},</if>",
            "<if test='orderPassword != null'>order_password = #{orderPassword,jdbcType=VARCHAR},</if>",
            "</set>",
            "where id = #{id, jdbcType=INTEGER}",
            "</script>"
    })
    void update(Payment payment);


    @Select("SELECT * FROM payments WHERE xid = #{order}")
    @Result(column = "session" , property = "sessionId")
    @Result(column = "order_password" , property = "orderPassword")
    Optional<Payment> getByOrder(String order);
}
