/*
 * Copyright 2015-2016 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.junit.gen5.engine.junit5.discovery;

import static java.lang.String.format;
import static org.junit.gen5.commons.util.ReflectionUtils.loadClass;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import org.junit.gen5.commons.util.PreconditionViolationException;
import org.junit.gen5.commons.util.Preconditions;
import org.junit.gen5.commons.util.ReflectionUtils;
import org.junit.gen5.commons.util.StringUtils;
import org.junit.gen5.engine.UniqueId;

/**
 * @since 5.0
 */
class JUnit5TestableFactory {

	private static final Logger LOG = Logger.getLogger(JUnit5TestableFactory.class.getName());

	private static final IsPotentialTestContainer isPotentialTestContainer = new IsPotentialTestContainer();
	private static final IsNestedTestClass isNestedTestClass = new IsNestedTestClass();
	private static final IsTestMethod isTestMethod = new IsTestMethod();

	public static final String TYPE_CLASS = "class";
	public static final String TYPE_NESTED_CLASS = "nested-class";
	public static final String TYPE_METHOD = "method";

	JUnit5Testable fromUniqueId(UniqueId uniqueId, UniqueId engineId) {
		Preconditions.notNull(uniqueId, "Unique ID must not be null");
		List<UniqueId.Segment> parts = uniqueId.getSegments();

		//Engine ID is not used
		parts.remove(0);

		return createTestable(uniqueId, engineId, parts, null);
	}

	JUnit5Testable fromClass(Class<?> clazz, UniqueId engineId) {
		Preconditions.notNull(clazz, "Class must not be null");
		Preconditions.notNull(engineId, "Engine ID must not be null");
		if (isPotentialTestContainer.test(clazz)) {
			UniqueId uniqueId = engineId.append(TYPE_CLASS, clazz.getName());
			return new JUnit5Class(uniqueId, clazz);
		}
		if (isNestedTestClass.test(clazz)) {
			return createNestedClassTestable(clazz, clazz.getEnclosingClass(), engineId);
		}
		LOG.warning(() -> {
			String classDescription = clazz.getName();
			return format("Class '%s' is not a test container", classDescription);
		});
		return JUnit5Testable.doNothing();
	}

	private JUnit5Testable createNestedClassTestable(Class<?> testClass, Class<?> container, UniqueId engineId) {
		UniqueId uniqueId = fromClass(container, engineId).getUniqueId().append(TYPE_NESTED_CLASS,
			testClass.getSimpleName());
		return new JUnit5NestedClass(uniqueId, testClass, container);
	}

	JUnit5Testable fromMethod(Method testMethod, Class<?> clazz, UniqueId engineId) {
		if (!isTestMethod.test(testMethod)) {
			LOG.warning(() -> {
				String methodDescription = testMethod.getDeclaringClass().getName() + "#" + testMethod.getName();
				return format("Method '%s' is not a test method", methodDescription);
			});

			return JUnit5Testable.doNothing();
		}
		String methodId = String.format("%s(%s)", testMethod.getName(),
			StringUtils.nullSafeToString(testMethod.getParameterTypes()));
		UniqueId uniqueId = fromClass(clazz, engineId).getUniqueId().append(TYPE_METHOD, methodId);
		return new JUnit5Method(uniqueId, testMethod, clazz);
	}

	private JUnit5Testable createTestable(UniqueId uniqueId, UniqueId engineId, List<UniqueId.Segment> parts,
			JUnit5Testable last) {
		if (parts.isEmpty())
			return last;
		JUnit5Testable next = null;
		UniqueId.Segment head = parts.remove(0);
		switch (head.getType()) {
			case TYPE_CLASS:
				next = fromClass(findTopLevelClass(head.getValue()), engineId);
				break;
			case TYPE_NESTED_CLASS: {
				Class<?> container = ((JUnit5Class) last).getJavaClass();
				next = fromClass(findNestedClass(head.getValue(), container), engineId);
				break;
			}
			case TYPE_METHOD: {
				Class<?> container = ((JUnit5Class) last).getJavaClass();
				Optional<Method> optionalMethod = findMethod(head.getValue(), container);
				if (!optionalMethod.isPresent()) {
					LOG.warning(() -> String.format("Cannot resolve part '%s' of unique ID '%s'", head,
						uniqueId.getUniqueString()));
					return JUnit5Testable.doNothing();
				}
				next = fromMethod(optionalMethod.get(), container, engineId);
				break;
			}
			default:
				LOG.warning(() -> String.format("Cannot resolve part '%s' of unique ID '%s'", head,
					uniqueId.getUniqueString()));
				return JUnit5Testable.doNothing();
		}
		return createTestable(uniqueId, engineId, parts, next);
	}

	private Optional<Method> findMethod(String methodSpecPart, Class<?> clazz) {
		return new MethodFinder().findMethod(methodSpecPart, clazz);
	}

	private Class<?> findNestedClass(String nameExtension, Class<?> containerClass) {
		return classByName(containerClass.getName() + "$" + nameExtension);
	}

	private Class<?> findTopLevelClass(String classNamePart) {
		return loadClassByName(classNamePart);
	}

	private Class<?> classByName(String className) {
		return loadClass(className).orElseThrow(
			() -> new PreconditionViolationException(String.format("Cannot resolve class name '%s'", className)));
	}

	private Class<?> loadClassByName(String className) {
		return ReflectionUtils.loadClass(className).orElseThrow(
			() -> new PreconditionViolationException(String.format("Cannot load class '%s'", className)));
	}
}
