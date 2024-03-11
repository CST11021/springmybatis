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

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Set;

/**
 * A {@link ClassPathBeanDefinitionScanner} that registers Mappers by
 * {@code basePackage}, {@code annotationClass}, or {@code markerInterface}. If
 * an {@code annotationClass} and/or {@code markerInterface} is specified, only
 * the specified types will be searched (searching for all interfaces will be
 * disabled).
 * <p>
 * This functionality was previously a private class of
 * {@link MapperScannerConfigurer}, but was broken out in version 1.2.0.
 *
 * @author Hunter Presnall
 * @author Eduardo Macarron
 *
 * @see MapperFactoryBean
 * @since 1.2.0
 */
public class ClassPathMapperScanner extends ClassPathBeanDefinitionScanner {

    /** 配置是否要将扫描的mapper接口注册到mybatis的Configuration中，一般会一直都是true */
    private boolean addToConfig = true;

    /** Mybatis的会话工厂，用于创建会话 */
    private SqlSessionFactory sqlSessionFactory;
    /** 表示Mybatis会话工厂的Bean名称 */
    private String sqlSessionFactoryBeanName;

    /** SqlSessionTemplate 实现了Mybatis的SqlSession接口，所以该类可以用于执行增删改查操作 */
    private SqlSessionTemplate sqlSessionTemplate;
    /** SqlSessionTemplate的Bean名称 */
    private String sqlSessionTemplateBeanName;

    /** 如果配置了注解则扫描注解注释的接口 */
    private Class<? extends Annotation> annotationClass;
    /** 如果配置了标记接口，则扫描该接口类型的Bean */
    private Class<?> markerInterface;

    /** MapperFactoryBean是一个Spring的工厂Bean，用于创建Mapper接口的实现类 */
    private MapperFactoryBean<?> mapperFactoryBean = new MapperFactoryBean<Object>();

    public ClassPathMapperScanner(BeanDefinitionRegistry registry) {
        super(registry, false);
    }


    /**
     * 注册过滤器，如果有配置注解，则扫描注解注释的接口，如果配置了标记接口，则扫描该接口类型的Bean
     */
    public void registerFilters() {
        boolean acceptAllInterfaces = true;

        // 如果有配置注解，则仅扫描注解注释的接口
        if (this.annotationClass != null) {
            addIncludeFilter(new AnnotationTypeFilter(this.annotationClass));
            acceptAllInterfaces = false;
        }

        // 如果有配置标记接口，则仅扫描该接口类型的Bean
        if (this.markerInterface != null) {
            addIncludeFilter(new AssignableTypeFilter(this.markerInterface) {
                @Override
                protected boolean matchClassName(String className) {
                    return false;
                }
            });
            acceptAllInterfaces = false;
        }

        if (acceptAllInterfaces) {
            // default include filter that accepts all classes
            addIncludeFilter(new TypeFilter() {
                @Override
                public boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory) throws IOException {
                    return true;
                }
            });
        }

        // exclude package-info.java
        addExcludeFilter(new TypeFilter() {
            @Override
            public boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory) throws IOException {
                String className = metadataReader.getClassMetadata().getClassName();
                return className.endsWith("package-info");
            }
        });
    }

    /**
     * 扫描指定包路径下的Mapper接口
     *
     * @param basePackages
     * @return
     */
    @Override
    public Set<BeanDefinitionHolder> doScan(String... basePackages) {
        // 扫描mybatis中所有的DAO接口，并解析为BeanDefinition
        Set<BeanDefinitionHolder> beanDefinitions = super.doScan(basePackages);

        if (beanDefinitions.isEmpty()) {
            logger.warn("No MyBatis mapper was found in '" + Arrays.toString(basePackages)
                        + "' package. Please check your configuration.");
        } else {
            // 修改BeanDefinition的配置信息
            processBeanDefinitions(beanDefinitions);
        }

        return beanDefinitions;
    }

    /**
     * 修改Mapper接口对象对应的BeanDefinition的信息，主要是：
     * 1、修改注册类的beanclass属性为代理类MapperFactoryBean
     * 2、设置 Mapper实例内部的 SqlSessionFactory 对象
     *
     * @param beanDefinitions
     */
    private void processBeanDefinitions(Set<BeanDefinitionHolder> beanDefinitions) {
        GenericBeanDefinition definition;
        for (BeanDefinitionHolder holder : beanDefinitions) {
            definition = (GenericBeanDefinition) holder.getBeanDefinition();

            if (logger.isDebugEnabled()) {
                logger.debug("Creating MapperFactoryBean with name '" + holder.getBeanName()
                             + "' and '" + definition.getBeanClassName() + "' mapperInterface");
            }

            // the mapper interface is the original class of the bean but, the actual class of the bean is MapperFactoryBean issue #59
            definition.getConstructorArgumentValues().addGenericArgumentValue(definition.getBeanClassName());
            // 修改注册类的beanclass属性为代理类MapperFactoryBean
            // 这里修改了mapper接口类的beandefination中的beanclass为MapperFactoryBean，它则负责生产数据类操作代理类，
            // 实际mapper接口类作为构造函数传入了 。由于只修改了beanclass,没有修改beanname，所以我们从容器中获取时候无感知的。
            definition.setBeanClass(this.mapperFactoryBean.getClass());
            definition.getPropertyValues().add("addToConfig", this.addToConfig);

            // 用于标记是否配置了sqlSessionFactoryBeanName或是sqlSessionTemplateBeanName，如果配置的工厂Bean的Name，则优先使用配置的工厂Bean，否则根据类型注入SqlSessionFactory实例和SqlSessionTemplate实例
            boolean explicitFactoryUsed = false;

            // 1、设置Mapper接口对象中的SqlSessionFactory实例，每个Mapper接口的实现都会封装一个SqlSessionFactory
            if (StringUtils.hasText(this.sqlSessionFactoryBeanName)) {
                definition.getPropertyValues().add("sqlSessionFactory", new RuntimeBeanReference(this.sqlSessionFactoryBeanName));
                explicitFactoryUsed = true;
            } else if (this.sqlSessionFactory != null) {
                definition.getPropertyValues().add("sqlSessionFactory", this.sqlSessionFactory);
                explicitFactoryUsed = true;
            }

            // 2、设置一个会话对象
            if (StringUtils.hasText(this.sqlSessionTemplateBeanName)) {
                if (explicitFactoryUsed) {
                    logger.warn("Cannot use both: sqlSessionTemplate and sqlSessionFactory together. sqlSessionFactory is ignored.");
                }
                definition.getPropertyValues().add("sqlSessionTemplate", new RuntimeBeanReference(this.sqlSessionTemplateBeanName));
                explicitFactoryUsed = true;
            } else if (this.sqlSessionTemplate != null) {
                if (explicitFactoryUsed) {
                    logger.warn("Cannot use both: sqlSessionTemplate and sqlSessionFactory together. sqlSessionFactory is ignored.");
                }
                definition.getPropertyValues().add("sqlSessionTemplate", this.sqlSessionTemplate);
                explicitFactoryUsed = true;
            }

            // 3、如果没有显式引用，就使用类型注入
            if (!explicitFactoryUsed) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Enabling autowire by type for MapperFactoryBean with name '" + holder.getBeanName() + "'.");
                }
                definition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);
            }
        }
    }

    /**
     * 判断这个BeanDefinition是否是一个可以实例化Bean组件
     *
     * @param beanDefinition
     * @return
     */
    @Override
    protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
        return beanDefinition.getMetadata().isInterface() && beanDefinition.getMetadata().isIndependent();
    }

    /**
     * 确定给定的bean定义是否具有候选资格
     *
     * @param beanName
     * @param beanDefinition
     * @return
     */
    @Override
    protected boolean checkCandidate(String beanName, BeanDefinition beanDefinition) {
        if (super.checkCandidate(beanName, beanDefinition)) {
            return true;
        } else {
            logger.warn(
                    "Skipping MapperFactoryBean with name '" + beanName + "' and '" + beanDefinition.getBeanClassName()
                    + "' mapperInterface" + ". Bean already defined with the same name!");
            return false;
        }
    }


    // setter ...

    public void setAddToConfig(boolean addToConfig) {
        this.addToConfig = addToConfig;
    }
    public void setAnnotationClass(Class<? extends Annotation> annotationClass) {
        this.annotationClass = annotationClass;
    }
    public void setMarkerInterface(Class<?> markerInterface) {
        this.markerInterface = markerInterface;
    }
    public void setSqlSessionFactory(SqlSessionFactory sqlSessionFactory) {
        this.sqlSessionFactory = sqlSessionFactory;
    }
    public void setSqlSessionTemplate(SqlSessionTemplate sqlSessionTemplate) {
        this.sqlSessionTemplate = sqlSessionTemplate;
    }
    public void setSqlSessionTemplateBeanName(String sqlSessionTemplateBeanName) {
        this.sqlSessionTemplateBeanName = sqlSessionTemplateBeanName;
    }
    public void setSqlSessionFactoryBeanName(String sqlSessionFactoryBeanName) {
        this.sqlSessionFactoryBeanName = sqlSessionFactoryBeanName;
    }
    public void setMapperFactoryBean(MapperFactoryBean<?> mapperFactoryBean) {
        this.mapperFactoryBean = mapperFactoryBean != null ? mapperFactoryBean : new MapperFactoryBean<Object>();
    }

}
