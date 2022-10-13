package org.lehoon.ssh;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * <p>Title: 主机环境变量</p>
 * <p>Description: </p>
 * <p>Copyright: CopyRight (c) 2020-2035</p>
 * <p>Company: lehoon Co. LTD.</p>
 * <p>Author: lehoon</p>
 * <p>Date: 2022/10/13 16:57</p>
 */
@Setter
@Getter
@NoArgsConstructor
public class SshEnvInfo implements Serializable {
    /**
     * 环境变量ID
     */
    private String envId;

    /**
     * 节点ID
     */
    private String nodeId;

    /**
     * 主机名称
     */
    private String nodeHost;

    /**
     * 用户名
     */
    private String userName;

    /**
     * 环境变量名称
     */
    private String envName;

    /**
     * 环境变量
     */
    private String envValue;
}
