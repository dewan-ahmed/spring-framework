/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.ejb.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.parsing.ComponentDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.beans.testfixture.beans.CollectingReaderEventListener;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Torsten Juergeleit
 * @author Juergen Hoeller
 * @author Chris Beams
 */
class JeeNamespaceHandlerEventTests {

	private final CollectingReaderEventListener eventListener = new CollectingReaderEventListener();

	private final DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

	private XmlBeanDefinitionReader reader;


	@BeforeEach
	void setup() {
		this.reader = new XmlBeanDefinitionReader(this.beanFactory);
		this.reader.setEventListener(this.eventListener);
		this.reader.loadBeanDefinitions(new ClassPathResource("jeeNamespaceHandlerTests.xml", getClass()));
	}


	@Test
	void testJndiLookupComponentEventReceived() {
		ComponentDefinition component = this.eventListener.getComponentDefinition("simple");
		boolean condition = component instanceof BeanComponentDefinition;
		assertThat(condition).isTrue();
	}

	@Test
	void testLocalSlsbComponentEventReceived() {
		ComponentDefinition component = this.eventListener.getComponentDefinition("simpleLocalEjb");
		boolean condition = component instanceof BeanComponentDefinition;
		assertThat(condition).isTrue();
	}

	@Test
	void testRemoteSlsbComponentEventReceived() {
		ComponentDefinition component = this.eventListener.getComponentDefinition("simpleRemoteEjb");
		boolean condition = component instanceof BeanComponentDefinition;
		assertThat(condition).isTrue();
	}

}
