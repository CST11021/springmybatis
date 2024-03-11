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
package org.mybatis.spring.mapper;

import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.session.Configuration;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.support.SqlSessionDaoSupport;
import org.springframework.beans.factory.FactoryBean;

import static org.springframework.util.Assert.notNull;

/**
 * BeanFactory that enables injection of MyBatis mapper interfaces. It can be set up with a
 * SqlSessionFactory or a pre-configured SqlSessionTemplate.
 * <p>
 * Sample configuration:
 *
 * <pre class="code">
 * {@code
 *   <bean id="baseMapper" class="org.mybatis.spring.mapper.MapperFactoryBean" abstract="true" lazy-init="true">
 *     <property name="sqlSessionFactory" ref="sqlSessionFactory" />
 *   </bean>
 *
 *   <bean id="oneMapper" parent="baseMapper">
 *     <property name="mapperInterface" value="my.package.MyMapperInterface" />
 *   </bean>
 *
 *   <bean id="anotherMapper" parent="baseMapper">
 *     <property name="mapperInterface" value="my.package.MyAnotherMapperInterface" />
 *   </bean>
 * }
 * </pre>
 * <p>
 * Note that this factory can only inject <em>interfaces</em>, not concrete classes.
 *
 * @author Eduardo Macarron
 *
 * @see SqlSessionTemplate
 */
// 能够注入MyBatis mapper接口的BeanFactory。它可以设置为SqlSessionFactory或预配置的SqlSessionTemplate。
// Mybatis中的Mapper接口，其本质是一个MapperFactoryBean，MapperFactoryBean实现了FactoryBean，所以每个Mapper对象在实例化的时候会调用FactoryBean#getObject()方法，创建一个Mapper的实例
public class MapperFactoryBean<T> extends SqlSessionDaoSupport implements FactoryBean<T> {

    /** 表示对应的Mapper接口 */
    private Class<T> mapperInterface;

    /** 如果addToConfig是false，那么mapper将不会添加到MyBatis中。这意味着它必须包含在mybatisconfig.xml中。 */
    private boolean addToConfig = true;

    public MapperFactoryBean() {
        //intentionally empty
    }
    public MapperFactoryBean(Class<T> mapperInterface) {
        this.mapperInterface = mapperInterface;
    }


    @Override
    protected void checkDaoConfig() {
        super.checkDaoConfig();

        notNull(this.mapperInterface, "Property 'mapperInterface' is required");

        Configuration configuration = getSqlSession().getConfiguration();
        if (this.addToConfig && !configuration.hasMapper(this.mapperInterface)) {
            try {
                configuration.addMapper(this.mapperInterface);
            } catch (Exception e) {
                logger.error("Error while adding the mapper '" + this.mapperInterface + "' to configuration.", e);
                throw new IllegalArgumentException(e);
            } finally {
                ErrorContext.instance().reset();
            }
        }
    }
    @Override
    public T getObject() throws Exception {
        // 从mybatis的SqlSession实例中，根据mapper接口类型获取对应的mapper接口实例
        return getSqlSession().getMapper(this.mapperInterface);
    }
    @Override
    public Class<T> getObjectType() {
        return this.mapperInterface;
    }
    // MapperFactoryBean 创建的Mapper实例都是单例
    @Override
    public boolean isSingleton() {
        return true;
    }





    // getter and setter ...
    public void setMapperInterface(Class<T> mapperInterface) {
        this.mapperInterface = mapperInterface;
    }
    public Class<T> getMapperInterface() {
        return mapperInterface;
    }
    public void setAddToConfig(boolean addToConfig) {
        this.addToConfig = addToConfig;
    }
    public boolean isAddToConfig() {
        return addToConfig;
    }
}
