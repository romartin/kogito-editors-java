/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates.
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
package org.kie.workbench.common.dmn.client.shape.view.decisionservice;

import com.ait.lienzo.test.LienzoMockitoTestRunner;
import org.junit.runner.RunWith;

@RunWith(LienzoMockitoTestRunner.class)
public class DecisionServiceSVGShapeViewTest {

    private static final double WIDTH = 100.0;

    private static final double HEIGHT = 200.0;

    // TODO
    /*@Mock
    private SVGPrimitiveShape svgPrimitive;

    @Mock
    private Shape shape;

    @Mock
    private Attributes attributes;

    @Mock
    private Node shapeNode;

    @Mock
    private DragHandler dragHandler;

    @Mock
    private DragContext dragContext;

    private NodeDragStartEvent nodeDragStartEvent;

    private NodeDragMoveEvent nodeDragMoveEvent;

    private NodeDragEndEvent nodeDragEndEvent;

    private DecisionServiceSVGShapeView view;

    @Before
    @SuppressWarnings("unchecked")
    public void setup() {
        when(svgPrimitive.get()).thenReturn(shape);
        when(shape.getAttributes()).thenReturn(attributes);
        when(shape.asNode()).thenReturn(shapeNode);
        when(attributes.getDouble(Attribute.WIDTH.getProperty())).thenReturn(WIDTH);
        when(attributes.getDouble(Attribute.HEIGHT.getProperty())).thenReturn(HEIGHT);

        this.nodeDragStartEvent = new NodeDragStartEvent(dragContext);
        this.nodeDragMoveEvent = new NodeDragMoveEvent(dragContext);
        this.nodeDragEndEvent = new NodeDragEndEvent(dragContext);

        this.view = new DecisionServiceSVGShapeView("name",
                                                    svgPrimitive,
                                                    WIDTH,
                                                    HEIGHT,
                                                    true);
    }

    @Test
    public void testGetDividerLineY() {
        assertThat(view.getDividerLineY()).isEqualTo(0.0);
    }

    @Test
    public void testSetDividerLineY() {
        view.setDividerLineY(50.0);

        assertThat(getMoveDividerControlHandle().getControl().getY()).isEqualTo(50.0);
    }

    private MoveDividerControlHandle getMoveDividerControlHandle() {
        final IControlHandleFactory controlHandleFactory = view.getPath().getControlHandleFactory();
        final IControlHandleList controlHandles = controlHandleFactory.getControlHandles(RESIZE).get(RESIZE);
        return StreamSupport
                .stream(controlHandles.spliterator(), false)
                .filter(ch -> ch instanceof MoveDividerControlHandle)
                .map(ch -> (MoveDividerControlHandle) ch)
                .findFirst()
                .get();
    }

    @Test
    public void testResize() {
        view.getHandlerManager().fireEvent(new WiresResizeStepEvent(view, nodeDragMoveEvent, 0, 0, WIDTH, HEIGHT));

        assertThat(getMoveDividerControlHandle().getControl().getX()).isEqualTo(WIDTH / 2);
    }

    @Test
    public void testAddDividerDragHandler() {
        view.addDividerDragHandler(dragHandler);

        final HandlerManager handlerManager = view.getHandlerManager();

        assertThat(handlerManager.isEventHandled(MoveDividerStartEvent.TYPE)).isTrue();
        assertThat(handlerManager.isEventHandled(MoveDividerStepEvent.TYPE)).isTrue();
        assertThat(handlerManager.isEventHandled(MoveDividerEndEvent.TYPE)).isTrue();

        assertThat(handlerManager.getHandlerCount(MoveDividerStartEvent.TYPE)).isEqualTo(1);
        assertThat(handlerManager.getHandlerCount(MoveDividerStepEvent.TYPE)).isEqualTo(1);
        assertThat(handlerManager.getHandlerCount(MoveDividerEndEvent.TYPE)).isEqualTo(1);

        handlerManager.getHandler(MoveDividerStartEvent.TYPE, 0).onMoveDividerStart(new MoveDividerStartEvent(view,
                                                                                                              nodeDragStartEvent));
        verify(dragHandler).start(any(DragEvent.class));

        handlerManager.getHandler(MoveDividerStepEvent.TYPE, 0).onMoveDividerStep(new MoveDividerStepEvent(view,
                                                                                                           nodeDragMoveEvent));
        verify(dragHandler).handle(any(DragEvent.class));

        handlerManager.getHandler(MoveDividerEndEvent.TYPE, 0).onMoveDividerEnd(new MoveDividerEndEvent(view,
                                                                                                        nodeDragEndEvent));
        verify(dragHandler).end(any(DragEvent.class));
    }

    @Test
    public void testShapeControlHandleFactory() {
        final IControlHandleFactory controlHandleFactory = view.getPath().getControlHandleFactory();
        assertThat(controlHandleFactory).isInstanceOf(DecisionServiceSVGShapeView.DecisionServiceControlHandleFactory.class);
    }

    @Test
    public void testShapeControlResizeHandles() {
        final IControlHandleFactory controlHandleFactory = view.getPath().getControlHandleFactory();
        final IControlHandleList controlHandles = controlHandleFactory.getControlHandles(Collections.singletonList(RESIZE)).get(RESIZE);

        assertThat(controlHandles.size()).isGreaterThan(0);
        assertThat(controlHandles).areExactly(1, new Condition<>(ch -> ch instanceof MoveDividerControlHandle,
                                                                 "Is a MoveDividerControlHandle"));
    }

    @Test
    public void testShapeControlResizeHandlersWithList() {
        final IControlHandleFactory controlHandleFactory = view.getPath().getControlHandleFactory();
        final IControlHandleList controlHandles = controlHandleFactory.getControlHandles(RESIZE).get(RESIZE);

        assertThat(controlHandles.size()).isGreaterThan(0);
        assertThat(controlHandles).areExactly(1, new Condition<>(ch -> ch instanceof MoveDividerControlHandle,
                                                                 "Is a MoveDividerControlHandle"));
    }

    @Test
    public void testShapeControlResizeHandlerMoveDividerEvents() {
        final MoveDividerControlHandle moveDividerControlHandle = getMoveDividerControlHandle();

        view.addDividerDragHandler(dragHandler);

        moveDividerControlHandle.getControl().fireEvent(nodeDragStartEvent);
        verify(dragHandler).start(any(DragEvent.class));

        moveDividerControlHandle.getControl().fireEvent(nodeDragMoveEvent);
        verify(dragHandler).handle(any(DragEvent.class));

        moveDividerControlHandle.getControl().fireEvent(nodeDragEndEvent);
        verify(dragHandler).end(any(DragEvent.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testDragConstraintHandler() {
        final MoveDividerControlHandle moveDividerControlHandle = getMoveDividerControlHandle();
        final IPrimitive control = moveDividerControlHandle.getControl();
        when(dragContext.getNode()).thenReturn(control);

        final MoveDividerDragHandler dragConstraints = (MoveDividerDragHandler) getMoveDividerControlHandle().getControl().getDragConstraints();
        dragConstraints.startDrag(dragContext);

        final DragBounds dragBounds = control.getDragBounds();
        assertThat(dragBounds.getX1()).isEqualTo(0.0);
        assertThat(dragBounds.getY1()).isEqualTo(GeneralRectangleDimensionsSet.DEFAULT_HEIGHT);
        assertThat(dragBounds.getX2()).isEqualTo(WIDTH);
        assertThat(dragBounds.getY2()).isEqualTo(HEIGHT - GeneralRectangleDimensionsSet.DEFAULT_HEIGHT);
    }*/
}
