package org.lehoon.ssh;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: CopyRight (c) 2020-2035</p>
 * <p>Company: lehoon Co. LTD.</p>
 * <p>Author: lehoon</p>
 * <p>Date: 2022/10/13 16:47</p>
 */
public class SshExecException extends SshException {
    public SshExecException() {
        super("执行命令发生错误,请稍后重试");
    }

    public SshExecException(String message) {
        super(message);
    }

    public SshExecException(Throwable cause) {
        super("执行命令发生错误,请稍后重试", cause);
    }

    public SshExecException(String message, Throwable cause) {
        super(message, cause);
    }
}
