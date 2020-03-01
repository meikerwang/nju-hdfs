package cn.nju.dist.datanode;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BlockDataRepository extends JpaRepository<BlockData, Integer> {
	List<BlockData> findAllByIdentity(String identity);
}
