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
package org.mybatis.spring.annotation;

import org.mybatis.spring.mapper.MapperFactoryBean;
import org.mybatis.spring.mapper.MapperScannerConfigurer;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * 在使用Java Config时，使用该注释来注册MyBatis映射器接口。它通过{@link MapperScannerRegistrar}执行与{@link MapperScannerConfigurer}相同的工作。
 *
 * <p>Configuration example:</p>
 * <pre class="code">
 * &#064;Configuration
 * &#064;MapperScan("org.mybatis.spring.sample.mapper")
 * public class AppConfig {
 *
 *   &#064;Bean
 *   public DataSource dataSource() {
 *     return new EmbeddedDatabaseBuilder()
 *              .addScript("schema.sql")
 *              .build();
 *   }
 *
 *   &#064;Bean
 *   public DataSourceTransactionManager transactionManager() {
 *     return new DataSourceTransactionManager(dataSource());
 *   }
 *
 *   &#064;Bean
 *   public SqlSessionFactory sqlSessionFactory() throws Exception {
 *     SqlSessionFactoryBean sessionFactory = new SqlSessionFactoryBean();
 *     sessionFactory.setDataSource(dataSource());
 *     return sessionFactory.getObject();
 *   }
 * }
 * </pre>
 *
 * @author Michael Lanyon
 * @author Eduardo Macarron
 *
 * @since 1.2.0
 * @see MapperScannerRegistrar
 * @see MapperFactoryBean
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(MapperScannerRegistrar.class)
public @interface MapperScan {

    /** 配置扫描的dao层的包路径，作用同 basePackages 属性，basePackages可以配置的多个包路径 */
    String[] value() default {};

    /** 配置扫描的dao层的包路径 */
    String[] basePackages() default {};

    /** 配置扫描特定的mapper接口 */
    Class<?>[] basePackageClasses() default {};

    /** 配置要扫描的mapper接口，被该注解修饰的接口才可以生成bean，该配置可以和markerInterface结合使用 */
    Class<? extends Annotation> annotationClass() default Annotation.class;

    /** 配置要扫描的mapper接口，该接口类的子类才可以生成bean，该配置可以和annotationClass结合使用 */
    Class<?> markerInterface() default Class.class;

    /** 配置执行SQL依赖的SqlSessionTemplate，SqlSessionTemplate实现了Mybatis的SqlSession接口 */
    String sqlSessionTemplateRef() default "";

    /** 配置生成代理类依赖的SqlSessionFactory */
    String sqlSessionFactoryRef() default "";

    /** {@link BeanNameGenerator}类用于命名Spring容器中检测到的组件。 */
    Class<? extends BeanNameGenerator> nameGenerator() default BeanNameGenerator.class;

    /** 配置生成Mapper接口代理实现的工厂Bean，默认使用MapperFactoryBean实现 */
    Class<? extends MapperFactoryBean> factoryBean() default MapperFactoryBean.class;

}
