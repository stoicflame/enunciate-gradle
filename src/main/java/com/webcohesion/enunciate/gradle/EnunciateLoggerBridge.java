/*
 * Copyright 2015 Web Cohesion
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */

package com.webcohesion.enunciate.gradle;

import org.gradle.api.logging.Logger;

import com.webcohesion.enunciate.EnunciateLogger;

/**
 * Bridge Enunciate log output to Gradle logger.
 *  
 * @author Jesper Skov <jskov@jyskebank.dk>
 */
public class EnunciateLoggerBridge implements EnunciateLogger {
	private Logger gradleLogger;

	public EnunciateLoggerBridge(Logger gradleLogger) {
		this.gradleLogger = gradleLogger;
	}
	
	@Override
	public void debug(String message, Object... formatArgs) {
		if (gradleLogger.isDebugEnabled()) {
			gradleLogger.debug(String.format(message, formatArgs));
		}
	}

	@Override
	public void info(String message, Object... formatArgs) {
		if (gradleLogger.isInfoEnabled()) {
			gradleLogger.info(String.format(message, formatArgs));
		}
	}

	@Override
	public void warn(String message, Object... formatArgs) {
		if (gradleLogger.isWarnEnabled()) {
			gradleLogger.warn(String.format(message, formatArgs));
		}
	}

	@Override
	public void error(String message, Object... formatArgs) {
		if (gradleLogger.isErrorEnabled()) {
			gradleLogger.error(String.format(message, formatArgs));
		}
	}
}
