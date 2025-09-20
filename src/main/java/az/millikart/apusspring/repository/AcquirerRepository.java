package az.millikart.apusspring.repository;

import az.millikart.apusspring.model.Acquirer;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Mapper
@Repository
public interface AcquirerRepository {

    @Select("SELECT * FROM acquirers_new WHERE id = #{id} AND active")
    @Result(column = "txpg_login" , property = "txpgLogin")
    @Result(column = "txpg_password" , property = "txpgPassword")
    Optional<Acquirer> getById(Integer id);


    @Select("SELECT * FROM acquirers_new WHERE bic = #{bic} AND active")
    @Result(column = "txpg_login" , property = "txpgLogin")
    @Result(column = "txpg_password" , property = "txpgPassword")
    Optional<Acquirer> getByBic(String bic);
}
