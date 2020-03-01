package cn.nju.dist.util;

import cn.nju.dist.namenode.Block;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class FileSplit {
    /**
     * 文件名
     */
    private String filename;

    /**
     * 文件块大小
     */
    private int blockSize;

    /**
     * 文件块数量
     */
    private int blockNum;

    /**
     * 文件
     */
    private File file;

    /**
     * 最长等待时间
     */
    private int awaitTime;

    /**
     * 线程倒计时器
     */
    public static CountDownLatch threadCountDown;

    public FileSplit(String filename, int blockSize, int awaitTime) {
        this.filename = filename;
        file = new File(this.filename);
        this.blockSize = blockSize;
        blockNum = (int) Math.ceil(file.length() / (double) blockSize);
        this.awaitTime = awaitTime;
    }

    /**
     * 分割文件为block
     */
    public List<Block> split() {
        List<Block> blocks = new ArrayList<>();
        // 给子线程一个倒计时
        threadCountDown = new CountDownLatch(blockNum);
        // 读取并分割文件
        RandomAccessFile rfile;
        try {
            rfile = new RandomAccessFile(file, "r"); // 只读
            // 开始分割（按线程，一个线程一个block）
            for (int blockId = 0; blockId < blockNum; blockId++) {
                Block block = new Block(filename, blockSize, blockNum, blockId);
                SplitRunnableImpl runnable = new SplitRunnableImpl(blockId * blockSize, rfile, block);
                runnable.start();
                blocks.add(block);
            }
            // 等待所有子线程
            threadCountDown.await(awaitTime, TimeUnit.SECONDS);
            rfile.close();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return blocks;
    }

    public int getBlockNum() {
        return blockNum;
    }

    class SplitRunnableImpl implements Runnable {

        /**
         * 线程
         */
        protected Thread thread;

        /**
         * 源文件
         */
        private RandomAccessFile rfile;

        /**
         * 分割的起始位置
         */
        private int startPos;

        /**
         * 最终获取的分割文件
         */
        private Block block;

        public SplitRunnableImpl(int startPos, RandomAccessFile rfile, Block block) {
            this.startPos = startPos;
            this.rfile = rfile;
            this.block = block;
        }

        /**
         * 开启线程
         */
        public void start() {
            thread = new Thread(this);
            thread.start();
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                // 移动到分割点
                rfile.seek(startPos);
                // 从分割起点开始读取
                int bNum = rfile.read(block.getData());
                block.setLength(bNum);
            } catch (IOException e) {
                e.printStackTrace();
            }
            FileSplit.threadCountDown.countDown();
        }

    }

}
