# 微服务架构的分布式文件系统设计与实现

https://github.com/njuics/sa-2018/wiki/%E8%AF%BE%E7%A8%8B%E5%A4%A7%E4%BD%9C%E4%B8%9A

- 基于Spring Boot实现NameNode和DataNode两个服务
- 在Spring Cloud微服务平台上运行一个NameNode实例和多个DataNode实例（无需考虑NameNode单点失效问题）
- NameNode提供REST风格接口与用户交互，实现用户文件上传、下载、删除，DataNode不与用户直接交互（无需考虑NameNode的IO瓶颈问题）
- NameNode将用户上传文件文件拆为固定大小的存储块，分散存储在各个DataNode上，每个块保存若干副本。块大小和副本数可通过系统参数配置。
- DataNode服务可弹性扩展，每次启动一个DataNode服务NameNode可发现并将其纳入整个系统
- NameNode负责检查各DataNode健康状态，需模拟某个DataNode下线时NameNode自动在其他DataNode上复制（迁移）该下线服务原本保存的数据块
- NameNode在管理数据块存储和迁移过程中应实现一定策略尽量保持各DataNode的负载均衡