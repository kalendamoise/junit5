/*
 * Copyright 2015-2016 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.junit.gen5.engine.junit5.discoveryNEW;

import java.lang.reflect.AnnotatedElement;

import org.junit.gen5.engine.TestDescriptor;
import org.junit.gen5.engine.UniqueId;
import org.junit.gen5.engine.junit5.descriptor.ClassTestDescriptor;
import org.junit.gen5.engine.junit5.discovery.IsPotentialTestContainer;

public class TestContainerResolver implements ElementResolver {

	@Override
	public boolean willResolve(AnnotatedElement element, TestDescriptor parent) {
		//Do not collapse
		if (!(element instanceof Class))
			return false;
		return new IsPotentialTestContainer().test((Class<?>) element);
	}

	@Override
	public UniqueId createUniqueId(AnnotatedElement element, TestDescriptor parent) {
		Class<?> testClass = (Class<?>) element;
		return parent.getUniqueId().append("class", testClass.getName());
	}

	@Override
	public TestDescriptor resolve(AnnotatedElement element, TestDescriptor parent, UniqueId uniqueId) {
		return resolveClass((Class<?>) element, parent, uniqueId);
	}

	private TestDescriptor resolveClass(Class<?> testClass, TestDescriptor parent, UniqueId uniqueId) {
		return new ClassTestDescriptor(uniqueId, testClass);
	}
}