package az.millikart.apusspring.repository;

import az.millikart.apusspring.model.BillerService;
import az.millikart.apusspring.model.Bin;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Mapper
@Repository
public interface BillerServiceRepository {

    @Select("SELECT * FROM services WHERE biller = #{biller} AND code = #{code} AND active")
    @Result(property = "minFee" , column = "min")
    @Result(property = "maxFee" , column = "max")
    @Result(property = "billerId" , column = "biller")
    List<BillerService> getByBillerAndCode(Integer biller , String code);


    @Select("SELECT * FROM services WHERE id = #{id} AND active")
    @Result(property = "minFee" , column = "min")
    @Result(property = "maxFee" , column = "max")
    @Result(property = "billerId" , column = "biller")
    Optional<BillerService> getById(Integer id);
}
