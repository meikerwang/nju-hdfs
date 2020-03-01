package cn.nju.dist.controller;

import cn.nju.dist.config.Configs;
import cn.nju.dist.namenode.Block;
import cn.nju.dist.namenode.BlockMap;
import cn.nju.dist.namenode.FileMap;
import cn.nju.dist.util.FileCombine;
import cn.nju.dist.util.FileSplit;
import com.netflix.appinfo.InstanceInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.netflix.eureka.server.event.EurekaInstanceCanceledEvent;
import org.springframework.cloud.netflix.eureka.server.event.EurekaInstanceRegisteredEvent;
import org.springframework.cloud.netflix.eureka.server.event.EurekaInstanceRenewedEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.Resources;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

@Controller
public class NameNodeController {

    private Logger logger = LoggerFactory.getLogger(NameNodeController.class);

    @EventListener
    public void listen(EurekaInstanceRenewedEvent event) {
        logger.info(event.getServerId() + " 续约");
        if (event.getInstanceInfo() != null)
            excludeServices.remove(event.getInstanceInfo().getHomePageUrl().replace("/", ""));
        refreshDataNodeList();
    }

    @EventListener
    public void listen(EurekaInstanceRegisteredEvent event) {
        InstanceInfo instanceInfo = event.getInstanceInfo();
        excludeServices.remove(instanceInfo.getHomePageUrl().replace("/", ""));
        logger.info(instanceInfo.getHomePageUrl() + " 注册");
        refreshDataNodeList();
    }

    private Set<String> excludeServices = new HashSet<>();

    @EventListener
    public void listen(EurekaInstanceCanceledEvent event) {
        logger.warn(event.getServerId() + " 下线");
        refreshDataBack();
        logger.warn(event.getAppName() + " 取消");
    }


    @Autowired
    private DiscoveryClient discoveryClient;

    @Autowired
    private HttpServletRequest httpServletRequest;

    @Bean
    RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private Configs configs;

    /**
     * 当前所有DataNode服务实例
     */
    private List<ServiceInstance> datanodeList = new ArrayList<>();

    /**
     * 当前所有文件在DataNode的的存储情况
     */
    private Map<String, FileMap> fileMaps;

    /**
     * 获取所有文件块在DataNode的分布情况
     */
    @GetMapping("/map")
    public @ResponseBody
    List<String> getDataNodeMap() {
        this.refreshFileMaps();
        List<String> fileList = new ArrayList<>();
        for (FileMap fileMap : fileMaps.values()) {
            fileList.add(fileMap.toString());
        }
        return fileList;
    }

    /**
     * 上传文件
     */
    @PostMapping("/files")
    public @ResponseBody
    Resources<Resource<String>> saveFile(@RequestParam String filename) {
        // 分割文件并保存至blocks
        logger.info("当前文件保存路径: " + new File("./").getAbsolutePath());
        FileSplit fileSplit = new FileSplit(filename, configs.getBlockSize(), configs.getAwaitTime());
        List<Block> blocks = fileSplit.split();
        int blockNum = fileSplit.getBlockNum();
        // 刷新，获取当前活动的DataNode列表以及当前fileMaps
        this.refreshFileMaps();
        // 上传成功的显示信息
        List<String> blockStrs = new ArrayList<>();
        // 若该文件已存在，取消上传
        if (fileMaps.containsKey(filename)) {
            blockStrs.add("File " + filename + " Already Exists, Please Upload a New File");
            List<Resource<String>> blockRes = blockStrs.stream()
                    .map(str -> new Resource<>(str)).collect(Collectors.toList());
            return new Resources<>(blockRes
                    , linkTo(methodOn(NameNodeController.class).saveFile(filename)).withSelfRel());
        }
        // 根据副本数发送所有块
        int copySize = Math.min(configs.getCopyNum(), datanodeList.size());
        logger.info("Upload File: CopyNum = " + copySize);

        for (Block block : blocks) {
            // 负载均衡：随机在DataNode列表中选择一个
            Collections.shuffle(datanodeList);
            for (int i = 0; i < copySize; i++) {
                // 封装发送的块数据
                Integer blockId = block.getBlockId();
                Block blockData = new Block(filename, blockNum, blockId, block.getData(), block.getLength());
                // 调用DataNode的方法上传块
                String uri = String.valueOf(datanodeList.get(i).getUri());
                String api = getApiByUri(uri, "blocks");
                restTemplate.postForEntity(api, blockData, String.class).getBody();
                // 添加上传块的信息到fileMaps
                Block blockInfo = new Block(filename, configs.getBlockSize(), blockNum, blockId);
                addBlockToMap(blockInfo, uri);
                // 便于显示结果
                int len = block.getLength();
                int showNum = Math.min(configs.getShowNum(), len); // 显示的byte数
                byte[] partData = new byte[showNum];
                System.arraycopy(block.getData(), 0, partData, 0, showNum);
                blockStrs.add(new String(partData));
            }
        }
        List<Resource<String>> blockRes = blockStrs.stream()
                .map(str -> new Resource<>(str)).collect(Collectors.toList());
        return new Resources<>(blockRes
                , linkTo(methodOn(NameNodeController.class).saveFile(filename)).withSelfRel());
    }

    /**
     * 下载文件
     * '/'和'\'在URL里面有点问题，替换成了'?'
     */
    @GetMapping("/files/**")
    public @ResponseBody
    Resources<Resource<String>> getFile() {
        String path = httpServletRequest.getRequestURI();
        String filename = getFilenameFromUrl(path.substring(new String("/files/").length()));
        //String[] pathStrs = filename.split("[/]");
        logger.info("DownLoad File: " + filename);
        // 刷新，获取当前活动的DataNode列表以及当前fileMaps
        this.refreshFileMaps();
        // 下载成功的显示信息
        List<String> uriStrs = new ArrayList<>();
        // 若该文件不存在于HDFS，取消下载
        if (!fileMaps.containsKey(filename)) {
            uriStrs.add("File " + filename + " Not Found, DownLoad Failed");
            List<Resource<String>> uriRes = uriStrs.stream()
                    .map(str -> new Resource<>(str)).collect(Collectors.toList());
            return new Resources<>(uriRes
                    , linkTo(methodOn(NameNodeController.class).saveFile(filename)).withSelfRel());
        }
        FileMap fileMap = fileMaps.get(filename);
        Map<Integer, BlockMap> blockMaps = fileMap.getBlockMaps();
        // 获取byte数组的List
        List<Block> blockList = new ArrayList<>();
        for (BlockMap blockMap : blockMaps.values()) {
            Integer blockId = blockMap.getBlockId();
            String identity = Block.toIdentity(filename, blockId);
            String identityUrl = Block.toIdentityUrl(filename, blockId);
            // 随机选择一个含有该块的DataNode
            List<String> uris = blockMap.getUris();
            int index = (int) (Math.random() * uris.size());
            // 调用DataNode的方法下载块
            String uri = uris.get(index);
            String api = getApiByUri(uri, new String("blocks/" + identityUrl));
            logger.info("DownLoad Block: " + identity + ", url = " + api);
            Block block = restTemplate.getForEntity(api, Block.class).getBody();
            blockList.add(block);
            // 便于显示结果
            uriStrs.add(new String(blockId + ":" + uri + ", "));
        }
        // 组合块并保存
        FileCombine fileCombine = new FileCombine(filename, blockList);
        fileCombine.write();

        List<Resource<String>> uriRes = uriStrs.stream()
                .map(str -> new Resource<>(str)).collect(Collectors.toList());
        return new Resources<>(uriRes
                , linkTo(methodOn(NameNodeController.class).saveFile(filename)).withSelfRel());
    }

    /**
     * 删除
     * '/'和'\'在URL里面有点问题，替换成了'?'
     */
    @DeleteMapping("/files/**")
    public @ResponseBody
    Resources<Resource<String>> deleteFile() {
        String path = httpServletRequest.getRequestURI();
        String filename = getFilenameFromUrl(path.substring(new String("/files/").length()));
        //String[] pathStrs = filename.split("[/]");
        logger.info("Delete File: " + filename);
        // 刷新，获取当前活动的DataNode列表以及当前fileMaps
        this.refreshFileMaps();
        // 下载成功的显示信息
        List<String> uriStrs = new ArrayList<>();
        // 若该文件不存在于HDFS，取消删除
        if (!fileMaps.containsKey(filename)) {
            uriStrs.add("File " + filename + " Not Found, Delete Failed");
            List<Resource<String>> uriRes = uriStrs.stream()
                    .map(str -> new Resource<>(str)).collect(Collectors.toList());
            return new Resources<>(uriRes
                    , linkTo(methodOn(NameNodeController.class).saveFile(filename)).withSelfRel());
        }
        FileMap fileMap = fileMaps.get(filename);
        Map<Integer, BlockMap> blockMaps = fileMap.getBlockMaps();
        for (BlockMap blockMap : blockMaps.values()) {
            Integer blockId = blockMap.getBlockId();
            String identity = Block.toIdentity(filename, blockId);
            String identityUrl = Block.toIdentityUrl(filename, blockId);
            // 遍历全部含有该块的DataNode
            List<String> uris = blockMap.getUris();
            for (String uri : uris) {
                // 调用DataNode的方法删除块
                String api = getApiByUri(uri, new String("blocks/" + identityUrl));
                logger.info("Delete Block: " + identity + ", url = " + api);
                restTemplate.delete(api);
                // 便于显示结果
                uriStrs.add(new String(blockId + ":" + uri + ", "));
            }
        }
        // 刷新当前fileMaps
        fileMaps.remove(filename);

        List<Resource<String>> uriRes = uriStrs.stream()
                .map(str -> new Resource<>(str)).collect(Collectors.toList());
        return new Resources<>(uriRes
                , linkTo(methodOn(NameNodeController.class).saveFile(filename)).withSelfRel());
    }

    /**
     * 根据主机和端口获取服务接口地址
     */
    private String getApiByUri(String uri, String api) {
        return uri + "/" + api;
    }

    /**
     * 将文件名URL转成filename
     */
    private String getFilenameFromUrl(String filenameUrl) {
        // 转换URL的转义字符
        String filename = "";
        try {
            filename = URLDecoder.decode(filenameUrl, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return filename;
    }

    /**
     * 添加一个文件块信息到fileMaps
     */
    private void addBlockToMap(Block blockInfo, String uri) {
        String filename = blockInfo.getFilename();
        // 若该文件信息存在一部分
        if (fileMaps.containsKey(filename)) {
            fileMaps.get(filename).addBlock(blockInfo, uri);
        } else { // 首次添加该文件的块
            fileMaps.put(filename,
                    new FileMap(filename, blockInfo.getBlockNum(),
                            new BlockMap(filename, blockInfo.getBlockId(), uri)));
        }
    }

    /**
     * 检测并获取当前所有DataNode的URI
     */
    private void refreshDataNodeList() {
        logger.info("NameNode: get all datanodes ");
        datanodeList.clear();
        for (ServiceInstance instance : discoveryClient.getInstances("datanode-service")) {
            if (instance != null) {
                logger.info("----- datanode [uri = " + instance.getUri() + "]");
                if (!excludeServices.contains(instance.getUri().toString().replace("/", ""))) {
                    datanodeList.add(instance);
                }
            }
        }
        logger.info("datanodeList " + datanodeList.size() + ":" + datanodeList);
        logger.info("excludeServices " + excludeServices.size() + ":" + excludeServices);
    }

    /**
     * 根据当前所有DataNode发的信息刷新fileMaps
     */
    private void refreshFileMaps() {
        fileMaps = new HashMap<>();
        // 遍历所有DataNode取得信息
        for (ServiceInstance datanode : datanodeList) {
            // 获取url和api
            String uri = String.valueOf(datanode.getUri());
            String api = getApiByUri(uri, "report");
            try {
                List<Block> blockInfos = restTemplate.exchange(api, HttpMethod.GET,
                        null, new ParameterizedTypeReference<List<Block>>() {
                        }).getBody();
                for (Block blockInfo : blockInfos) {
                    addBlockToMap(blockInfo, uri);
                }
            } catch (Exception ex) {
                excludeServices.add(uri.replace("/", ""));
            }
        }
        refreshDataNodeList();

    }


    private void refreshDataBack() {
        refreshFileMaps();
        String[] result = new String[datanodeList.size()];
        logger.info("开始备份和转移");
        for (Map.Entry<String, FileMap> entry : fileMaps.entrySet()) {
            String filename = entry.getKey();
            FileMap fileMap = entry.getValue();
            for (Map.Entry<Integer, BlockMap> entry2 : fileMap.getBlockMaps().entrySet()) {
                int blockId = entry2.getKey();
                BlockMap blockMap = entry2.getValue();
                List<String> uris = blockMap.getUris();
                if (uris.size() >= configs.getCopyNum()) {
                    continue;
                }
                // 先下载该块
                int indexGet = (int) (Math.random() * uris.size());
                // 调用DataNode的方法下载块
                String uriGet = uris.get(indexGet);
                String identityUrlGet = Block.toIdentityUrl(filename, blockId);
                String api = getApiByUri(uriGet, new String("blocks/" + identityUrlGet));
                Block blockGet = restTemplate.getForEntity(api, Block.class).getBody();

                // 选择不再uris中的一个uri
                Set<String> set = new HashSet<>();
                for (ServiceInstance instance : datanodeList) {
                    set.add(instance.getUri().toString());
                }
                set.removeAll(uris);
                set.toArray(result);
                int index = (int) (Math.random() * set.size());
                String uri = result[index];
                String apiPost = getApiByUri(uri, "blocks");
                Block blockData = new Block(filename, blockGet.getBlockNum(), blockId, blockGet.getData(), blockGet.getLength());
                restTemplate.postForEntity(apiPost, blockData, String.class).getBody();
                // 添加上传块的信息到fileMaps
                Block blockInfo = new Block(filename, configs.getBlockSize(), blockGet.getBlockNum(), blockId);
                addBlockToMap(blockInfo, uri);
            }
        }
        logger.info("备份结束");
    }
}
