/**
 * JBoss, Home of Professional Open Source.
 * Copyright 2014-2019 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
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
package org.jboss.pnc.restclient.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.org.slf4j.internal.Logger;
import com.sun.org.slf4j.internal.LoggerFactory;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.WebSocket;
import org.jboss.pnc.common.json.JsonOutputConverterMapper;
import org.jboss.pnc.dto.notification.BuildChangedNotification;
import org.jboss.pnc.dto.notification.BuildConfigurationCreation;
import org.jboss.pnc.dto.notification.BuildPushResultNotification;
import org.jboss.pnc.dto.notification.GroupBuildChangedNotification;
import org.jboss.pnc.dto.notification.Notification;
import org.jboss.pnc.dto.notification.RepositoryCreationFailure;
import org.jboss.pnc.dto.notification.SCMRepositoryCreationSuccess;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * @author <a href="mailto:jmichalo@redhat.com">Jan Michalov</a>
 */
public class VertxWebSocketClient implements WebSocketClient {

    private static final Logger log = LoggerFactory.getLogger(VertxWebSocketClient.class);

    private HttpClient httpClient;

    private WebSocket webSocketConnection;

    private Set<Dispatcher> dispatchers = new HashSet<>();

    private ObjectMapper objectMapper = JsonOutputConverterMapper.getMapper();

    public VertxWebSocketClient() {
        Vertx vertx = Vertx.vertx();
        httpClient = vertx.createHttpClient();
    }

    @Override
    public CompletableFuture<Void> connect(String webSocketServerUrl) {
        if (webSocketServerUrl == null) {
            throw new IllegalArgumentException("WebSocketServerUrl is null");
        }

        try {
            new URI(webSocketServerUrl);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("WebSocketServerUrl is not valid URI", e);
        }

        if (webSocketConnection == null || webSocketConnection.isClosed()) {
            log.trace("Already connected.");
            return CompletableFuture.completedFuture(null);
        }
        CompletableFuture<Void> future = new CompletableFuture<>();
        httpClient.webSocket(webSocketServerUrl,
                result -> {
                    if (result.succeeded()) {
                        log.debug("Connection to WebSocket server:" + webSocketServerUrl + " successful.");
                        webSocketConnection = result.result();
                        webSocketConnection.textMessageHandler(this::dispatch);
                        //Async operation complete
                        future.complete(null);
                    } else {
                        log.error("Connection to WebSocket server:" + webSocketServerUrl + " unsuccessful.", result.cause());
                        future.completeExceptionally(result.cause());
                    }
                });
        return future;
    }

    @Override
    public CompletableFuture<Void> disconnect() {
        if(webSocketConnection == null || webSocketConnection.isClosed()) {
            //already disconnected
            return CompletableFuture.completedFuture(null);
        }
        CompletableFuture<Void> future = new CompletableFuture<>();
        webSocketConnection.close((result) -> {
            if (result.succeeded()) {
                log.debug("Connection to WebSocket server successfully closed.");
                future.complete(null);
            } else {
                log.error("Connection to WebSocket server unsuccessfully closed.", result.cause());
                future.completeExceptionally(result.cause());
            }
        });
        return future;
    }

    private void dispatch(String message) {
        dispatchers.forEach((dispatcher) -> dispatcher.accept(message));
    }

    @Override
    public <T extends Notification> ListenerUnsubscriber onMessage(Class<T> notificationClass, Consumer<T> listener,
            Predicate<T>... filters) throws ConnectionClosedException {
        if (webSocketConnection == null || webSocketConnection.isClosed()) {
            throw new ConnectionClosedException("Connection to WebSocket is closed.");
        }
        //add JSON message mapping before executing the listener
        Dispatcher dispatcher = (stringMessage) -> {
            T notification;
            try {
                notification = objectMapper.readValue(stringMessage, notificationClass);
                for (Predicate<T> filter : filters) {
                    if (filter != null && !filter.test(notification)) {
                        //does not satisfy a predicate
                        return;
                    }
                }
            } catch (JsonProcessingException e) {
                //could not parse to particular class of notification, unknown or different type of notification
                //ignoring the message
                return;
            }
            listener.accept(notification);
        };
        dispatchers.add(dispatcher);
        return () -> dispatchers.remove(dispatcher);
    }


    @Override
    public <T extends Notification> CompletableFuture<T> catchSingleNotification(Class<T> notificationClass,
            Predicate<T>... filters) {
        CompletableFuture<T> future = new CompletableFuture<>();
        ListenerUnsubscriber unsubscriber = null;

        try {
            unsubscriber = onMessage(notificationClass, future::complete, filters);
        } catch (ConnectionClosedException e) {
            future.completeExceptionally(e);
            //in this case we have to set unsubscriber manually
            unsubscriber = () -> {};
        }

        final ListenerUnsubscriber finalUnsubscriber = unsubscriber;
        return future.whenComplete((notification, throwable) -> finalUnsubscriber.run());
    }

    @Override
    public ListenerUnsubscriber onBuildChangedNotification(Consumer<BuildChangedNotification> onNotification,
            Predicate<BuildChangedNotification>... filters) throws ConnectionClosedException {
        return onMessage(BuildChangedNotification.class, onNotification, filters);
    }

    @Override
    public ListenerUnsubscriber onBuildConfigurationCreation(Consumer<BuildConfigurationCreation> onNotification,
            Predicate<BuildConfigurationCreation>... filters) throws ConnectionClosedException {
        return onMessage(BuildConfigurationCreation.class, onNotification, filters);
    }

    @Override
    public ListenerUnsubscriber onBuildPushResultNotification(Consumer<BuildPushResultNotification> onNotification,
            Predicate<BuildPushResultNotification>... filters) throws ConnectionClosedException {
        return onMessage(BuildPushResultNotification.class, onNotification, filters);
    }

    @Override
    public ListenerUnsubscriber onGroupBuildChangedNotification(
            Consumer<GroupBuildChangedNotification> onNotification, Predicate<GroupBuildChangedNotification>... filters) throws ConnectionClosedException {
        return onMessage(GroupBuildChangedNotification.class, onNotification, filters);
    }

    @Override
    public ListenerUnsubscriber onRepositoryCreationFailure(Consumer<RepositoryCreationFailure> onNotification,
            Predicate<RepositoryCreationFailure>... filters) throws ConnectionClosedException {
        return onMessage(RepositoryCreationFailure.class, onNotification, filters);
    }

    @Override
    public ListenerUnsubscriber onSCMRepositoryCreationSuccess(Consumer<SCMRepositoryCreationSuccess> onNotification,
            Predicate<SCMRepositoryCreationSuccess>... filters) throws ConnectionClosedException {
        return onMessage(SCMRepositoryCreationSuccess.class, onNotification, filters);
    }

    @Override
    public CompletableFuture<BuildChangedNotification> catchBuildChangedNotification(
            Predicate<BuildChangedNotification>... filters) {
        return catchSingleNotification(BuildChangedNotification.class, filters);
    }

    @Override
    public CompletableFuture<BuildConfigurationCreation> catchBuildConfigurationCreation(
            Predicate<BuildConfigurationCreation>... filters) {
        return catchSingleNotification(BuildConfigurationCreation.class, filters);
    }

    @Override
    public CompletableFuture<BuildPushResultNotification> catchBuildPushResultNotification(
            Predicate<BuildPushResultNotification>... filters) {
        return catchSingleNotification(BuildPushResultNotification.class, filters);
    }

    @Override
    public CompletableFuture<GroupBuildChangedNotification> catchGroupBuildChangedNotification(
            Predicate<GroupBuildChangedNotification>... filters) {
        return catchSingleNotification(GroupBuildChangedNotification.class, filters);
    }

    @Override
    public CompletableFuture<RepositoryCreationFailure> catchRepositoryCreationFailure(
            Predicate<RepositoryCreationFailure>... filters) {
        return catchSingleNotification(RepositoryCreationFailure.class, filters);
    }

    @Override
    public CompletableFuture<SCMRepositoryCreationSuccess> catchSCMRepositoryCreationSuccess(
            Predicate<SCMRepositoryCreationSuccess>... filters) {
        return catchSingleNotification(SCMRepositoryCreationSuccess.class, filters);
    }

}