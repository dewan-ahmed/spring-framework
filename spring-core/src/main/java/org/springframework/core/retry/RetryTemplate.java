/*
 * Copyright 2002-2025 the original author or authors.
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

package org.springframework.core.retry;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.core.log.LogAccessor;
import org.springframework.core.retry.support.CompositeRetryListener;
import org.springframework.core.retry.support.MaxRetryAttemptsPolicy;
import org.springframework.util.Assert;
import org.springframework.util.backoff.BackOff;
import org.springframework.util.backoff.BackOffExecution;
import org.springframework.util.backoff.FixedBackOff;

/**
 * A basic implementation of {@link RetryOperations} that invokes and potentially
 * retries a {@link RetryCallback} based on a configured {@link RetryPolicy} and
 * {@link BackOff} policy.
 *
 * <p>By default, a callback will be invoked at most 3 times with a fixed backoff
 * of 1 second.
 *
 * <p>A {@link RetryListener} can be {@linkplain #setRetryListener(RetryListener)
 * registered} to intercept and inject behavior during key retry phases (before a
 * retry attempt, after a retry attempt, etc.).
 *
 * <p>All retry operations performed by this class are logged at debug level,
 * using {@code "org.springframework.core.retry.RetryTemplate"} as the log category.
 *
 * @author Mahmoud Ben Hassine
 * @author Sam Brannen
 * @since 7.0
 * @see RetryOperations
 * @see RetryPolicy
 * @see BackOff
 * @see RetryListener
 * @see RetryCallback
 */
public class RetryTemplate implements RetryOperations {

	protected final LogAccessor logger = new LogAccessor(LogFactory.getLog(getClass()));

	protected RetryPolicy retryPolicy = new MaxRetryAttemptsPolicy();

	protected BackOff backOffPolicy = new FixedBackOff(1000, Long.MAX_VALUE);

	protected RetryListener retryListener = new RetryListener() {
	};

	/**
	 * Create a new {@code RetryTemplate} with maximum 3 retry attempts and a
	 * fixed backoff of 1 second.
	 */
	public RetryTemplate() {
	}

	/**
	 * Create a new {@code RetryTemplate} with a custom {@link RetryPolicy} and a
	 * fixed backoff of 1 second.
	 * @param retryPolicy the retry policy to use
	 */
	public RetryTemplate(RetryPolicy retryPolicy) {
		Assert.notNull(retryPolicy, "RetryPolicy must not be null");
		this.retryPolicy = retryPolicy;
	}

	/**
	 * Create a new {@code RetryTemplate} with a custom {@link RetryPolicy} and
	 * {@link BackOff} policy.
	 * @param retryPolicy the retry policy to use
	 * @param backOffPolicy the backoff policy to use
	 */
	public RetryTemplate(RetryPolicy retryPolicy, BackOff backOffPolicy) {
		this(retryPolicy);
		Assert.notNull(backOffPolicy, "BackOff policy must not be null");
		this.backOffPolicy = backOffPolicy;
	}

	/**
	 * Set the {@link RetryPolicy} to use.
	 * <p>Defaults to {@code new MaxRetryAttemptsPolicy()}.
	 * @param retryPolicy the retry policy to use
	 * @see MaxRetryAttemptsPolicy
	 */
	public void setRetryPolicy(RetryPolicy retryPolicy) {
		Assert.notNull(retryPolicy, "Retry policy must not be null");
		this.retryPolicy = retryPolicy;
	}

	/**
	 * Set the {@link BackOff} policy to use.
	 * <p>Defaults to {@code new FixedBackOff(1000, Long.MAX_VALUE))}.
	 * @param backOffPolicy the backoff policy to use
	 * @see FixedBackOff
	 */
	public void setBackOffPolicy(BackOff backOffPolicy) {
		Assert.notNull(backOffPolicy, "BackOff policy must not be null");
		this.backOffPolicy = backOffPolicy;
	}

	/**
	 * Set the {@link RetryListener} to use.
	 * <p>If multiple listeners are needed, use a {@link CompositeRetryListener}.
	 * <p>Defaults to a <em>no-op</em> implementation.
	 * @param retryListener the retry listener to use
	 */
	public void setRetryListener(RetryListener retryListener) {
		Assert.notNull(retryListener, "Retry listener must not be null");
		this.retryListener = retryListener;
	}

	/**
	 * Execute the supplied {@link RetryCallback} according to the configured
	 * retry and backoff policies.
	 * <p>If the callback succeeds, its result will be returned. Otherwise, a
	 * {@link RetryException} will be thrown to the caller.
	 * @param retryCallback the callback to call initially and retry if needed
	 * @param <R> the type of the result
	 * @return the result of the callback, if any
	 * @throws RetryException if the {@code RetryPolicy} is exhausted; exceptions
	 * encountered during retry attempts are available as suppressed exceptions
	 */
	@Override
	public <R extends @Nullable Object> R execute(RetryCallback<R> retryCallback) throws RetryException {
		String callbackName = retryCallback.getName();
		// Initial attempt
		try {
			logger.debug(() -> "Preparing to execute callback '" + callbackName + "'");
			R result = retryCallback.run();
			logger.debug(() -> "Callback '" + callbackName + "' completed successfully");
			return result;
		}
		catch (Throwable initialException) {
			logger.debug(initialException,
					() -> "Execution of callback '" + callbackName + "' failed; initiating the retry process");
			// Retry process starts here
			RetryExecution retryExecution = this.retryPolicy.start();
			BackOffExecution backOffExecution = this.backOffPolicy.start();
			List<Throwable> suppressedExceptions = new ArrayList<>();

			Throwable retryException = initialException;
			while (retryExecution.shouldRetry(retryException)) {
				logger.debug(() -> "Preparing to retry callback '" + callbackName + "'");
				try {
					this.retryListener.beforeRetry(retryExecution);
					R result = retryCallback.run();
					this.retryListener.onRetrySuccess(retryExecution, result);
					logger.debug(() -> "Callback '" + callbackName + "' completed successfully after retry");
					return result;
				}
				catch (Throwable currentAttemptException) {
					this.retryListener.onRetryFailure(retryExecution, currentAttemptException);
					try {
						long duration = backOffExecution.nextBackOff();
						logger.debug(() -> "Retry callback '" + callbackName + "' failed due to '" +
								currentAttemptException.getMessage() + "'; backing off for " + duration + "ms");
						Thread.sleep(duration);
					}
					catch (InterruptedException interruptedException) {
						Thread.currentThread().interrupt();
						throw new RetryException("Unable to back off for retry callback '" + callbackName + "'",
								interruptedException);
					}
					suppressedExceptions.add(currentAttemptException);
					retryException = currentAttemptException;
				}
			}
			// The RetryPolicy has exhausted at this point, so we throw a RetryException with the
			// initial exception as the cause and remaining exceptions as suppressed exceptions.
			RetryException finalException = new RetryException("Retry policy for callback '" + callbackName +
					"' exhausted; aborting execution", initialException);
			suppressedExceptions.forEach(finalException::addSuppressed);
			this.retryListener.onRetryPolicyExhaustion(retryExecution, finalException);
			throw finalException;
		}
	}

}
