/*
 * Copyright 2015 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.junit.gen5.engine.junit5.execution.injection;

import java.lang.reflect.Parameter;

/**
 * @since 5.0
 */
class SimpleTypeBasedMethodArgumentResolver implements MethodArgumentResolver {

	@Override
	public boolean supports(Parameter parameter) {
		// TODO check should be based on class-objects not strings
		return parameter.getType().getName().equals(
			"org.junit.gen5.engine.junit5.execution.injection.sample.CustomType");
	}

}