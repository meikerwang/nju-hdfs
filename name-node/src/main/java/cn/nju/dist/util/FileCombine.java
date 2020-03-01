package cn.nju.dist.util;

import cn.nju.dist.namenode.Block;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Comparator;
import java.util.List;

public class FileCombine {

    private Logger logger = LoggerFactory.getLogger(FileCombine.class);

    /**
     * 文件名
     */
    private String filename;

    /**
     * 各个block的数据
     */
    private List<Block> blocks;

    public FileCombine(String filename, List<Block> blocks) {
        this.filename = filename;
        this.blocks = blocks;
    }

    /**
     * 写入文件
     */
    public void write() {
        filename = "_" + filename;
        File file = new File(filename);
        if (!file.exists()) {
            if (file.getParentFile() != null && !file.getParentFile().exists())
                file.getParentFile().mkdirs();
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // 把blocks按照blockId排序
        blocks.sort(new Comparator<Block>() {
            public int compare(Block arg0, Block arg1) {
                return arg0.getBlockId().compareTo(arg1.getBlockId());
            }
        });
        try {
            OutputStream fos = new FileOutputStream(file);
            logger.info("Combine File '" + filename + "': ");
            for (Block block : blocks) {
                fos.write(block.getData(), 0, block.getLength());
                logger.info("--- Block " + block.getBlockId() + ": length = " + block.getLength());
            }
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
