package cn.nju.dist.namenode;

import java.io.Serializable;

public class Block implements Serializable {

    private static final long serialVersionUID = 136425181921699732L;

    /**
     * 块id
     */
    private String identity;

    /**
     * 文件名
     */
    private String filename;

    /**
     * 该文件的块数量
     */
    private int blockNum;

    /**
     * 该块id
     */
    private Integer blockId;

    /**
     * 块内容
     */
    private byte[] data;

    /**
     * 块内容长度
     */
    private int length;

    /**
     * filename+id转identity
     */
    public static String toIdentity(String name, Integer id) {
        return new String(name + "?" + id);
    }

    /**
     * filename+id转identity(Url格式)
     */
    public static String toIdentityUrl(String name, Integer id) {
        return new String(name + "%3F" + id);
    }

    public Block() {
    }

    /**
     * DataInfo型构造，用于向DataNode上传，含byte数组
     */
    public Block(String filename, int blockNum, Integer blockId, byte[] data, int length) {
        this.filename = filename;
        this.blockId = blockId;
        identity = toIdentity(filename, blockId);
        this.blockNum = blockNum;
        this.data = data;
        this.length = length;
    }

    /**
     * Info型构造，用于向NameNode进行BlockReport以及SplitFile
     */
    public Block(String filename, int blockSize, int blockNum, Integer blockId) {
        this.filename = filename;
        this.blockId = blockId;
        identity = toIdentity(filename, blockId);
        this.blockNum = blockNum;
        this.data = new byte[blockSize];
        this.length = blockSize;
    }

    /**
     * Data型构造，用于从DataNode下载，含byte数组
     */
    public Block(String identity, byte[] data, int length) {
        this.identity = identity;
        String[] tmp = identity.split("[?]");
        this.filename = tmp[0];
        this.blockId = Integer.valueOf(tmp[1]);
        this.blockNum = 0;
        this.data = data;
        this.length = length;
    }

    public String getFilename() {
        return filename;
    }

    public int getBlockNum() {
        return blockNum;
    }

    public Integer getBlockId() {
        return blockId;
    }

    public byte[] getData() {
        return data;
    }

    public String getIdentity() {
        return identity;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

}
