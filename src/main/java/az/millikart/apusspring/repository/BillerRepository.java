package az.millikart.apusspring.repository;

import az.millikart.apusspring.model.Biller;
import az.millikart.apusspring.model.Bin;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Mapper
@Repository
public interface BillerRepository {

    @Select("SELECT * FROM billers WHERE code = #{code} AND active")
    Optional<Biller> getByCode(String code);

    @Select("SELECT * FROM billers WHERE id = #{id} AND active")
    Optional<Biller> getById(Integer id);
}
