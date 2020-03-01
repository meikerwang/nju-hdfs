package cn.nju.dist.datanode;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.io.Serializable;

@Entity
public class BlockInfo implements Serializable {

	private static final long serialVersionUID = 4628294585624054317L;

	@Id 
	@GeneratedValue(strategy= GenerationType.IDENTITY)
	private Integer id;

	/** 用于查询的块id */
	private String identity;
	
	/** 文件名 */
	private String filename;

	/** 该文件的块数量 */
	private int blockNum;
	
	/** 该块id */
	private Integer blockId;
	
	public BlockInfo() {}
	
	public BlockInfo(String filename, int blockNum, Integer blockId) {
		this.filename = filename;
		this.blockId = blockId;
		this.blockNum = blockNum;
		identity = new String(filename + "?" + blockId);
	}
	
	public String getFilename() {
		return filename;
	}
	public void setFilename(String filename) {
		this.filename = filename;
	}
	public int getBlockNum() {
		return blockNum;
	}
	public void setBlockNum(int blockNum) {
		this.blockNum = blockNum;
	}
	public Integer getBlockId() {
		return blockId;
	}
	public void setBlockId(Integer blockId) {
		this.blockId = blockId;
	}
	public String getIdentity() {
		return identity;
	}
	public void setIdentity(String identity) {
		this.identity = identity;
	}
	
}
