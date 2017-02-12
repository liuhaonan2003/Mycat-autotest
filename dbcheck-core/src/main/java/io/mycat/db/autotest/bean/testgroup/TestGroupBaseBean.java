package io.mycat.db.autotest.bean.testgroup;

import java.io.Closeable;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

import io.mycat.db.autotest.autoTestCheckPerformance.tagServer.TagServerType;
import io.mycat.db.autotest.bean.AutoTestBaseBean;
import io.mycat.db.autotest.bean.AutoTestDataSource;
import io.mycat.db.autotest.bean.ProjectConfig;
import io.mycat.db.autotest.bean.annotation.FieldType;
import io.mycat.db.autotest.bean.testgroup.usecase.Transaction;
import io.mycat.db.autotest.bean.testgroup.usecase.UseCase;
import io.mycat.db.autotest.dataSource.UseDataSource;
import io.mycat.db.autotest.server.ioc.BeanFactory;
import io.mycat.db.autotest.utils.BeetlUtils;
import io.mycat.db.autotest.utils.LogFrameFile;
import io.mycat.db.autotest.utils.PathUtils;

import javax.sql.DataSource;

public class TestGroupBaseBean extends AutoTestBaseBean implements AutoTestDataSource ,TagServerType {
	
	public TestGroupBaseBean() {
		super(Arrays.asList("config","beforeTestGroup","afterTestGroup","beforeTest","afterTest"),  "testGroup", null);
	}

	private Config config;
	
	private TestGroupTransaction beforeTestGroup ;
	
	private TestGroupTransaction afterTestGroup;
	
	private TestGroupTransaction beforeTest;
	
	private TestGroupTransaction afterTest;

	private List<UseCase> useCases;

	private String path;

	private boolean asyn;

	private String defaultDataSource;

	@FieldType(childName="connection",childType= io.mycat.db.autotest.bean.Connection.class)
	private List<io.mycat.db.autotest.bean.Connection> connections;

	private transient Map<String,DataSource> dataSources = new java.util.concurrent.ConcurrentHashMap<>();

	public String getDefaultDataSource() {
		return defaultDataSource;
	}

	public void setDefaultDataSource(String defaultDataSource) {
		this.defaultDataSource = defaultDataSource;
	}

	public boolean isAsyn() {
		return asyn;
	}

	public void setAsyn(boolean asyn) {
		this.asyn = asyn;
	}

	public Map<String, DataSource> getDataSources() {
		return dataSources;
	}

	public void setDataSources(Map<String, DataSource> dataSources) {
		this.dataSources = dataSources;
	}

	public Config getConfig() {
		return config;
	}

	public void setConfig(Config config) {
		this.config = config;
	}

	public TestGroupTransaction getBeforeTestGroup() {
		return beforeTestGroup;
	}

	public void setBeforeTestGroup(TestGroupTransaction beforeTestGroup) {
		this.beforeTestGroup = beforeTestGroup;
	}

	public TestGroupTransaction getAfterTestGroup() {
		return afterTestGroup;
	}

	public void setAfterTestGroup(TestGroupTransaction afterTestGroup) {
		this.afterTestGroup = afterTestGroup;
	}

	public TestGroupTransaction getBeforeTest() {
		return beforeTest;
	}

	public void setBeforeTest(TestGroupTransaction beforeTest) {
		this.beforeTest = beforeTest;
	}

	public TestGroupTransaction getAfterTest() {
		return afterTest;
	}

	public void setAfterTest(TestGroupTransaction afterTest) {
		this.afterTest = afterTest;
	}

	public List<UseCase> getUseCases() {
		return useCases;
	}

	public void setUseCases(List<UseCase> useCases) {
		this.useCases = useCases;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public List<io.mycat.db.autotest.bean.Connection> getConnections() {
		return connections;
	}

	public void setConnections(List<io.mycat.db.autotest.bean.Connection> connections) {
		this.connections = connections;
	}

	@Override
	public void initDataSource() {
		if(connections != null){
			for (io.mycat.db.autotest.bean.Connection connection : connections) {
				dataSources.put(connection.getId(), UseDataSource.getDataSource(connection));
			}
		}
	}

	@Override
	public java.sql.Connection getConnection(String name) throws SQLException {
		DataSource dataSource = dataSources.get(name);
		if(dataSource == null){
			//全局
			ProjectConfig projectConfig = BeanFactory.getProjectConfig();
			return projectConfig.getConnection(name);
		}
		return dataSource.getConnection();
	}

	@Override
	public void close() throws Exception {
		Collection<DataSource> ls = dataSources.values();
		for (DataSource dataSource : ls) {
			try {
				((Closeable)dataSource).close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public boolean exec() throws Exception {
		TestGroupBaseBean tgbb = this;
		TestGroupTransaction afterTestGroup = tgbb.getAfterTestGroup();
		TestGroupTransaction beforeTestGroup = tgbb.getBeforeTestGroup();
		TestGroupTransaction afterTest = tgbb.getAfterTest();
		TestGroupTransaction beforeTest = tgbb.getBeforeTest();
		Config config = tgbb.getConfig();


		if(beforeTestGroup != null){
			Transaction transaction = beforeTestGroup.getTransaction();
			transaction.exec();
		}

		for (UseCase useCase : useCases) {

			if(beforeTest != null){
				Transaction transaction = beforeTest.getTransaction();
				transaction.exec();
			}

			useCase.exec();


			if(afterTest != null){
				Transaction transaction = afterTest.getTransaction();
				transaction.exec();
			}

		}

		if(afterTestGroup != null){
			Transaction transaction = afterTestGroup.getTransaction();
			transaction.exec();
		}


		String path = "groupUseCase/"+this.getId()+".html";
		Map<String, Object> datas = new HashMap<>();
		datas.put("useCases",useCases);
		datas.put("groupUseCase",this);
		try {
			createHtml(datas,path);
		} catch (UnsupportedEncodingException e) {
			LogFrameFile.getInstance().error("",e);
		}

		return true;
	}

	private static boolean createHtml( Map<String, Object> datas, String path) throws UnsupportedEncodingException {
		ProjectConfig projectConfig = BeanFactory.getProjectConfig();
		String outPath = PathUtils.getPath(projectConfig.getPath(),projectConfig.getOutPath());
		String templateid = "groupUseCase.html";
		BeetlUtils.createpathTemplate(outPath + "/" + path,templateid,datas);
		return true;
	}
}