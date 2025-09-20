package az.millikart.apusspring.repository;

import az.millikart.apusspring.model.Bin;
import az.millikart.apusspring.model.Session;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Mapper
@Repository
public interface BinRepository {

    @Select("SELECT * FROM bins WHERE bin = #{bin} OR bin = '000000' AND active ORDER BY bin DESC")
    List<Bin> getByBin(String bin);
}
