package cn.nju.dist.datanode;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.io.Serializable;

@Entity
public class BlockData implements Serializable {

	private static final long serialVersionUID = -1666644079279045084L;

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
	
	/** 块内容 */
	private byte[] data;

	/** 块内容长度 */
	private int length;

	public BlockData() {}

	public BlockData(String filename, int blockNum, Integer blockId, byte[] data, int length) {
		this.filename = filename;
		this.blockId = blockId;
		this.blockNum = blockNum;
		this.data = data;
		this.length = length;
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
	public byte[] getData() {
		return data;
	}
	public void setData(byte[] data) {
		this.data = data;
	}
	public String getIdentity() {
		return identity;
	}
	public void setIdentity(String identity) {
		this.identity = identity;
	}
	public int getLength() {
		return length;
	}
	public void setLength(int length) {
		this.length = length;
	}
	
}
