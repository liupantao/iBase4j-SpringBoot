package top.ibase4j.core.support.fastdfs;

import java.io.Serializable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.luhuiguo.fastdfs.conn.ConnectionManager;
import com.luhuiguo.fastdfs.conn.ConnectionPoolConfig;
import com.luhuiguo.fastdfs.conn.FdfsConnectionPool;
import com.luhuiguo.fastdfs.conn.PooledConnectionFactory;
import com.luhuiguo.fastdfs.conn.TrackerConnectionManager;
import com.luhuiguo.fastdfs.service.DefaultFastFileStorageClient;
import com.luhuiguo.fastdfs.service.DefaultTrackerClient;
import com.luhuiguo.fastdfs.service.FastFileStorageClient;
import com.luhuiguo.fastdfs.service.TrackerClient;

import top.ibase4j.core.util.DataUtil;
import top.ibase4j.core.util.InstanceUtil;
import top.ibase4j.core.util.PropertiesUtil;

/**
 * @author ShenHuaJie
 * @version 2016年6月27日 上午9:51:06
 */
@SuppressWarnings("serial")
public class FileManager implements Serializable {
    private static Logger logger = LogManager.getLogger();
    private static FileManager fileManager;
    private FastFileStorageClient fastFileStorageClient;

    public static FileManager getInstance() {
        if (fileManager == null) {
            synchronized (FileManager.class) {
                fileManager = new FileManager();
            }
        }
        return fileManager;
    }

    private FileManager() {
        PooledConnectionFactory pooledConnectionFactory = new PooledConnectionFactory();
        pooledConnectionFactory.setSoTimeout(PropertiesUtil.getInt("fdfs.soTimeout", 1000));
        pooledConnectionFactory.setConnectTimeout(PropertiesUtil.getInt("fdfs.connectTimeout", 1000));
        ConnectionPoolConfig connectionPoolConfig = new ConnectionPoolConfig();
        FdfsConnectionPool pool = new FdfsConnectionPool(pooledConnectionFactory, connectionPoolConfig);
        TrackerConnectionManager trackerConnectionManager = new TrackerConnectionManager(pool,
            InstanceUtil.newArrayList(PropertiesUtil.getString("fdfs.trackerList").split(",")));
        TrackerClient trackerClient = new DefaultTrackerClient(trackerConnectionManager);
        ConnectionManager connectionManager = new ConnectionManager(pool);
        fastFileStorageClient = new DefaultFastFileStorageClient(trackerClient, connectionManager);
    }

    public void upload(final FileModel file) {
        if (DataUtil.isEmpty(file.getGroupName())) {
            String path = fastFileStorageClient.uploadFile(file.getContent(), file.getExt()).getFullPath();
            logger.info("Upload to fastdfs success =>" + path);
            file.setRemotePath(PropertiesUtil.getString("fdfs.fileHost") + path);
        } else {
            String path = fastFileStorageClient.uploadFile(file.getGroupName(), file.getContent(), file.getExt())
                .getFullPath();
            logger.info("Upload to fastdfs success =>" + path);
            file.setRemotePath(PropertiesUtil.getString("fdfs.fileHost") + path);
        }
    }

    public FileModel getFile(String groupName, String path) {
        FileModel file = new FileModel();
        file.setContent(fastFileStorageClient.downloadFile(groupName, path));
        return file;
    }

    public void deleteFile(String groupName, String path) throws Exception {
        fastFileStorageClient.deleteFile(groupName, path);
    }
}
