package cn.nju.dist.namenode;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 某一个文件块出现在哪些DataNode中
 */
public class BlockMap implements Serializable {

    private static final long serialVersionUID = -909209754631536343L;
    /**
     * 文件名
     */
    private String filename;

    /**
     * 文件块编号
     */
    private Integer blockId;

    /**
     * 所在DataNode
     */
    private List<String> uris;

    public BlockMap(String filename, Integer blockId, String uri) {
        this.filename = filename;
        this.blockId = blockId;
        uris = new ArrayList<>();
        uris.add(uri);
    }

    /**
     * 添加一个含有该块的DataNode
     */
    public void addDataNode(String uri) {
        uris.add(uri);
    }

    public String getFilename() {
        return filename;
    }

    public Integer getBlockId() {
        return blockId;
    }

    public List<String> getUris() {
        return uris;
    }

    public String toString() {
        StringBuilder tmp = new StringBuilder("[");
        for (String uri : uris)
            tmp.append("'").append(uri).append("',");
        tmp.append("]");
        return tmp.toString();
    }

}
