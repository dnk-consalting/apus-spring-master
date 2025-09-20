package az.millikart.apusspring.repository;

import az.millikart.apusspring.model.Session;
import az.millikart.apusspring.model.SessionService;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface SessionServiceRepository {

    @Insert("insert into session_services_new(session, service, destination, invoice, amount, fee, fcm)\n" +
            "VALUES (#{sessionId}, #{serviceId}, #{destination}, #{invoice}, #{amount}, #{fee}, #{fcm})")
    @Options(useGeneratedKeys = true, keyColumn = "id", keyProperty = "id")
    void save(SessionService sessionService);

    @Select("SELECT * FROM session_services_new WHERE session = #{sessionId}")
    @Results(value = {
            @Result(property = "id", column = "id", javaType = Integer.class),
            @Result(property = "sessionId", column = "session", javaType = Integer.class),
            @Result(property = "serviceId", column = "service", javaType = Integer.class),
            @Result(property = "destination", column = "destination", javaType = String.class),
            @Result(property = "amount", column = "amount", javaType = Double.class),
            @Result(property = "fee", column = "fee", javaType = Double.class),
            @Result(property = "fcm", column = "fcm", javaType = String.class),
            @Result(property = "invoice", column = "invoice", javaType = String.class),
    })
    List<SessionService> getBySessionId(Integer sessionId);


}
