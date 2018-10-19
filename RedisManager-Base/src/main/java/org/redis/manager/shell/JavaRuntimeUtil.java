package org.redis.manager.shell;

import java.io.File;

import org.redis.manager.notify.Notify;
import org.redis.manager.shell.client.SftpInterface;
import org.redis.manager.shell.client.ShellClient;
import org.redis.manager.util.GzipUtil;

public final class JavaRuntimeUtil extends LinuxUtil{

	private static String sourceRule = "jre.{version}.{system}.gz";
	
	private String source;
	private String workpath;
	
	public JavaRuntimeUtil(SftpInterface ftp, ShellClient client, String workpath, String source, Notify notify) {
		super(ftp, client, notify);
		this.workpath = workpath;
		this.source = source;
	}
	
	/**
	 * 是否存在JAVA运行环境
	 */
	public boolean find() throws Exception{
		return find(null);
	}
	/**
	 * 是否存在JAVA运行环境
	 */
	public boolean find(String javaHome) throws Exception{
		return getJavaVersion(javaHome) != null;
	}
	/**
	 * 获取已安装JAVA的版本
	 */
	public JavaVersion getJavaVersion(String javaHome) throws Exception{
		String version = null;
		if(javaHome != null){
			version = client.exec(javaHome + "/bin/java -version");
		}else{
			version = client.exec("java -version");
		}
		if(version.startsWith("java version")){
			String versionStr = version.substring(14, version.indexOf("\n")-2);
			message(">> JAVA Version :"+ versionStr);
			return JavaVersion.get(versionStr);
		}
		return null;
	}
	
	public boolean minVersion(JavaVersion min) throws Exception {
		JavaVersion version = getJavaVersion(null);
		if(version == null){
			return false;
		}
		if(min == null || version.greaterOrEqual(min)){
			return true;
		}
		return false;
	}
	
	/**
	 * 添加JAVA环境变量
	 */
	public void setJavaEnv(String javaHome)throws Exception{
		message(">> set JAVA_HOME:" + javaHome);
		client.exec("export JAVA_HOME=" + javaHome);
		client.exec("export PATH=$JAVA_HOME/bin:$PATH");
		client.exec("export CLASSPATH=.:$JAVA_HOME/lib/dt.jar:$JAVA_HOME/lib/tools.jar");
	}
	
	/**
	 * 设置JAVA运行环境
	 */
	public void init(JavaVersion minVersion) throws Exception{
        //若已经安装了java并且版本符合最小版本要求
		if(minVersion(minVersion)){
			message(">> java is existing");
			return;
		}
        //当前路径下是否存在安装版本
		setJavaEnv(workpath + "/java");
        //若已经安装过并且版本符合最小版本要求则返回
		if(minVersion(minVersion)){
			message(">> last installed java:" + workpath + "/java");
			return;
		}
		mkdirs(workpath);
		cd(workpath);
		String system = x64()?"x64":"x86";
		String name = sourceRule.replace("{system}", system).replace("version", minVersion.keyword);
		if(!checkFile(workpath + "/jre.gz")){
			File sourceFile = new File(source + "/" + name);
			message(">> upload file:" + sourceFile.getPath());
			ftp.upload(workpath, sourceFile, "jre.gz");
		}
		if(!checkDir(workpath + "/java")){
			message(">> unzip jre");
			untar(workpath + "/jre.gz", workpath);
			String currentVersion = GzipUtil.getTarFiles(source + "/" + name).get(0);
			rename(workpath + "/" + currentVersion, workpath + "/java");
            //当前路径下是否存在安装版本
			setJavaEnv(workpath + "/java");
            //若已经安装过并且版本符合最小版本要求则返回
			if(minVersion(minVersion)){
				message(">> install JAVA_HOME:" + workpath + "/java");
				return;
			}
		}
		throw new Exception("can not find java runtime!");
	}
	
	public enum JavaVersion{
        /**
         * jre 1.6
         */
		Version1_6("1.6.", 1.6),
        /**
         * jre 1.7
         */
		Version1_7("1.7.", 1.7),
        /**
         * jre 1.8
         */
		Version1_8("1.8.", 1.8),
        /**
         * jre 1.9
         */
		Version1_9("1.9.", 1.9);
		
		private String keyword;
		private double version;
		
		JavaVersion(String keyword, double version){
			this.keyword = keyword;
			this.version = version;
		}
		
		public static JavaVersion get(String versionString){
			if(versionString == null){
				return null;
			}
			for (JavaVersion v : JavaVersion.values()) {
				if(versionString.startsWith(v.keyword)){
					return v;
				}
			}
			return null;
		}
		
		public boolean greaterOrEqual(JavaVersion version){
			return this.version >= version.version;
		}
	}
}