package cn.nju.dist.datanode;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BlockInfoRepository extends JpaRepository<BlockInfo, Integer> {
	List<BlockInfo> findAllByIdentity(String identity);
}
