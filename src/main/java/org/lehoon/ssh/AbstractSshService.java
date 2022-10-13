package org.lehoon.ssh;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.SCPClient;
import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.StreamGobbler;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>Title: ssh操作抽象类  具体实现逻辑可以继承该类</p>
 * <p>Description: </p>
 * <p>Copyright: CopyRight (c) 2020-2035</p>
 * <p>Company: lehoon Co. LTD.</p>
 * <p>Author: lehoon</p>
 * <p>Date: 2022/10/13 16:49</p>
 */
public abstract class AbstractSshService {
    //ssh connection
    protected Connection connection = null;
    protected AbstractSshConnectInfo sshConnectInfo;
    protected Logger log = Logger.getLogger(this.getClass());

    public AbstractSshService(AbstractSshConnectInfo sshConnectInfo) {
        this.sshConnectInfo = sshConnectInfo;
    }

    public void service() throws SshException {
        preCheck();

        try {
            login(1000);
        } catch (SshException e) {
            //log.error("登陆远程服务器失败. ", e);
            throw e;
        }

        try {
            nextCheck();
            sleep3();
            process();
            disConnect();
        } catch (SshException e) {
            log.error("部署服务操作失败, 错误信息,", e);
            rollBack();
            disConnect();
            throw e;
        } finally {
            disConnect();
        }
    }

    public void disConnect() {
        if (connection == null) return;
        connection.close();
        connection = null;
    }

    /**
     * 登陆远程服务器
     * @param timeout
     * @throws Exception
     */
    public void login(int timeout) throws SshException {
        connection = new Connection(sshConnectInfo.getNodeHost(), sshConnectInfo.getNodePort());
        try {
            connection.connect(null, timeout, 0);
            boolean isAuthenticate = connection.authenticateWithPassword(sshConnectInfo.getUserName(), sshConnectInfo.getPassword());
            if (!isAuthenticate) {
                throw new SshException("用户名密码错误,登陆失败");
            }
        } catch (IOException e) {
            log.error("登陆远程服务器失败,", e);
            if (e.getCause().getMessage().indexOf("method password not supported") != -1) {
                throw new SshException("远程服务器不支持密码认证, 请修改ssh配置文件");
            }

            throw new SshException("登陆远程服务器失败, 请检查原因");
        } catch (Exception e){
            throw new SshException("登陆远程服务器失败, 请检查原因");
        }
    }

    private void checkConnectState() throws IOException {
        if (connection == null) throw new IOException("未与服务器建立连接, 不能执行命令.");
        if (!connection.isAuthenticationComplete()) throw new IOException("与服务器连接认证未通过, 不能执行命令.");
    }

    private String readCmdResult(InputStream inputStream) {
        StringBuilder result = new StringBuilder(1024);

        try {
            InputStream stdout = new StreamGobbler(inputStream);
            BufferedReader br = new BufferedReader(new InputStreamReader(stdout));
            String cmdResLine = null;

            while (true) {
                cmdResLine = br.readLine();
                if (cmdResLine == null) {
                    break;
                }
                result.append(cmdResLine);
            }
        } catch (IOException e) {
            log.error("读取远程服务器shell指令结果失败, ", e);
        }

        return result.toString();
    }

    public boolean executeCmd(String cmd) throws SshExecException {
        try {
            return execute1(cmd);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new SshExecException(e.getMessage());
        }
    }

    private Session openSession() throws Exception {
        checkConnectState();

        try {
            return connection.openSession();
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage());
            throw new Exception("打开终端执行环境会话失败,执行命令失败.");
        }
    }

    /**
     * 执行命令
     * @param cmd
     * @return
     * @throws IOException
     */
    public String execute(String cmd) throws Exception {
        sleepCmd();
        Session session = openSession();
        preHandleSession(session);
        session.execCommand(cmd);
        String result = readCmdResult(session.getStdout());
        log.info(String.format("执行命令execute[%s],返回结果为[%s]",  cmd, result));
        session.close();
        return result;
    }

    public boolean execute1(String cmd) throws Exception {
        sleepCmd();
        Session session = openSession();
        preHandleSession(session);
        session.execCommand(cmd);
        String result = readCmdResult(session.getStdout());
        log.info(String.format("执行命令execute1[%s],返回结果为[%s]",  cmd, result));
        int code = 0;
        try {
            code = session.getExitStatus();
            log.info(String.format("执行命令execute1[%s],操作码为[%d],返回结果为[%s]",  cmd, code, result));
        } catch (Exception e) {
            log.error("执行命令出错", e.fillInStackTrace());
        } finally {
            if (session != null) session.close();
        }
        return code == 0;
    }

    /**
     * 创建目录
     * @param dir
     * @return
     */
    public String mkdir(String dir) throws SshExecException {
        String cmd = String.format("mkdir -p %s", dir);
        try {
            return execute(cmd);
        } catch (Exception e) {
            log.error("创建目录失败,", e);
            throw new SshExecException(e.getMessage());
        }
    }

    public boolean mkdir1(String dir) throws SshExecException {
        String cmd = String.format("mkdir -p %s", dir);
        return executeCmd(cmd);
    }

    public boolean createDirectory(String directory) throws SshExecException{
        return mkdir1(directory);
    }

    /**
     * 解压zip文件到指定目录
     * @param zipSrc
     * @param distDir
     * @return
     */
    public boolean unzip(String zipSrc, String distDir) throws SshExecException {
        String cmd = String.format("unzip -oq %s -d %s", zipSrc, distDir);
        return executeCmd(cmd);
    }

    /**
     * 修改文件内容
     * @param src
     * @param dist
     * @param filePath
     * @return
     * @throws Exception
     */
    public String replaceInFile(String src, String dist, String filePath) throws SshExecException {
        String cmd = String.format("sed -i 's/%s/%s/' %s", src, dist, filePath);
        try {
            return execute(cmd);
        } catch (Exception e) {
            log.error("修改文件内容失败", e);
            throw new SshExecException();
        }
    }

    public boolean rmdir(String filePath) throws SshExecException {
        if (filePath == null || filePath.length() == 0) return false;
        for (String path : SAFE_DELETE_FILESYSTEM_PATH_DEFAULT) {
            if (path.equalsIgnoreCase(filePath)) throw new SshExecException(String.format("目录[%s]不允许删除", filePath));
        }

        String cmd = String.format("rm -rf %s", filePath);
        return executeCmd(cmd);
    }

    protected boolean deleteDirectory(String directory) throws SshExecException {
        if (directory == null || directory.length() == 0 || "/".equalsIgnoreCase(directory)) return true;
        String cmd = String.format("rm -rf %s", directory);
        return executeCmd(cmd);
    }

    /**
     * 删除文件
     * @return
     * @throws IOException
     */
    protected boolean deleteFile(String filePath) throws SshExecException {
        if (filePath == null || filePath.length() == 0 || "/".equalsIgnoreCase(filePath)) return true;
        String cmd = String.format("rm -rf %s", filePath);
        return executeCmd(cmd);
    }

    protected boolean deleteFile1(String filePath) {
        try {
            return deleteFile(filePath);
        } catch (SshExecException e) {
            return false;
        }
    }

    /**
     * 文件拷贝：war包+策略zip文件
     * src=路径+文件名；des=目的路径
     *
     * */
    public void copyDoucment(String src, String des)
            throws SshExecException {
        if (connection == null) throw new SshExecException("未与服务器建立连接, 不能执行上传文件命令.");
        try {
            SCPClient scpClient = connection.createSCPClient();
            scpClient.put(src, des);
        } catch (IOException e) {
            log.error("上传文件到远程服务器失败,", e);
            throw new SshExecException();
        }
    }

    /**
     * 进入指定目录并返回目录名称
     * @param basePath
     * @return
     * @throws IOException
     */
    public String cmdPwd(String basePath) throws SshExecException {
        String cmd = String.format("cd %s && pwd", basePath);
        try {
            return execute(cmd);
        } catch (Exception e) {
            log.error(String.format("切换文件目录失败,目录[%s]不存在", basePath), e);
            throw new SshExecException();
        }
    }

    public String moveFile(String oldPath, String newPath) throws SshExecException {
        String cmd = String.format("mv %s %s", oldPath, newPath);
        try {
            return execute(cmd);
        } catch (Exception e) {
            log.error(String.format("移动文件失败,原文件[%s]目标目录[%s]不存在", oldPath, newPath), e);
            throw new SshExecException();
        }
    }

    public String lsCmd(String filePath) throws SshExecException {
        String cmd = String.format("ls %s", filePath);
        try {
            return execute(cmd);
        } catch (Exception e) {
            log.error(String.format("获取文件路径失败,文件[%s]不存在", filePath), e);
            throw new SshExecException();
        }
    }

    public boolean checkFileExist(String filePath) throws SshExecException {
        String checkFilePath = lsCmd(filePath);

        if (filePath.equalsIgnoreCase(checkFilePath)) {
            return true;
        }

        return false;
    }


    /**
     * 上传文件到远程服务器
     * @param localFileName
     * @param remotePath
     * @param remoteFileName
     * @throws IOException
     */
    public void uploadFileToRemote(String localFileName, String remotePath, String remoteFileName)
            throws SshExecException {
        if (connection == null) throw new SshExecException("未与服务器建立连接, 不能执行上传文件命令.");

        try {
            SCPClient scpClient = connection.createSCPClient();// 建立ＳＣＰ客户端：就是为了安全远程传输文件
            scpClient.put(localFileName, remoteFileName, remotePath, "0600");
        } catch (IOException e) {
            log.error("上传文件到远程服务器失败,", e);
            throw new SshExecException();
        }
    }

    public boolean uploadFile2Remote(String localFileName, String remotePath, String remoteFileName) {
        try {
            uploadFileToRemote(localFileName, remotePath, remoteFileName);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    protected boolean dos2unix(String filePath) throws SshExecException {
        if (filePath == null || filePath.length() == 0) return false;
        String cmd = String.format("dos2unix %s", filePath);
        return executeCmd(cmd);
    }

    protected String getFileName(String filename) {
        if (filename == null || filename.length() == 0) return null;
        return filename.substring(0, filename.lastIndexOf("."));
    }

    private void preHandleSession(Session session) throws IOException {
        session.requestPTY("vt100");
    }

    protected String shellDisableEchoCmd() {
        return " >> /dev/null 2>&1";
    }

    protected void sleepCmd() {
        sleep(500);
    }

    protected void sleep3() {
        sleep(2000);
    }

    protected void sleep5() {
        sleep(5000);
    }

    protected void sleep(int timeout) {
        try {
            Thread.sleep(timeout);
        } catch (InterruptedException e) {
        }
    }

    protected String fixFsFilePath(String path) {
        if (path == null || path.length() == 0) return "";
        if (!path.endsWith("/")) return path;
        return path.substring(0, path.length() - 1);
    }

    abstract protected void preCheck() throws SshException;
    abstract protected void nextCheck() throws SshException;
    abstract protected void process() throws SshException;
    abstract protected void rollBack() throws SshException;
    
    private static List<String> SAFE_DELETE_FILESYSTEM_PATH_DEFAULT
            = new ArrayList<String>(16);

    static {
        SAFE_DELETE_FILESYSTEM_PATH_DEFAULT.add("/");
        SAFE_DELETE_FILESYSTEM_PATH_DEFAULT.add("/boot");
        SAFE_DELETE_FILESYSTEM_PATH_DEFAULT.add("/home");
        SAFE_DELETE_FILESYSTEM_PATH_DEFAULT.add("/opt");
        SAFE_DELETE_FILESYSTEM_PATH_DEFAULT.add("/usr");
        SAFE_DELETE_FILESYSTEM_PATH_DEFAULT.add("/etc");
    }
}
