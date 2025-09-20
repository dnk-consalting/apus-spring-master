package az.millikart.apusspring.repository;

import az.millikart.apusspring.model.InternationalTerminal;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Mapper
@Repository
public interface InternationalTerminalRepository {

    @Select("select * from international_terminals where biller = #{biller}")
    Optional<InternationalTerminal> getByBiller(String biller);
}
