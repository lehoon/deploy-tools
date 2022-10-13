package org.lehoon.ssh;

/**
 * <p>Title: ssh异常定义</p>
 * <p>Description: </p>
 * <p>Copyright: CopyRight (c) 2020-2035</p>
 * <p>Company: lehoon Co. LTD.</p>
 * <p>Author: lehoon</p>
 * <p>Date: 2022/10/13 16:45</p>
 */
public class SshException extends Exception {
    public SshException() {
        super("执行命令发生错误,请稍后重试");
    }

    public SshException(String message) {
        super(message);
    }

    public SshException(String message, Throwable cause) {
        super("执行命令发生错误,请稍后重试", cause);
    }
}
