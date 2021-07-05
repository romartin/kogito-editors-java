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
import com.ait.lienzo.client.core.event.NodeMouseDoubleClickEvent;
import com.ait.lienzo.client.core.event.NodeMouseDoubleClickHandler;
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
import com.ait.lienzo.client.core.types.PathPartList;
import com.ait.lienzo.client.core.types.Point2D;
import com.ait.lienzo.client.core.types.Point2DArray;
import com.ait.lienzo.client.widget.DragConstraintEnforcer;
import com.ait.lienzo.client.widget.DragContext;
import com.ait.lienzo.shared.core.types.ColorName;
import com.ait.lienzo.shared.core.types.Direction;
import com.ait.lienzo.shared.core.types.DragMode;
import com.ait.lienzo.shared.core.types.ShapeType;
import com.ait.lienzo.tools.client.event.HandlerRegistrationManager;
import elemental2.dom.DomGlobal;
import jsinterop.annotations.JsProperty;

public abstract class AbstractMultiPointShape<T extends AbstractMultiPointShape<T> & IMultiPointShape<T>>
        extends Shape<T> implements IMultiPointShape<T> {

    @JsProperty
    protected Point2DArray points;

    private final PathPartList m_list = new PathPartList();

    protected AbstractMultiPointShape(final ShapeType type) {
        super(type);
    }

    public final T setControlPoints(final Point2DArray points) {
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

        public static final double R0 = 6;

        public static final double R1 = 10;

        public static final double SELECTION_OFFSET = R0 * 0.5;

        private static final double ANIMATION_DURATION = 100;

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
                } else if (type == ControlHandleStandardType.CONNECTOR) {
                    IControlHandleList chList = getConnectorOffsetPointHandles();
                    map.put(IControlHandle.ControlHandleStandardType.CONNECTOR, chList);
                }
            }
            return map;
        }

        public IControlHandleList getConnectorOffsetPointHandlesOtrhoPoints() {
            if (m_shape instanceof OrthogonalMultipointPolyLine) {
                // return getPointHandles(((OrthogonalMultipointPolyLine) m_shape).orthogonalPoints, new ControlHandleList(m_shape));
            }
            return getPointHandles();
        }

        private IControlHandleList getConnectorOffsetPointHandles() {
            final ControlHandleList chlist = new ControlHandleList(m_shape);
            Point2DArray points = m_shape.getPoint2DArray();

            if (points.size() == 2) {
                Point2DArray ps = new Point2DArray();
                ps.push(points.get(0));
                ps.push(points.get(1));
                return getPointHandles(ps, chlist);
            } else if (points.size() > 2) {

                Point2D first = points.get(0);
                Circle firstPrim = createCircle();
                firstPrim.setX(firstPrim.getX() + first.getX());
                firstPrim.setY(firstPrim.getY() + first.getY());
                chlist.add(new LineOffsetControlHandle(m_shape, Direction.NONE, chlist, 0, first, first, firstPrim));

                for (int i = 0; i < (points.size() - 1); i++) {
                    Point2D p0 = points.get(i);
                    Point2D p1 = points.get(i + 1);

                    final Circle prim = createCircle();

                    Direction direction = Direction.NONE;
                    if (p0.getX() == p1.getX()) {
                        // Vertical orientation
                        double py = (p1.getY() - p0.getY()) / 2;
                        prim.setX(prim.getX() + p0.getX());
                        prim.setY(prim.getY() + p0.getY() + py);
                        direction = p1.getY() > p0.getY() ? Direction.NORTH : Direction.SOUTH;
                    } else if (p0.getY() == p1.getY()) {
                        // Horizontal orientation
                        double px = (p1.getX() - p0.getX()) / 2;
                        prim.setX(prim.getX() + p0.getX() + px);
                        prim.setY(prim.getY() + p0.getY());
                        direction = p1.getX() > p0.getX() ? Direction.EAST : Direction.WEST;
                    }

                    chlist.add(new LineOffsetControlHandle(m_shape, direction, chlist, i, p0, p1, prim));
                }

                Point2D last = points.get(points.size() - 1);
                Circle lastPrim = createCircle();
                lastPrim.setX(lastPrim.getX() + last.getX());
                lastPrim.setY(lastPrim.getY() + last.getY());
                chlist.add(new LineOffsetControlHandle(m_shape, Direction.NONE, chlist, 0, last, last, lastPrim));

            }



            return chlist;
        }

        private Circle createCircle() {
            final Circle prim = new Circle(R0)
                    .setX(m_shape.getX())
                    .setY(m_shape.getY())
                    .setDraggable(true)
                    .setFillColor(ColorName.YELLOW);
            return prim;
        }

        public static class LineOffsetControlHandle extends AbstractControlHandle {

            private final AbstractMultiPointShape shape;
            private final Direction direction;
            private final ControlHandleList handleList;
            private final int index;
            private final Point2D p0;
            private final Point2D p1;
            private final Shape<?> prim;

            public LineOffsetControlHandle(AbstractMultiPointShape shape, Direction direction, ControlHandleList handleList, int index, Point2D p0, Point2D p1, Shape<?> prim) {
                this.shape = shape;
                this.direction = direction;
                this.handleList = handleList;
                this.index = index;
                this.p0 = p0;
                this.p1 = p1;
                this.prim = prim;
                init();
            }

            private double getDistance() {
                if (isVertical()) {
                    return (p1.getX() - p0.getX()) / 2;
                } else if (isHorizontal()) {
                    return (p1.getY() - p0.getY()) / 2;
                }
                return 0;
            }

            private boolean isVertical() {
                return Direction.NORTH.equals(direction) || Direction.SOUTH.equals(direction);
            }

            private boolean isHorizontal() {
                return Direction.EAST.equals(direction) || Direction.WEST.equals(direction);
            }

            @Override
            public IControlHandle setLocation(double x, double y) {
                setX(x);
                setY(y);
                return this;
            }

            public IControlHandle setX(double x) {
                if (isVertical()) {
                    DomGlobal.console.log("CONNECTOR HANDLE - SETTING X [" + x + "]");
                    p0.setX(x);
                    p1.setX(x);
                } else if (isHorizontal())  {
                    if (index == 0) {
                        p0.setX(x);
                    } else {
                        p1.setX(x);
                    }
                } else {
                    p0.setX(x);
                    p1.setX(x);
                }
                return this;
            }

            public IControlHandle setY(double y) {
                if (isHorizontal()) {
                    DomGlobal.console.log("CONNECTOR HANDLE - SETTING Y [" + y + "]");
                    p0.setY(y);
                    p1.setY(y);
                } else if (isVertical()) {
                    if (index == 0) {
                        p0.setY(y);
                    } else {
                        p1.setY(y);
                    }
                } else {
                    p0.setY(y);
                    p1.setY(y);
                }
                return this;
            }

            private LineOffsetControlHandle initDragConstraints() {
                prim.setDragConstraints(new DragConstraintEnforcer() {
                    @Override
                    public void startDrag(DragContext dragContext) {
                    }

                    @Override
                    public boolean adjust(Point2D dxy) {
                        if (isVertical()) {
                            dxy.setY(0);
                            return true;
                        } else if (isHorizontal()) {
                            dxy.setX(0);
                            return true;
                        }
                        return false;
                    }
                });
                return this;
            }

            private void init() {
                initDragConstraints();
                // TODO: registrations below
                prim.addNodeDragStartHandler(new NodeDragStartHandler() {
                    @Override
                    public void onNodeDragStart(NodeDragStartEvent event) {
                        if (isEnabled()) {
                            DomGlobal.console.log("OffsetCP - Start dragging");
                            prim.setFillColor(ColorName.YELLOWGREEN);
                            shape.getLayer().batch();
                        }
                    }
                });

                prim.addNodeDragMoveHandler(new NodeDragMoveHandler() {
                    @Override
                    public void onNodeDragMove(NodeDragMoveEvent event) {
                        if (isEnabled()) {
                            if (isVertical()) {
                                double d = getDistance();
                                setLocation(prim.getX() - shape.getX() - d, prim.getY());
                            } else if (isHorizontal()) {
                                double d = getDistance();
                                setLocation(prim.getX(), prim.getY() - shape.getY() - d);
                            } else {
                                double d = 0;
                                setLocation(prim.getX() - shape.getX() - d, prim.getY() - shape.getY() - d);
                            }
                            DomGlobal.console.log("OffsetCP - set P0 [" + p0 + "]");
                            DomGlobal.console.log("OffsetCP - set P1 [" + p1 + "]");
                            shape.refresh();
                            shape.getLayer().batch();
                        }
                    }
                });
                prim.addNodeDragEndHandler(new NodeDragEndHandler() {
                    @Override
                    public void onNodeDragEnd(NodeDragEndEvent event) {
                        if (isEnabled()) {
                            DomGlobal.console.log("OffsetCP - End dragging");
                            prim.setFillColor(ColorName.YELLOW);
                            if (shape instanceof OrthogonalPolyLine) {
                                shape.setPoints(OrthogonalMultipointPolyLine.correctComputedPoints(shape.getPoints()));
                                shape.refresh();
                            }
                            shape.getLayer().batch();
                        }
                    }
                });
            }

            @Override
            public IPrimitive<?> getControl() {
                return prim;
            }

            @Override
            public ControlHandleType getType() {
                return ControlHandleStandardType.CONNECTOR;
            }

            private boolean isEnabled() {
                return ((isActive()) && (handleList.isActive()));
            }
        }

        private IControlHandleList getPointHandles() {
            return getPointHandles(m_shape.getPoint2DArray(), new ControlHandleList(m_shape));
        }

        private IControlHandleList getPointHandles(Point2DArray points, final ControlHandleList chlist) {

            HandlerRegistrationManager manager = chlist.getHandlerRegistrationManager();

            ShapeXorYChanged shapeXoYChangedHandler = new ShapeXorYChanged(m_shape, chlist);

            manager.register(m_shape.addNodeDragStartHandler(shapeXoYChangedHandler));

            manager.register(m_shape.addNodeDragMoveHandler(shapeXoYChangedHandler));

            manager.register(m_shape.addNodeDragEndHandler(shapeXoYChangedHandler));

            int i = 0;
            for (final Point2D point : points.asArray()) {
                final Point2D p = point;

                final Circle prim = new Circle(R0).setX(m_shape.getX() + p.getX()).setY(m_shape.getY() + p.getY()).setFillColor(ColorName.DARKRED).setFillAlpha(0.8).setStrokeAlpha(0).setDraggable(true).setDragMode(m_dmode);

                prim.setSelectionStrokeOffset(SELECTION_OFFSET);
                prim.setSelectionBoundsOffset(SELECTION_OFFSET);
                prim.setFillBoundsForSelection(true);

                final int idx = i;
                chlist.add(new AbstractPointControlHandle() {
                    @Override
                    public AbstractPointControlHandle init() {
                        ControlXorYChanged handler = new ControlXorYChanged(idx, chlist, m_shape, p, prim, this, m_shape.getLayer());

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

                        prim.addNodeMouseDoubleClickHandler(new NodeMouseDoubleClickHandler() {
                            @Override
                            public void onNodeMouseDoubleClick(NodeMouseDoubleClickEvent event) {
                                Point2DArray result = destroyControlPoints(new int[]{idx});
                                m_shape.setPoints(result);
                                m_shape.refresh();
                                prim.removeFromParent();
                                m_shape.draw();
                            }
                        });

                        setPoint(p);

                        return this;
                    }

                    private Point2DArray destroyControlPoints(final int[] indexes) {
                        final Point2DArray oldPoints = m_shape.getPoint2DArray();
                        final Point2DArray newPoints = new Point2DArray();
                        for (int i = 0; i < oldPoints.size(); i++) {
                            if (!contains(indexes, i)) {
                                newPoints.push(oldPoints.get(i));
                            }
                        }
                        return newPoints;
                    }

                    private boolean contains(final int[] indexes,
                                                    final int index) {
                        for (int i : indexes) {
                            if (i == index) {
                                return true;
                            }
                        }
                        return false;
                    }


                    @Override
                    public IControlHandle setLocation(double x, double y) {
                        if (m_shape instanceof OrthogonalMultipointPolyLine) {
                            ((OrthogonalMultipointPolyLine) m_shape).update(p, x, y);
                        } else {
                            super.setLocation(x, y);
                        }
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

                i++;
            }
            return chlist;
        }

        private static void animate(final Circle circle, final double radius) {
            circle.animate(AnimationTweener.LINEAR, AnimationProperties.toPropertyList(AnimationProperty.Properties.RADIUS(radius)), ANIMATION_DURATION);
        }
    }

    public static class ShapeXorYChanged implements NodeDragStartHandler,
                                                    NodeDragMoveHandler,
                                                    NodeDragEndHandler {

        private IControlHandleList m_handleList;

        private Shape<?> m_shape;

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

        }

        @Override
        public void onNodeDragEnd(NodeDragEndEvent event) {

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

    public static class ControlXorYChanged implements NodeDragStartHandler,
                                                      NodeDragMoveHandler,
                                                      NodeDragEndHandler,
                                                      DragConstraintEnforcer {

        private int index;

        private Shape<?> m_prim;

        private AbstractPointControlHandle m_handle;

        private Point2D m_point;

        private IControlHandleList m_handleList;

        private Shape<?> m_shape;

        private Layer m_layer;

        public ControlXorYChanged(int index, IControlHandleList handleList, Shape<?> shape, Point2D point, Shape<?> prim, AbstractPointControlHandle handle, Layer layer) {
            this.index = index;

            m_handleList = handleList;

            m_shape = shape;

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
                m_prim.setFillColor(ColorName.GREEN);
                m_prim.getLayer().batch();
            }
        }

        @Override
        public void onNodeDragEnd(NodeDragEndEvent event) {
            if ((m_handle.isActive()) && (m_handleList.isActive())) {
                double x = m_prim.getX() - m_shape.getX();
                double y = m_prim.getY() - m_shape.getY();
                m_handle.setLocation(x, y);
                m_prim.setFillColor(ColorName.DARKRED);
                m_prim.getLayer().batch();
            }
        }

        @Override
        public void onNodeDragMove(NodeDragMoveEvent event) {
            if ((m_handle.isActive()) && (m_handleList.isActive())) {
                double x = m_prim.getX() - m_shape.getX();
                double y = m_prim.getY() - m_shape.getY();
                if (false && m_shape instanceof OrthogonalMultipointPolyLine) {

                    Point2D before = null;
                    if (index > 0) {
                        IControlHandle h = m_handleList.getHandle(index - 1);
                        before = h.getLocation();
                    }
                    Point2D after = null;
                    if (index < m_handleList.size() - 1) {
                        IControlHandle h = m_handleList.getHandle(index + 1);
                        after = h.getLocation();
                    }
                    final Point2D location = adjustPoint(before,
                                                       new Point2D(x, y),
                                                       after);
                    if (null != location) {
                        x = location.getX();
                        y = location.getY();
                    }

                    m_handle.setLocation(x, y);

                } else {
                    m_handle.setLocation(x, y);
                    m_shape.refresh();
                }
                m_shape.getLayer().batch();
            }
        }

        @Override
        public void startDrag(DragContext dragContext) {

        }

        @Override
        public boolean adjust(Point2D dxy) {
            // TODO: ?
            return false;
        }

        private static final double SEGMENT_SNAP_DISTANCE = 5d;

        // TODO: Properly move this logic to the adjust of drag constraints enforcer above ^^
        public Point2D adjustPoint(Point2D before, Point2D target, Point2D after) {

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

            return new Point2D(target.getX(), target.getY());
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
        public IControlHandle setLocation(double x, double y) {
            getPoint().setX(x);
            getPoint().setY(y);
            return this;
        }

        @Override
        public final ControlHandleType getType() {
            return ControlHandleStandardType.POINT;
        }

    }
}

