package org.lehoon.ssh;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Map;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: CopyRight (c) 2020-2035</p>
 * <p>Company: lehoon Co. LTD.</p>
 * <p>Author: lehoon</p>
 * <p>Date: 2022/10/13 16:54</p>
 */
@Setter
@Getter
@NoArgsConstructor
public class AbstractSshConnectInfo implements Serializable {
    /**
     * service id
     */
    protected String serviceId;

    /**
     * 部署节点编号
     */
    protected String nodeId;

    /**
     * 主机名称
     */
    protected String nodeHost;

    /**
     * ssh port
     */
    protected Integer nodePort;

    /**
     * 用户名
     */
    protected String userName;

    /**
     * ssh password
     */
    protected String password;

    /**
     * node 删除状态
     */
    protected String nodeDelete;

    /**
     * 引擎编号
     */
    protected String engineId;

    /**
     * engine port
     */
    private Integer enginePort;

    /**
     * 引擎类型
     */
    protected String engineType;

    /**
     * engine state
     */
    private String engineState;

    /**
     * 引擎删除状态
     */
    protected String engineDelete;

    /**
     * deploy state
     */
    protected String deployState;

    /**
     * service state
     */
    protected String serviceState;

    /**
     * 所属人登陆名
     */
    protected String ownerName;

    /**
     * 环境变量
     */
    protected Map<String, SshEnvInfo> envMap;
}
