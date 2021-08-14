/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.workbench.common.stunner.bpmn.client.canvas.controls;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.workbench.common.stunner.bpmn.definition.ParallelGateway;
import org.kie.workbench.common.stunner.bpmn.definition.StartNoneEvent;
import org.kie.workbench.common.stunner.bpmn.definition.UserTask;
import org.kie.workbench.common.stunner.core.graph.Node;
import org.kie.workbench.common.stunner.core.graph.content.Bounds;
import org.kie.workbench.common.stunner.core.graph.content.view.MagnetConnection;
import org.kie.workbench.common.stunner.core.graph.content.view.ViewImpl;
import org.kie.workbench.common.stunner.core.graph.impl.NodeImpl;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;

@RunWith(MockitoJUnitRunner.class)
public class BPMNCreateNodeActionTest {

    private Node gatewayNode;
    private Node eventNode;
    private Node taskNode;

    @Mock
    private BPMNCreateNodeAction connection;

    @Mock
    private MagnetConnection magnetConnection;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() {
        gatewayNode = new NodeImpl<>("gNode");
        gatewayNode.setContent(new ViewImpl<>(new ParallelGateway(),
                                              Bounds.createEmpty()));
        taskNode = new NodeImpl<>("tNode");
        taskNode.setContent(new ViewImpl<>(new UserTask(),
                                           Bounds.createEmpty()));
        eventNode = new NodeImpl<>("eNode");
        eventNode.setContent(new ViewImpl<>(new StartNoneEvent(),
                                            Bounds.createEmpty()));
        doCallRealMethod().when(connection).buildConnectionBetween(any(), any());
        // TODO ?hen(connection.getMagnetConnectionFixed(any(), any())).thenReturn(magnetConnection);
    }

    @Test
    public void testbuildConnectionBetweenGatewayAndTask() {
        connection.buildConnectionBetween(gatewayNode, taskNode);
        // TODO ?verify(connection, times(1)).getMagnetConnectionFixed(gatewayNode, taskNode);
    }

    @Test
    public void testbuildConnectionBetweenGatewayAndEvent() {
        connection.buildConnectionBetween(gatewayNode, eventNode);
        // TODO ?verify(connection, times(1)).getMagnetConnectionFixed(gatewayNode, eventNode);
    }

    @Test
    public void testbuildConnectionBetweenTaskAndGateway() {
        connection.buildConnectionBetween(taskNode, gatewayNode);
        // TODO ?verify(connection, times(1)).getMagnetConnectionFixed(taskNode, gatewayNode);
    }

    @Test
    public void testbuildConnectionBetweenEventAndGateway() {
        connection.buildConnectionBetween(eventNode, gatewayNode);
        // TODO ?verify(connection, times(1)).getMagnetConnectionFixed(eventNode, gatewayNode);
    }

    @Test
    public void testbuildConnectionBetweenGatewayAndGateway() {
        connection.buildConnectionBetween(gatewayNode, gatewayNode);
        // TODO ?verify(connection, times(1)).getMagnetConnectionFixed(gatewayNode, gatewayNode);
    }

    @Test
    public void testbuildConnectionBetweenTaskAndTask() {
        connection.buildConnectionBetween(taskNode, taskNode);
        // TODO ?verify(connection, times(1)).getMagnetConnectionCenter(taskNode, taskNode);
    }

    @Test
    public void testbuildConnectionBetweenTaskAndEvent() {
        connection.buildConnectionBetween(taskNode, eventNode);
        // TODO ?verify(connection, times(1)).getMagnetConnectionCenter(taskNode, eventNode);
    }

    @Test
    public void testbuildConnectionBetweenEventAndEvent() {
        connection.buildConnectionBetween(eventNode, eventNode);
        // TODO ?verify(connection, times(1)).getMagnetConnectionCenter(eventNode, eventNode);
    }
}
