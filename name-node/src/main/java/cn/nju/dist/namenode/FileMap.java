package cn.nju.dist.namenode;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * NameNode保存的，某一文件各块所在DataNode
 */
public class FileMap implements Serializable {

    private static final long serialVersionUID = 7492519762360497648L;
    /**
     * 文件名
     */
    private String filename;

    /**
     * 块数量
     */
    private int blockNum;

    /**
     * 各块所在的DataNode列表
     */
    private Map<Integer, BlockMap> blockMaps;

    public FileMap(String filename, int blockNum, BlockMap blockMap) {
        this.filename = filename;
        this.blockNum = blockNum;
        blockMaps = new HashMap<>();
        blockMaps.put(blockMap.getBlockId(), blockMap);
    }

    /**
     * 添加一份块信息
     */
    public void addBlock(Block blockInfo, String uri) {
        Integer blockId = blockInfo.getBlockId();
        // 若该块已经存在于blockMaps（即存在备份）
        if (blockMaps.containsKey(blockId)) {
            blockMaps.get(blockId).addDataNode(uri);
        } else { // 若第一次发现该块
            blockMaps.put(blockId, new BlockMap(filename, blockId, uri));
        }
    }

    public String getFilename() {
        return filename;
    }

    public int getBlockNum() {
        return blockNum;
    }

    public Map<Integer, BlockMap> getBlockMaps() {
        return blockMaps;
    }

    public String toString() {
        StringBuilder tmp = new StringBuilder("File '" + filename + "' ");
        try {
            tmp.append("(bn=" + blockNum +
                    ", url='" + URLEncoder.encode(filename.replace('/', '?'), "UTF-8") + "')");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        tmp.append(": [");
        for (Entry<Integer, BlockMap> map : blockMaps.entrySet()) {
            tmp.append(map.getKey() + ":" + map.getValue().toString() + ", ");
        }
        tmp.append("]");
        return tmp.toString();
    }
}
