package cn.nju.dist.controller;

import cn.nju.dist.config.Configs;
import cn.nju.dist.datanode.Block;
import cn.nju.dist.datanode.BlockData;
import cn.nju.dist.datanode.BlockInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.Resources;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

@Controller
public class DataNodeController {

    private Logger logger = LoggerFactory.getLogger(DataNodeController.class);

    @Autowired
    private Registration registration; // 服务注册

    @Autowired
    private HttpServletRequest httpServletRequest; // request

//    @Autowired
//    private BlockInfoRepository blockInfoRepository;

//    @Autowired
//    private BlockDataRepository blockDataRepository;

    @Autowired
    private Configs configs;

    @GetMapping("/debug")
    public @ResponseBody
    String debug() {
        String instanceId = registration.getServiceId();
        String uri = String.valueOf(registration.getUri());
        logger.info("DataNode (" + instanceId + ") start at port " + registration.getPort() + ": uri = " + uri);
        return "DataNode (" + instanceId + ") start at port " + registration.getPort() + "\n    uri = " + uri;
    }


    private Map<String, BlockInfo> blockInfoMap = new HashMap<>();
    private Map<String, BlockData> blockDataMap = new HashMap<>();

    /**
     * BlockReport，告知NameNode自己拥有的block
     */
    @GetMapping("/report")
    public @ResponseBody
    List<Block> blockReport() {
        try {
//            List<BlockInfo> blockInfos = blockInfoRepository.findAll();
            List<Block> blocks = new ArrayList<>();
            for (BlockInfo blockInfo : blockInfoMap.values())
                blocks.add(new Block(blockInfo.getFilename(), configs.getBlockSize(), blockInfo.getBlockNum(),
                        blockInfo.getBlockId()));
            return blocks;
        } catch (Exception e) {
        }
        return new ArrayList<Block>();
    }

    @GetMapping("/blocks")
    public @ResponseBody
    Resources<Resource<String>> showBlock() {
//        List<BlockData> blockList = blockDataRepository.findAll();
        List<String> blockStrs = new ArrayList<>();
        for (BlockData data : blockDataMap.values()) {
            int len = data.getLength();
            int showNum = Math.min(configs.getShowNum(), len); // 显示的byte数
            byte[] partData = new byte[showNum];
            System.arraycopy(data.getData(), 0, partData, 0, showNum);
            blockStrs.add(data.getFilename() +
                    "(" + data.getBlockId() + "/" + data.getBlockNum() + "): " +
                    new String(partData) + "..., length = " + data.getLength());
        }
        List<Resource<String>> blockRes = blockStrs.stream()
                .map(str -> new Resource<>(str)).collect(Collectors.toList());
        return new Resources<>(blockRes
                , linkTo(methodOn(DataNodeController.class).showBlock()).withSelfRel()
        );
    }

    /**
     * 上传文件块
     */
    @PostMapping("/blocks")
    public @ResponseBody
    String saveBlock(@RequestBody Block block) {
        BlockData blockData = new BlockData(block.getFilename(),
                block.getBlockNum(), block.getBlockId(), block.getData(), block.getLength());
//        blockDataRepository.save(blockData);
        blockDataMap.put(blockData.getIdentity(), blockData);
        BlockInfo blockInfo = new BlockInfo(blockData.getFilename(), blockData.getBlockNum(), blockData.getBlockId());
//        blockInfoRepository.save(blockInfo);
        blockInfoMap.put(blockInfo.getIdentity(), blockInfo);
        return "Save Block Success";
    }

    /**
     * 下载文件块
     */
    @GetMapping("/blocks/**")
    public @ResponseBody
    Block getBlock() {
        String path = httpServletRequest.getRequestURI();
        String identityUrl = path.substring(new String("/blocks/").length());
        System.out.println(identityUrl);
        String identity = getIdentityFromUrl(identityUrl);
        //String[] pathStrs = identity.split("[/]");
        logger.info("DownLoad Block: " + identity + "(" + identityUrl + ")");
//        List<BlockData> blockDatas = blockDataRepository.findAllByIdentity(identity);
//        Collection<BlockData> blockDataCollection = blockDataMap.values();
//        int index = (int) (Math.random() * blockDataCollection.size());
        BlockData blockData = blockDataMap.get(identity);
        if (blockData == null) {
            return new Block();
        }
//        BlockData blockData = (BlockData) blockDataCollection.toArray()[index];
//        BlockData blockData = blockDatas.get(index);
        return new Block(identity, blockData.getData(), blockData.getLength());
    }

//    public <T> List<T> findAll(Map<String, T> map, String file) {
//        ArrayList<T> list = new ArrayList<>();
//        for (Map.Entry<String, T> entry : map.entrySet()) {
//            String identify = entry.getKey();
//            T value = entry.getValue();
//            String name = "";
//            if (value instanceof BlockData) {
//                BlockData blockData = (BlockData) value;
//                name = blockData.getFilename();
//            } else if (value instanceof BlockInfo) {
//                BlockInfo blockInfo = (BlockInfo) value;
//                name = blockInfo.getFilename();
//            }
//            if (name.equals(file)) {
//                list.add(value);
//            }
//        }
//        return list;
//    }

    /**
     * 删除文件块
     */
    @DeleteMapping("/blocks/**")
    public @ResponseBody
    String deleteBlock() {
        String path = httpServletRequest.getRequestURI();
        String identityUrl = path.substring(new String("/blocks/").length());
        String identity = getIdentityFromUrl(identityUrl);
        //String[] pathStrs = identity.split("[/]");
        logger.info("Delete Block: " + identity + "(" + identityUrl + ")");
//        List<BlockInfo> blockInfos = blockInfoRepository.findAllByIdentity(identity);
//        List<BlockData> blockDatas = blockDataRepository.findAllByIdentity(identity);
//        for (BlockInfo blockInfo : blockInfos)
//            blockInfoRepository.delete(blockInfo);
//        for (BlockData blockData : blockDatas)
//            blockDataRepository.delete(blockData);
        BlockInfo blockInfo = blockInfoMap.get(identity);
        BlockData blockData = blockDataMap.get(identity);
        blockInfoMap.remove(blockInfo.getIdentity());
        blockDataMap.remove(blockData.getIdentity());
//        List<BlockInfo> blockInfos = findAll(blockInfoMap, identity);
//        List<BlockData> blockDatas = findAll(blockDataMap, identity);
//        for (BlockInfo blockInfo : blockInfos)
//            blockInfoMap.remove(blockInfo.getIdentity());
//        for (BlockData blockData : blockDatas)
//            blockDataMap.remove(blockData.getIdentity());
        return new String("Delete Block " + identityUrl + "(copyNum = " + blockInfo.getBlockNum() + ") Success");
    }

    /**
     * 将block的URL转成identity
     */
    private String getIdentityFromUrl(String identityUrl) {
        // 转换URL的转义字符
        String identity = "";
        try {
            identity = URLDecoder.decode(identityUrl, "UTF-8");
            identity = URLDecoder.decode(identity, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return identity;
    }
}
