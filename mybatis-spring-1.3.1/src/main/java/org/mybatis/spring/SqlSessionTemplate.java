/**
 * Copyright 2010-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.mybatis.spring;

import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.exceptions.PersistenceException;
import org.apache.ibatis.executor.BatchResult;
import org.apache.ibatis.session.*;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.dao.support.PersistenceExceptionTranslator;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.util.List;
import java.util.Map;

import static java.lang.reflect.Proxy.newProxyInstance;
import static org.apache.ibatis.reflection.ExceptionUtil.unwrapThrowable;
import static org.mybatis.spring.SqlSessionUtils.*;
import static org.springframework.util.Assert.notNull;

/**
 * Thread safe, Spring managed, {@code SqlSession} that works with Spring
 * transaction management to ensure that that the actual SqlSession used is the
 * one associated with the current Spring transaction. In addition, it manages
 * the session life-cycle, including closing, committing or rolling back the
 * session as necessary based on the Spring transaction configuration.
 * <p>
 * The template needs a SqlSessionFactory to create SqlSessions, passed as a
 * constructor argument. It also can be constructed indicating the executor type
 * to be used, if not, the default executor type, defined in the session factory
 * will be used.
 * <p>
 * This template converts MyBatis PersistenceExceptions into unchecked
 * DataAccessExceptions, using, by default, a {@code MyBatisExceptionTranslator}.
 * <p>
 * Because SqlSessionTemplate is thread safe, a single instance can be shared
 * by all DAOs; there should also be a small memory savings by doing this. This
 * pattern can be used in Spring configuration files as follows:
 *
 * <pre class="code">
 * {@code
 * <bean id="sqlSessionTemplate" class="org.mybatis.spring.SqlSessionTemplate">
 *   <constructor-arg ref="sqlSessionFactory" />
 * </bean>
 * }
 * </pre>
 *
 * @author Putthibong Boonbong
 * @author Hunter Presnall
 * @author Eduardo Macarron
 *
 * @see SqlSessionFactory
 * @see MyBatisExceptionTranslator
 */
// SqlSessionTemplate 实现了 SqlSession 接口，所以该类可以用于执行增删改查的功能
public class SqlSessionTemplate implements SqlSession, DisposableBean {

    /** mybatis的SqlSessionFactory */
    private final SqlSessionFactory sqlSessionFactory;

    private final ExecutorType executorType;

    private final PersistenceExceptionTranslator exceptionTranslator;

    private final SqlSession sqlSessionProxy;


    public SqlSessionTemplate(SqlSessionFactory sqlSessionFactory) {
        this(sqlSessionFactory, sqlSessionFactory.getConfiguration().getDefaultExecutorType());
    }
    public SqlSessionTemplate(SqlSessionFactory sqlSessionFactory, ExecutorType executorType) {
        this(sqlSessionFactory, executorType,
             new MyBatisExceptionTranslator(sqlSessionFactory.getConfiguration().getEnvironment().getDataSource(), true));
    }
    public SqlSessionTemplate(SqlSessionFactory sqlSessionFactory, ExecutorType executorType, PersistenceExceptionTranslator exceptionTranslator) {

        notNull(sqlSessionFactory, "Property 'sqlSessionFactory' is required");
        notNull(executorType, "Property 'executorType' is required");

        this.sqlSessionFactory = sqlSessionFactory;
        this.executorType = executorType;
        this.exceptionTranslator = exceptionTranslator;
        this.sqlSessionProxy = (SqlSession) newProxyInstance(SqlSessionFactory.class.getClassLoader(),
                                                            new Class[] {SqlSession.class},
                                                            new SqlSessionInterceptor());
    }

    public SqlSessionFactory getSqlSessionFactory() {
        return this.sqlSessionFactory;
    }

    public ExecutorType getExecutorType() {
        return this.executorType;
    }

    public PersistenceExceptionTranslator getPersistenceExceptionTranslator() {
        return this.exceptionTranslator;
    }



    /**
     * Allow gently dispose bean:
     * <pre>
     * {@code
     *
     * <bean id="sqlSession" class="org.mybatis.spring.SqlSessionTemplate">
     *  <constructor-arg index="0" ref="sqlSessionFactory" />
     * </bean>
     * }
     *</pre>
     *
     * The implementation of {@link DisposableBean} forces spring context to use {@link DisposableBean#destroy()} method instead of {@link SqlSessionTemplate#close()} to shutdown gently.
     *
     * @see SqlSessionTemplate#close()
     * @see org.springframework.beans.factory.support.DisposableBeanAdapter#inferDestroyMethodIfNecessary
     * @see org.springframework.beans.factory.support.DisposableBeanAdapter#CLOSE_METHOD_NAME
     */
    @Override
    public void destroy() throws Exception {
        //This method forces spring disposer to avoid call of SqlSessionTemplate.close() which gives UnsupportedOperationException
    }

    /**
     * Proxy needed to route MyBatis method calls to the proper SqlSession got
     * from Spring's Transaction Manager
     * It also unwraps exceptions thrown by {@code Method#invoke(Object, Object...)} to
     * pass a {@code PersistenceException} to the {@code PersistenceExceptionTranslator}.
     */
    private class SqlSessionInterceptor implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            SqlSession sqlSession = getSqlSession(SqlSessionTemplate.this.sqlSessionFactory,
                                                  SqlSessionTemplate.this.executorType,
                                                  SqlSessionTemplate.this.exceptionTranslator);

            try {
                Object result = method.invoke(sqlSession, args);
                if (!isSqlSessionTransactional(sqlSession, SqlSessionTemplate.this.sqlSessionFactory)) {
                    // 即使在非脏会话上也强制提交，因为一些数据库在调用close()之前需要提交/回滚。
                    sqlSession.commit(true);
                }
                return result;
            } catch (Throwable t) {
                Throwable unwrapped = unwrapThrowable(t);
                if (SqlSessionTemplate.this.exceptionTranslator != null && unwrapped instanceof PersistenceException) {
                    // release the connection to avoid a deadlock if the translator is no loaded. See issue #22
                    closeSqlSession(sqlSession, SqlSessionTemplate.this.sqlSessionFactory);
                    sqlSession = null;
                    Throwable translated = SqlSessionTemplate.this.exceptionTranslator.translateExceptionIfPossible(
                            (PersistenceException)unwrapped);
                    if (translated != null) {
                        unwrapped = translated;
                    }
                }
                throw unwrapped;
            } finally {
                if (sqlSession != null) {
                    closeSqlSession(sqlSession, SqlSessionTemplate.this.sqlSessionFactory);
                }
            }
        }
    }







    // 实现SqlSession接口

    @Override
    public <T> T selectOne(String statement) {
        return this.sqlSessionProxy.<T>selectOne(statement);
    }
    @Override
    public <T> T selectOne(String statement, Object parameter) {
        return this.sqlSessionProxy.<T>selectOne(statement, parameter);
    }
    @Override
    public <K, V> Map<K, V> selectMap(String statement, String mapKey) {
        return this.sqlSessionProxy.<K, V>selectMap(statement, mapKey);
    }
    @Override
    public <K, V> Map<K, V> selectMap(String statement, Object parameter, String mapKey) {
        return this.sqlSessionProxy.<K, V>selectMap(statement, parameter, mapKey);
    }
    @Override
    public <K, V> Map<K, V> selectMap(String statement, Object parameter, String mapKey, RowBounds rowBounds) {
        return this.sqlSessionProxy.<K, V>selectMap(statement, parameter, mapKey, rowBounds);
    }
    @Override
    public <T> Cursor<T> selectCursor(String statement) {
        return this.sqlSessionProxy.selectCursor(statement);
    }
    @Override
    public <T> Cursor<T> selectCursor(String statement, Object parameter) {
        return this.sqlSessionProxy.selectCursor(statement, parameter);
    }
    @Override
    public <T> Cursor<T> selectCursor(String statement, Object parameter, RowBounds rowBounds) {
        return this.sqlSessionProxy.selectCursor(statement, parameter, rowBounds);
    }
    @Override
    public <E> List<E> selectList(String statement) {
        return this.sqlSessionProxy.<E>selectList(statement);
    }
    @Override
    public <E> List<E> selectList(String statement, Object parameter) {
        return this.sqlSessionProxy.<E>selectList(statement, parameter);
    }
    @Override
    public <E> List<E> selectList(String statement, Object parameter, RowBounds rowBounds) {
        return this.sqlSessionProxy.<E>selectList(statement, parameter, rowBounds);
    }
    @Override
    public void select(String statement, ResultHandler handler) {
        this.sqlSessionProxy.select(statement, handler);
    }
    @Override
    public void select(String statement, Object parameter, ResultHandler handler) {
        this.sqlSessionProxy.select(statement, parameter, handler);
    }
    @Override
    public void select(String statement, Object parameter, RowBounds rowBounds, ResultHandler handler) {
        this.sqlSessionProxy.select(statement, parameter, rowBounds, handler);
    }
    @Override
    public int insert(String statement) {
        return this.sqlSessionProxy.insert(statement);
    }
    @Override
    public int insert(String statement, Object parameter) {
        return this.sqlSessionProxy.insert(statement, parameter);
    }
    @Override
    public int update(String statement) {
        return this.sqlSessionProxy.update(statement);
    }
    @Override
    public int update(String statement, Object parameter) {
        return this.sqlSessionProxy.update(statement, parameter);
    }
    @Override
    public int delete(String statement) {
        return this.sqlSessionProxy.delete(statement);
    }
    @Override
    public int delete(String statement, Object parameter) {
        return this.sqlSessionProxy.delete(statement, parameter);
    }
    @Override
    public <T> T getMapper(Class<T> type) {
        return getConfiguration().getMapper(type, this);
    }
    @Override
    public void commit() {
        throw new UnsupportedOperationException("Manual commit is not allowed over a Spring managed SqlSession");
    }
    @Override
    public void commit(boolean force) {
        throw new UnsupportedOperationException("Manual commit is not allowed over a Spring managed SqlSession");
    }
    @Override
    public void rollback() {
        throw new UnsupportedOperationException("Manual rollback is not allowed over a Spring managed SqlSession");
    }
    @Override
    public void rollback(boolean force) {
        throw new UnsupportedOperationException("Manual rollback is not allowed over a Spring managed SqlSession");
    }
    @Override
    public void close() {
        throw new UnsupportedOperationException("Manual close is not allowed over a Spring managed SqlSession");
    }
    @Override
    public void clearCache() {
        this.sqlSessionProxy.clearCache();
    }
    @Override
    public Configuration getConfiguration() {
        return this.sqlSessionFactory.getConfiguration();
    }
    @Override
    public Connection getConnection() {
        return this.sqlSessionProxy.getConnection();
    }
    @Override
    public List<BatchResult> flushStatements() {
        return this.sqlSessionProxy.flushStatements();
    }


}
