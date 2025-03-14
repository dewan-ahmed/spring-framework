/**
 * This package contains classes used to determine the requested the media types in a request.
 *
 * <p>{@link org.springframework.web.accept.ContentNegotiationStrategy} is the main
 * abstraction for determining requested {@linkplain org.springframework.http.MediaType media types}
 * with implementations based on a
 * {@linkplain org.springframework.web.accept.ParameterContentNegotiationStrategy a request parameter}, the
 * {@linkplain org.springframework.web.accept.HeaderContentNegotiationStrategy 'Accept' header}, or a
 * {@linkplain org.springframework.web.accept.FixedContentNegotiationStrategy default content type}.
 *
 * <p>{@link org.springframework.web.accept.ContentNegotiationManager} is used to delegate to one
 * ore more of the above strategies in a specific order.
 */
@NullMarked
package org.springframework.web.accept;

import org.jspecify.annotations.NullMarked;
