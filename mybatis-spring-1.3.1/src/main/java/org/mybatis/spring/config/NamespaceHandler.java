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
package org.mybatis.spring.config;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * MyBastic命名空间处理器
 *
 * @author Lishu Luo
 *
 * @see MapperScannerBeanDefinitionParser
 * @since 1.2.0
 */
public class NamespaceHandler extends NamespaceHandlerSupport {

    /**
     * {@inheritDoc}
     */
    @Override
    public void init() {
        // 注册<scan>标签扫描的注解解析器
        registerBeanDefinitionParser("scan", new MapperScannerBeanDefinitionParser());
    }

}
