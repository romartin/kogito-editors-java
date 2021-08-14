/*
   Copyright (c) 2017 Ahome' Innovation Technologies. All rights reserved.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

package com.ait.lienzo.client.core.shape;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ait.lienzo.client.core.animation.AnimationProperties;
import com.ait.lienzo.client.core.animation.AnimationProperty;
import com.ait.lienzo.client.core.animation.AnimationTweener;
import com.ait.lienzo.client.core.event.NodeDragEndEvent;
import com.ait.lienzo.client.core.event.NodeDragEndHandler;
import com.ait.lienzo.client.core.event.NodeDragMoveEvent;
import com.ait.lienzo.client.core.event.NodeDragMoveHandler;
import com.ait.lienzo.client.core.event.NodeDragStartEvent;
import com.ait.lienzo.client.core.event.NodeDragStartHandler;
import com.ait.lienzo.client.core.event.NodeMouseEnterEvent;
import com.ait.lienzo.client.core.event.NodeMouseEnterHandler;
import com.ait.lienzo.client.core.event.NodeMouseExitEvent;
import com.ait.lienzo.client.core.event.NodeMouseExitHandler;
import com.ait.lienzo.client.core.shape.wires.AbstractControlHandle;
import com.ait.lienzo.client.core.shape.wires.ControlHandleList;
import com.ait.lienzo.client.core.shape.wires.IControlHandle;
import com.ait.lienzo.client.core.shape.wires.IControlHandle.ControlHandleStandardType;
import com.ait.lienzo.client.core.shape.wires.IControlHandle.ControlHandleType;
import com.ait.lienzo.client.core.shape.wires.IControlHandleFactory;
import com.ait.lienzo.client.core.shape.wires.IControlHandleList;
import com.ait.lienzo.client.core.shape.wires.decorator.IShapeDecorator;
import com.ait.lienzo.client.core.shape.wires.decorator.PointHandleDecorator;
import com.ait.lienzo.client.core.types.PathPartList;
import com.ait.lienzo.client.core.types.Point2D;
import com.ait.lienzo.client.core.types.Point2DArray;
import com.ait.lienzo.client.widget.DragConstraintEnforcer;
import com.ait.lienzo.client.widget.DragContext;
import com.ait.lienzo.shared.core.types.ColorName;
import com.ait.lienzo.shared.core.types.DragMode;
import com.ait.lienzo.shared.core.types.ShapeType;
import com.ait.lienzo.tools.client.event.HandlerRegistrationManager;
import elemental2.dom.DomGlobal;
import jsinterop.annotations.JsProperty;

public abstract class AbstractMultiPointShape<T extends AbstractMultiPointShape<T> & IMultiPointShape<T>> extends Shape<T> implements IMultiPointShape<T> {

    @JsProperty
    protected Point2DArray points;

    private final PathPartList m_list = new PathPartList();

    protected AbstractMultiPointShape(final ShapeType type) {
        super(type);
    }

    public T setControlPoints(final Point2DArray points) {
        this.points = points;

        return refresh();
    }

    public final Point2DArray getControlPoints() {
        return this.points;
    }

    @Override
    public PathPartList getPathPartList() {
        return m_list;
    }

    /**
     * Gets this triangles points.
     *
     * @return {@link Point2DArray}
     */
    public Point2DArray getPoints() {
        return this.points;
    }

    /**
     * Sets the end-points of this line.
     * The points should be a 2-element {@link Point2DArray}
     *
     * @param points
     * @return this Line
     */
    public T setPoints(final Point2DArray points) {
        this.points = points;

        return refresh();
    }

    @Override
    public T setPoint2DArray(final Point2DArray points) {
        if (points.size() > 3) {
            throw new IllegalArgumentException("Cannot have more than 3 points");
        }

        this.points = points;

        return refresh();
    }

    @Override
    public Point2DArray getPoint2DArray() {
        return getPoints();
    }

    @Override
    public boolean isControlPointShape() {
        return false;
    }

    @Override
    public IMultiPointShape<?> asMultiPointShape() {
        return this;
    }

    @Override
    public IOffsetMultiPointShape<?> asOffsetMultiPointShape() {
        return null;
    }

    @Override
    public IDirectionalMultiPointShape<?> asDirectionalMultiPointShape() {
        return null;
    }

    @Override
    public IControlHandleFactory getControlHandleFactory() {
        IControlHandleFactory factory = super.getControlHandleFactory();

        if (null != factory) {
            return factory;
        }
        return new DefaultMultiPointShapeHandleFactory(this);
    }

    @Override
    public T refresh() {
        getPathPartList().clear();
        return super.refresh();
    }

    public static final class DefaultMultiPointShapeHandleFactory implements IControlHandleFactory {

        public static final double R0 = 5;

        public static final double R1 = 10;

        public static final double SELECTION_OFFSET = R0 * 0.5;

        private static final double ANIMATION_DURATION = 150;

        private final AbstractMultiPointShape<?> m_shape;

        private DragMode m_dmode = DragMode.SAME_LAYER;

        private DefaultMultiPointShapeHandleFactory(final AbstractMultiPointShape<?> shape) {
            m_shape = shape;
        }

        @Override
        public Map<ControlHandleType, IControlHandleList> getControlHandles(ControlHandleType... types) {
            return getControlHandles(Arrays.asList(types));
        }

        @Override
        public Map<ControlHandleType, IControlHandleList> getControlHandles(final List<ControlHandleType> types) {
            if ((null == types) || (types.isEmpty())) {
                return null;
            }
            HashMap<ControlHandleType, IControlHandleList> map = new HashMap<ControlHandleType, IControlHandleList>();
            for (ControlHandleType type : types) {
                if (type == ControlHandleStandardType.HANDLE) {
                    IControlHandleList chList = getPointHandles();
                    map.put(IControlHandle.ControlHandleStandardType.HANDLE, chList);
                } else if (type == ControlHandleStandardType.POINT) {
                    IControlHandleList chList = getPointHandles();
                    map.put(IControlHandle.ControlHandleStandardType.POINT, chList);
                } else if (type == ControlHandleStandardType.OFFSET) {
                    IControlHandleList chList = getSegmentHandles();
                    map.put(IControlHandle.ControlHandleStandardType.OFFSET, chList);
                }
            }
            return map;
        }

        private IControlHandleList getSegmentHandles() {
            final ControlHandleList chlist = new ControlHandleList(m_shape);
            HandlerRegistrationManager manager = chlist.getHandlerRegistrationManager();
            ShapeXorYChanged shapeXoYChangedHandler = new ShapeXorYChanged(m_shape, chlist);
            manager.register(m_shape.addNodeDragStartHandler(shapeXoYChangedHandler));
            manager.register(m_shape.addNodeDragMoveHandler(shapeXoYChangedHandler));
            manager.register(m_shape.addNodeDragEndHandler(shapeXoYChangedHandler));

            Point2DArray points = m_shape.getPoint2DArray();
            int size = points.size();
            if (size >= 2) {
                for (int i = 1; i < size - 2; i++) {
                    SegmentHandle handleI = SegmentHandle.build(i, m_shape);
                    chlist.add(handleI);
                }
            }

            return chlist;
        }

        public static class SegmentHandle extends AbstractControlHandle {

            private final int index;
            private final AbstractMultiPointShape<?> shape;
            private SegmentXorYChanged handle;

            public static SegmentHandle build(int index, AbstractMultiPointShape<?> shape) {
                SegmentHandle handle = new SegmentHandle(index, shape);
                Point2D p0 = handle.getP0();
                Point2D p1 = handle.getP1();
                if (SegmentXorYChanged.isVertical(p0, p1) || SegmentXorYChanged.isHorizontal(p0, p1)) {
                    return handle.init();
                }
                return null;
            }

            private SegmentHandle(int index, AbstractMultiPointShape<?> shape) {
                this.index = index;
                this.shape = shape;
            }

            public SegmentHandle init() {
                Point2D p0 = getP0();
                Point2D p1 = getP1();
                handle = new SegmentXorYChanged(this, shape, p0, p1);
                final Shape<?> prim = handle.getPrimitive();
                register(prim.addNodeDragMoveHandler(handle));
                register(prim.addNodeDragStartHandler(handle));
                register(prim.addNodeDragEndHandler(handle));
                prim.setDragConstraints(handle);
                register(prim.addNodeMouseEnterHandler(new NodeMouseEnterHandler() {
                    @Override
                    public void onNodeMouseEnter(NodeMouseEnterEvent event) {
                        animate(prim, R1);
                    }
                }));
                register(prim.addNodeMouseExitHandler(new NodeMouseExitHandler() {
                    @Override
                    public void onNodeMouseExit(NodeMouseExitEvent event) {
                        animate(prim, R0);
                    }
                }));
                return this;
            }

            public void move() {
                handle.onMove();
            }

            public int getIndex() {
                return index;
            }

            public Point2D getP0() {
                return shape.getPoint2DArray().get(index);
            }

            public Point2D getP1() {
                return shape.getPoint2DArray().get(index + 1);
            }

            @Override
            public IPrimitive<?> getControl() {
                return handle.getPrimitive();
            }

            @Override
            public ControlHandleType getType() {
                return ControlHandleStandardType.HANDLE;
            }
        }

        public static class SegmentXorYChanged extends HandleXorYChanged implements DragConstraintEnforcer {
            private final Point2D p0;
            private final Point2D p1;

            public SegmentXorYChanged(IControlHandle m_handle, AbstractMultiPointShape<?> shape, Point2D p0, Point2D p1) {
                super(m_handle, shape);
                this.p0 = p0;
                this.p1 = p1;
                init();
            }

            @Override
            Shape<?> buildPrimitive() {
                double px = isHorizontal() ? p0.getX() + (p1.getX() - p0.getX()) / 2 : p1.getX();
                double py = isVertical() ? p0.getY() + (p1.getY() - p0.getY()) / 2 : p1.getY();
                return new Circle(R0)
                        .setX(m_shape.getX() + px)
                        .setY(m_shape.getY() + py)
                        .setDraggable(true)
                        .setDragMode(DragMode.SAME_LAYER);
            }


            @Override
            public void onMove() {
                double p0x = isVertical() ? m_prim.getX() - m_shape.getX() : p0.getX();
                double p0y = isHorizontal() ? m_prim.getY() - m_shape.getY() : p0.getY();
                double p1x = isVertical() ? m_prim.getX() - m_shape.getX() : p1.getX();
                double p1y = isHorizontal() ? m_prim.getY() - m_shape.getY() : p1.getY();
                p0.setX(p0x);
                p0.setY(p0y);
                p1.setX(p1x);
                p1.setY(p1y);
                m_shape.refresh();
                // TODO: getLayer().batch(); remove?
            }

            @Override
            public void startDrag(DragContext dragContext) {
            }

            @Override
            public boolean adjust(Point2D dxy) {
                Point2D delta = null;
                if (isHorizontal()) {
                    dxy.setX(0);
                    delta = dxy;
                } else if (isVertical())  {
                    dxy.setY(0);
                    delta = dxy;
                }
                return null != delta;
            }

            public boolean isHorizontal() {
                return isHorizontal(p0, p1);
            }

            public boolean isVertical() {
                return isVertical(p0, p1);
            }

            public static boolean isVertical(Point2D p0, Point2D p1) {
                return p1.getX() == p0.getX();
            }

            public static boolean isHorizontal(Point2D p0, Point2D p1) {
                return p1.getY() == p0.getY();
            }


            @Override
            ColorName getHandleColor() {
                return ColorName.GREEN;
            }

            @Override
            Point2D getPoint() {
                return p1;
            }
        }

        public static class ConnectionHandle extends AbstractPointControlHandle {

            private final boolean headNorTail;
            private final AbstractMultiPointShape<?> shape;
            private ConnectionHandleChanged handle;

            private ConnectionHandle(boolean headNorTail, AbstractMultiPointShape<?> shape) {
                this.headNorTail = headNorTail;
                this.shape = shape;
            }

            @Override
            public ConnectionHandle init() {
                handle = new ConnectionHandleChanged(headNorTail, this, shape);
                final Shape<?> prim = handle.getPrimitive();
                register(prim.addNodeDragMoveHandler(handle));
                register(prim.addNodeDragStartHandler(handle));
                register(prim.addNodeDragEndHandler(handle));
                register(prim.addNodeMouseEnterHandler(new NodeMouseEnterHandler() {
                    @Override
                    public void onNodeMouseEnter(NodeMouseEnterEvent event) {
                        animate(prim, R1);
                    }
                }));
                register(prim.addNodeMouseExitHandler(new NodeMouseExitHandler() {
                    @Override
                    public void onNodeMouseExit(NodeMouseExitEvent event) {
                        animate(prim, R0);
                    }
                }));
                return this;
            }

            @Override
            public Point2D getPoint() {
                return headNorTail ? shape.getPoints().get(0) : shape.getPoints().get(shape.getPoints().size() - 1);
            }

            public void move(double x, double y) {
                handle.move(x, y);
            }

            @Override
            public IPrimitive<?> getControl() {
                return handle.getPrimitive();
            }
        }

        public static class ConnectionHandleChanged extends HandleXorYChanged {

            private final boolean headNorTail;

            public ConnectionHandleChanged(boolean headNorTail, IControlHandle m_handle, AbstractMultiPointShape<?> m_shape) {
                super(m_handle, m_shape);
                this.headNorTail = headNorTail;
                init();
            }

            @Override
            Shape<?> buildPrimitive() {
                Circle circle = new Circle(R0)
                        .setX(m_shape.getX() + getPoint().getX())
                        .setY(m_shape.getY() + getPoint().getY())
                        .setDraggable(true)
                        .setDragMode(DragMode.SAME_LAYER);
                return PointHandleDecorator.decorateShape(circle,
                                                          IShapeDecorator.ShapeState.VALID);
            }

            @Override
            public void move(double x, double y) {
                if (m_shape instanceof OrthogonalNewPolyLine) {
                    Point2DArray points = m_shape.getPoints();
                    ((OrthogonalNewPolyLine) m_shape).update(headNorTail ? 0 : points.size() - 1, x, y);
                } else {
                    super.move(x, y);
                }
            }

            @Override
            public void onNodeDragEnd(NodeDragEndEvent event) {
                super.onNodeDragEnd(event);
                if (isActive()) {
                    if (m_shape instanceof OrthogonalNewPolyLine) {
                        ((OrthogonalNewPolyLine) m_shape).clearNonOrthogonalPoints();
                    }
                }
            }

            @Override
            ColorName getHandleColor() {
                return ColorName.GOLD;
            }

            @Override
            Point2D getPoint() {
                return headNorTail ? m_shape.getPoints().get(0) : m_shape.getPoints().get(m_shape.getPoints().size() - 1);
            }

        }

        public static abstract class HandleXorYChanged implements NodeDragStartHandler,
                                                         NodeDragMoveHandler,
                                                         NodeDragEndHandler {

            protected IControlHandle m_handle;
            protected AbstractMultiPointShape<?> m_shape;
            protected Shape<?> m_prim;

            HandleXorYChanged(IControlHandle m_handle, AbstractMultiPointShape<?> m_shape) {
                this.m_handle = m_handle;
                this.m_shape = m_shape;
            }

            void init() {
                m_prim = buildPrimitive()
                        .setFillColor(getHandleColor());
                m_prim.setSelectionStrokeOffset(SELECTION_OFFSET);
                m_prim.setSelectionBoundsOffset(SELECTION_OFFSET);
                m_prim.setFillBoundsForSelection(true);
            }

            abstract Shape<?> buildPrimitive();

            abstract ColorName getHandleColor();

            abstract Point2D getPoint();

            @Override
            public void onNodeDragStart(NodeDragStartEvent event) {
                if (isActive()) {
                    PointHandleDecorator.decorateShape(m_prim, IShapeDecorator.ShapeState.INVALID);
                    m_prim.getLayer().batch();
                }
            }

            @Override
            public void onNodeDragEnd(NodeDragEndEvent event) {
                if (isActive()) {
                    onMove();
                    PointHandleDecorator.decorateShape(m_prim, IShapeDecorator.ShapeState.VALID);
                    m_prim.getLayer().batch();
                }
            }

            @Override
            public void onNodeDragMove(NodeDragMoveEvent event) {
                if (isActive()) {
                    onMove();
                }
            }

            public void onMove() {
                double x = m_prim.getX() - m_shape.getX();
                double y = m_prim.getY() - m_shape.getY();
                move(x, y);
            }

            public  void move(double x, double y) {
                Point2D point = getPoint();
                point.setX(x);
                point.setY(y);
                m_shape.refresh();
                // TODO m_shape.getLayer().batch(); ?
            }

            protected boolean isActive() {
                return m_handle.isActive();
            }

            public Shape<?> getPrimitive() {
                return m_prim;
            }

            public Layer getLayer() {
                return m_shape.getLayer();
            }


        }



        private IControlHandleList getPointHandles() {
            final ControlHandleList chlist = new ControlHandleList(m_shape);

            HandlerRegistrationManager manager = chlist.getHandlerRegistrationManager();

            ShapeXorYChanged shapeXoYChangedHandler = new ShapeXorYChanged(m_shape, chlist);

            manager.register(m_shape.addNodeDragStartHandler(shapeXoYChangedHandler));

            manager.register(m_shape.addNodeDragMoveHandler(shapeXoYChangedHandler));

            manager.register(m_shape.addNodeDragEndHandler(shapeXoYChangedHandler));

            Point2DArray points = m_shape.getPoint2DArray();

            ConnectionHandle c0 = new ConnectionHandle(true, m_shape).init();
            chlist.add(c0);

            int size = points.size();
            for (int i = 1; i < size - 1; i++) {
                final Point2D p = points.get(i);

                final Circle prim = PointHandleDecorator.decorateShape(new Circle(R0)
                                                                               .setX(m_shape.getX() + p.getX())
                                                                               .setY(m_shape.getY() + p.getY())
                                                                               .setDraggable(true)
                                                                               .setDragMode(m_dmode),
                                                                       IShapeDecorator.ShapeState.VALID);

                prim.setSelectionStrokeOffset(SELECTION_OFFSET);
                prim.setSelectionBoundsOffset(SELECTION_OFFSET);
                prim.setFillBoundsForSelection(true);

                final int idx = i;

                // Point handle.
                chlist.add(new AbstractPointControlHandle() {
                    @Override
                    public AbstractPointControlHandle init() {
                        ControlPointChanged handler = new ControlPointChanged(chlist, m_shape, idx, p, prim, this, m_shape.getLayer());

                        register(prim.addNodeDragMoveHandler(handler));

                        register(prim.addNodeDragStartHandler(handler));

                        register(prim.addNodeDragEndHandler(handler));

                        register(prim.addNodeMouseEnterHandler(new NodeMouseEnterHandler() {
                            @Override
                            public void onNodeMouseEnter(NodeMouseEnterEvent event) {
                                animate(prim, R1);
                            }
                        }));
                        register(prim.addNodeMouseExitHandler(new NodeMouseExitHandler() {
                            @Override
                            public void onNodeMouseExit(NodeMouseExitEvent event) {
                                animate(prim, R0);
                            }
                        }));

                        prim.setDragConstraints(handler);

                        setPoint(p);

                        return this;
                    }

                    @Override
                    public IPrimitive<?> getControl() {
                        return prim;
                    }

                    @Override
                    public void destroy() {
                        super.destroy();
                    }
                }.init());
            }

            ConnectionHandle cN = new ConnectionHandle(false, m_shape).init();
            chlist.add(cN);

            return chlist;
        }

        private static void animate(final IPrimitive<?> primitive, final double radius) {
            primitive.animate(AnimationTweener.LINEAR, AnimationProperties.toPropertyList(AnimationProperty.Properties.RADIUS(radius)), ANIMATION_DURATION);
        }
    }

    public static class ShapeXorYChanged implements NodeDragStartHandler,
                                                    NodeDragMoveHandler,
                                                    NodeDragEndHandler {

        private IControlHandleList m_handleList;

        private Shape<?> m_shape;

        private boolean m_dragging;

        public ShapeXorYChanged(Shape<?> shape, IControlHandleList handleList) {
            m_shape = shape;

            m_handleList = handleList;
        }

        @Override
        public void onNodeDragMove(NodeDragMoveEvent event) {
            shapeMoved();
        }

        @Override
        public void onNodeDragStart(NodeDragStartEvent event) {
            m_dragging = true;
        }

        @Override
        public void onNodeDragEnd(NodeDragEndEvent event) {
            m_dragging = false;
        }

        private void shapeMoved() {
            for (IControlHandle handle : m_handleList) {
                Point2D p = ((AbstractPointControlHandle) handle).getPoint();

                handle.getControl().setX(m_shape.getX() + p.getX());

                handle.getControl().setY(m_shape.getY() + p.getY());
            }
            m_shape.getLayer().batch();
        }
    }

    public static class ControlPointChanged implements NodeDragStartHandler,
                                                      NodeDragMoveHandler,
                                                      NodeDragEndHandler,
                                                       DragConstraintEnforcer{

        private Shape<?> m_prim;

        private AbstractPointControlHandle m_handle;

        private Point2D m_point;

        private IControlHandleList m_handleList;

        private Shape<?> m_shape;

        private int m_index;

        private Layer m_layer;


        public ControlPointChanged(IControlHandleList handleList, Shape<?> shape, int index, Point2D point, Shape<?> prim, AbstractPointControlHandle handle, Layer layer) {
            m_handleList = handleList;

            m_shape = shape;

            m_index = index;

            m_layer = layer;

            m_prim = prim;

            m_point = point;

            m_handle = handle;
        }

        public Layer getLayer() {
            return m_layer;
        }

        @Override
        public void onNodeDragStart(NodeDragStartEvent event) {
            if ((m_handle.isActive()) && (m_handleList.isActive())) {
                PointHandleDecorator.decorateShape(m_prim, IShapeDecorator.ShapeState.INVALID);
                m_prim.getLayer().batch();
            }
        }

        @Override
        public void onNodeDragEnd(NodeDragEndEvent event) {
            if ((m_handle.isActive()) && (m_handleList.isActive())) {
                PointHandleDecorator.decorateShape(m_prim, IShapeDecorator.ShapeState.VALID);
                if (m_shape instanceof OrthogonalNewPolyLine) {
                    ((OrthogonalNewPolyLine) m_shape).clearNonOrthogonalPoints();
                }
                m_prim.getLayer().batch();
            }
        }

        @Override
        public void onNodeDragMove(NodeDragMoveEvent event) {
            if ((m_handle.isActive()) && (m_handleList.isActive())) {
                double x = m_prim.getX() - m_shape.getX();
                double y = m_prim.getY() - m_shape.getY();
                m_point.setX(x);
                m_point.setY(y);
                m_shape.refresh();
                m_shape.getLayer().batch();
            }
        }

        private double dragStartX;
        private double dragStartY;

        @Override
        public void startDrag(DragContext dragContext) {

            dragStartX = dragContext.getNode().getX();
            dragStartY = dragContext.getNode().getY();
        }

        @Override
        public boolean adjust(Point2D dxy) {
            Point2D before = null;
            if (m_index > 0) {
                before = ((OrthogonalNewPolyLine) m_shape).getPoints().get(m_index - 1);
            }
            Point2D after = null;
            if (m_index < m_handleList.size() - 1) {
                after = ((OrthogonalNewPolyLine) m_shape).getPoints().get(m_index + 1);
            }
            Point2D target = new Point2D(dragStartX + dxy.getX(), dragStartY + dxy.getY());
            DomGlobal.console.log("CHECKING ADJUST [" + dxy.getX() + ", " + dxy.getY() + "]");
            DomGlobal.console.log("TARGET ADJUST IS " + target);
            adjustPoint(before, target, after);
            dxy.setX(target.getX() - dragStartX);
            dxy.setY(target.getY() - dragStartY);
            return true;
        }

        private static final double SEGMENT_SNAP_DISTANCE = 5d;

        private static void adjustPoint(Point2D before, Point2D target, Point2D after) {

            double xDiffBefore = Double.MAX_VALUE;
            double yDiffBefore = Double.MAX_VALUE;

            if (before != null) {
                xDiffBefore = target.getX() - before.getX();
                yDiffBefore = target.getY() - before.getY();
            }

            double xDiffAfter = Double.MAX_VALUE;
            double yDiffAfter = Double.MAX_VALUE;

            if (after != null) {
                xDiffAfter = target.getX() - after.getX();
                yDiffAfter = target.getY() - after.getY();
            }

            if (Math.abs(xDiffBefore) < Math.abs(xDiffAfter) && Math.abs(xDiffBefore) <= SEGMENT_SNAP_DISTANCE) {
                target.setX(target.getX() - xDiffBefore);
            } else if (Math.abs(xDiffAfter) <= SEGMENT_SNAP_DISTANCE) {
                target.setX(target.getX() - xDiffAfter);
            }

            if (Math.abs(yDiffBefore) < Math.abs(yDiffAfter) && Math.abs(yDiffBefore) <= SEGMENT_SNAP_DISTANCE) {
                target.setY(target.getY() - yDiffBefore);
            } else if (Math.abs(yDiffAfter) <= SEGMENT_SNAP_DISTANCE) {
                target.setY(target.getY() - yDiffAfter);
            }

        }

    }

    private static abstract class AbstractPointControlHandle extends AbstractControlHandle {

        private Point2D m_point;

        public abstract AbstractPointControlHandle init();

        public Point2D getPoint() {
            return m_point;
        }

        public void setPoint(Point2D point) {
            m_point = point;
        }

        @Override
        public final ControlHandleType getType() {
            return ControlHandleStandardType.POINT;
        }
    }
}
