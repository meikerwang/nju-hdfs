package cn.nju.dist.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

@Component
@PropertySource(value = "classpath:config.properties", encoding = "UTF-8", ignoreResourceNotFound = true)
public class Configs {

	/** 块大小 */
	@Value("${BlockSize}")
	private int BlockSize; 

	/** 副本数 */
	@Value("${CopyNum}")
	private int CopyNum; 

	/** Get方法中展示某一块时的字节数（最好小于BlockSize） */
	@Value("${ShowNum}")
	private int ShowNum;

	/** 分割文件过程中，等待文件分割结束的最长时间 */
	@Value("${AwaitTime}")
	private int AwaitTime;

	public int getBlockSize() {
		return BlockSize;
	}

	public int getCopyNum() {
		return CopyNum;
	}

	public int getShowNum() {
		return ShowNum;
	}

	public int getAwaitTime() {
		return AwaitTime;
	}
	
}
