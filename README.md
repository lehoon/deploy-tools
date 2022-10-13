# java通过ganymed-ssh2实现linux执行命令
## maven依赖
```xml
    <dependency>
      <groupId>ch.ethz.ganymed</groupId>
      <artifactId>ganymed-ssh2</artifactId>
      <version>build210</version>
    </dependency>
```
## 使用说明
继承类AbstractSshService实现自己的业务逻辑
```java
import com.lehoon.AbstractSshService;

/**
 * <p>Title: tomcat远程部署</p>
 * <p>Description: </p>
 * <p>Copyright: CopyRight (c) 2020-2035</p>
 * <p>Company: lehoon Co. LTD.</p>
 * <p>Author: lehoon</p>
 * <p>Date: 2022/1/20 16:26</p>
 */
class SSHDeployTomcatService extends AbstractSshService
{
   public BtpSSHDeployTomcatService(DeployServiceDeployVoEntity serviceDeployVo) {
        super(serviceDeployVo);
    }

    @Override
    protected void preCheck() throws DeployException {
       if (deployVo == null)
            throw new DeployException(DeployOperateCode.BTP_DEPLOY_OPERATE_CODE_FAIL_SERVICE_INVOKE_REQUEST_NOTSET.getMessage());
        if (deployVo.getStrategyFile() == null)
            throw new DeployException(DeployOperateCode.BTP_DEPLOY_OPERATE_CODE_FAIL_STRATEGY_INFO_NOTFILL.getMessage());
        if (deployVo.getStrategyFile().getStrategyFilePath() == null)
            throw new DeployException(DeployOperateCode.BTP_DEPLOY_OPERATE_CODE_FAIL_STRATEGY_FILE_NOTSET.getMessage());

        if (!FileUtils.checkFileExist(deployVo.getStrategyFile().getStrategyFilePath())) {
            log.error("部署策略文件不存在,部署失败.");
            throw new DeployException(DeployOperateCode.BTP_DEPLOY_OPERATE_CODE_FAIL_STRATEGY_FILE_NOTEXIST.getMessage());
        }

        if (!checkNodeNetState()) {
            throw new DeployException(DeployOperateCode.BTP_DEPLOY_OPERATE_CODE_FAIL_NODE_DISCONNECT.getMessage());
        }

        if (isDeployBasePathEnvEmpty())
            throw new DeployException(DeployOperateCode.BTP_DEPLOY_OPERATE_CODE_FAIL_NODE_ENV_DEPLOYHOME_PATH_NOTSET.getMessage());

        if (!isDeployBasePathValid())
            throw new DeployException(DeployOperateCode.BTP_DEPLOY_OPERATE_CODE_FAIL_NODE_ENV_DEPLOYHOME_PATH_INVALID.getMessage());
    }

    @Override
    protected void nextCheck() throws DeployException {
        if (!createDirectory(deployBasePath)) {
            log.error(String.format("创建服务部署根目录失败,%s", deployBasePath));
        }

        if (!checkDeployBasePathExist()) {
            throw new DeployException(DeployOperateCode.BTP_DEPLOY_OPERATE_CODE_FAIL_NODE_DEPLOYPATH_NOT_EXIST.getMessage());
        }

        updateDeployPath();
        if (!deleteDirectory(deployPath)) {
            log.error(String.format("删除旧的服务部署目录失败,%s", deployPath));
        }

        if (!createDirectory(deployPath)) {
            log.error(String.format("创建服务部署目录失败%s", deployPath));
            throw new DeployException(DeployOperateCode.BTP_DEPLOY_OPERATE_CODE_FAIL_NODE_DEPLOYPATH_CREATE_ERROR.getMessage());
        }

        if (!checkDeployPathExist()) {
            log.error(String.format("创建服务部署目录失败%s", deployPath));
            throw new DeployException(DeployOperateCode.BTP_DEPLOY_OPERATE_CODE_FAIL_NODE_DEPLOYPATH_NOT_EXIST.getMessage());
        }

        log.info(String.format("部署服务[%s], 部署目录[%s]", deployVo.getServiceId(), deployBasePath));
    }
    
    /**
    * 具体部署逻辑代码
    */
    @Override
    protected void process() throws DeployException {
        String newZipFileName = getNewZipFileName();
        String uploadTempPath = String.format("%s/temp/", deployBasePath);

        if(!createDirectory(uploadTempPath)) {
            log.error("创建部署服务策略文件下载目录失败, {}", uploadTempPath);
            throw new DeployException(DeployOperateCode.BTP_DEPLOY_OPERATE_CODE_FAIL_NODE_DOWNLOAD_TEMPPATH_CREATE_ERROR.getMessage());
        }

        if (!checkDirectoryPathExist(uploadTempPath)) {
            log.error("创建部署服务策略文件下载目录失败, {}", uploadTempPath);
            throw new DeployException(DeployOperateCode.BTP_DEPLOY_OPERATE_CODE_FAIL_NODE_DOWNLOAD_TEMPPATH_CREATE_ERROR.getMessage());
        }

        uploadTempPath = fixFsFilePath(uploadTempPath);

        if (!uploadFile2Remote(deployVo.getStrategyFile().getStrategyFilePath(), uploadTempPath, newZipFileName)) {
            log.error("上传部署服务策略压缩包到远程服务器失败, {}", uploadTempPath);
            throw new DeployException(DeployOperateCode.BTP_DEPLOY_OPERATE_CODE_FAIL_STRATEGY_FILE_DOWNLOAD_ERROR.getMessage());
        }

        //检查文件完整性
        String zipPath = String.format("%s/%s", uploadTempPath, newZipFileName);
        if (!checkFileExist(zipPath)) {
            log.error("上传部署服务策略压缩包到远程服务器失败, 部署失败");
            throw new DeployException(DeployOperateCode.BTP_DEPLOY_OPERATE_CODE_FAIL_STRATEGY_FILE_DOWNLOAD_ERROR.getMessage());
        }

        //解压文件
        if (!unzip(zipPath, deployPath)) {
            log.error("解压策略服务压缩包失败,部署服务失败");
            throw new DeployException(DeployOperateCode.BTP_DEPLOY_OPERATE_CODE_FAIL_STRATEGY_FILE_UNZIP_ERROR.getMessage());
        }

        log.info(String.format("部署策略服务目录为[%s]", deployPath));
        checkDeployServiceFile();
        log.info("部署服务完成,开始修改部署服务的文件参数. {}", deployVo);
        modifyFile();
        log.info("部署服务完成,修改部署服务的文件参数完成. {}", deployVo);
        if(!deleteFile1(zipPath)) {
            log.error(String.format("删除部署服务关联策略服务压缩包失败,%s", zipPath));
        }
        deleteLfsStrategyFile();    
    }   

    @Override
    protected void rollBack() throws DeployException {
        try {
            deleteDirectory(deployPath);
            deleteFile(String.format("%s/temp/%s", deployBasePath, getNewZipFileName()));
        } catch (Exception e) {
            log.error("部署失败, 回滚操作失败", e);
        }
        deleteLfsStrategyFile();
    }

    @Override
    public void deploy() throws BtpDeployException {
        service();
    }
}
```

## 异常
操作失败需要捕获异常，然后从异常拿具体的错误信息。
```java
public class BtpServiceDeployTask extends BtpAbstractServiceTask {
        @Override
        public void service() {
            //获取具体的部署逻辑对象
            IBtpDeployService btpDeployService = BtpServiceFactory.getDeployService(request);
    
            try {
                btpDeployService.deploy();
                log.info("部署服务成功{}", request);
                createServiceDeployOperateLog("在服务器上部署成功");
            } catch (Exception e) {
                log.error("部署服务失败", e);
                createServiceDeployOperateLog(String.format("在服务器上部署失败,%s", e.getMessage()));
            }
        }
        
}

```



